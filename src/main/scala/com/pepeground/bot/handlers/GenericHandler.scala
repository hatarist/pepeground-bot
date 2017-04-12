package com.pepeground.bot.handlers

import com.pepeground.bot.Config
import com.pepeground.bot.entities.ChatEntity
import com.pepeground.bot.repositories.{ChatRepository, ContextRepository}
import info.mukel.telegrambot4s.models.{Message, MessageEntity, User}

import scala.util.Random

class GenericHandler(message: Message) {
  def before(): Unit = {
    if (isChatChanged) ChatRepository.updateChat(chat.id, Option(chatName), migrationId)
  }

  def isChatChanged: Boolean = chatName != chat.name.getOrElse("") || migrationId != telegramId
  def isPrivate: Boolean = message.chat.`type` == "private"
  def isRandomAnswer: Boolean = scala.util.Random.nextInt(100) < chat.randomChance

  def isReplyToBot: Boolean = message.replyToMessage match {
    case Some(r: Message) => r.from match {
      case Some(f: User) => f.username.getOrElse("") == Config.bot.name
      case None => false
    }
    case None => false
  }

  def hasAnchors: Boolean = {
    hasText && (words.exists(Config.bot.anchors.contains(_)) || text.getOrElse("").contains(Config.bot.name))
  }

  def hasEntities: Boolean = message.entities match {
    case Some(s: Seq[MessageEntity]) => s.nonEmpty
    case None => false
  }

  def isEdition: Boolean = message.editDate match {
    case Some(_) => true
    case None => false
  }

  def hasText: Boolean = text match {
    case Some(t: String) => t.nonEmpty
    case None => false
  }

  def isMentioned: Boolean = text match {
    case Some(t: String) => t.contains(s"@${Config.bot.name}")
    case None => false
  }

  def isCommand: Boolean = text match {
    case Some(t: String) => t.startsWith("/")
    case None => false
  }

  def getWords(): List[String] = {
    var textCopy: String = text match {
      case Some(s: String) => s
      case None => return List()
    }

    message.entities match {
      case Some(s: Seq[MessageEntity]) => s.foreach { entity =>
        textCopy = textCopy.replace(textCopy.substring(entity.offset, entity.length), " " * entity.length)
      }
      case _ =>
    }

    textCopy
      .split(" ")
      .filter(s => s != " " || s.nonEmpty)
      .map(_.toLowerCase)
      .toList
  }

  lazy val chat: ChatEntity = ChatRepository.getOrCreateBy(telegramId, chatName, chatType)
  lazy val telegramId: Long = message.chat.id
  lazy val migrationId: Long = message.migrateToChatId.getOrElse(telegramId)
  lazy val chatType: String = message.chat.`type`
  lazy val chatName: String = message.chat.title.getOrElse(fromUsername)
  lazy val fromUsername: String = message.from match {
    case Some(u: User) => u.username.getOrElse("Unknown")
    case None => "Unknown"
  }

  lazy val context: List[String] = Random.shuffle(ContextRepository.getContext(chatContext, 10)).take(3)
  lazy val fullContext: List[String] = Random.shuffle(ContextRepository.getContext(chatContext, 50))
  lazy val words: List[String] = getWords()
  lazy val text: Option[String] = message.text
  lazy val chatContext: String = s"chat_context/${chat.id}"
}