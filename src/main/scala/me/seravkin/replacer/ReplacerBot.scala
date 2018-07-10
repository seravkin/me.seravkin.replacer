package me.seravkin.replacer

import cats._
import cats.implicits._
import com.thoughtworks.dsl.keywords.Monadic
import com.thoughtworks.dsl.domains.cats._
import info.mukel.telegrambot4s.methods.{AnswerInlineQuery, SendMessage}
import info.mukel.telegrambot4s.models._
import me.seravkin.replacer.ReplacerBot._
import me.seravkin.replacer.domain.AuthorizedUser
import me.seravkin.replacer.domain.repositories.{Caching, ChatStateRepository, ReplacerRepository, UserRepository}
import me.seravkin.replacer.infrastructure._

/**
  * Bot that give a list of substitutes to given word in inline query
  */
final class ReplacerBot[F[_]: Monad](replacer: ReplacerRepository[F] with Caching[F],
                               userRepository: UserRepository[F],
                               chatStateRepository: ChatStateRepository[F, ReplacerBotState],
                               sender: RequestHandlerF[F]) extends (BotEvent => F[Unit]) {


  override def apply(event: BotEvent): F[Unit] = event match {
    case ReceiveInlineQuery(inlineQuery) =>
      val result = !Monadic(replacer.findSuitableWords(inlineQuery.query))

      val answer = AnswerInlineQuery(inlineQuery.id,
        result.distinct.zipWithIndex.map { case (phrase, i) => InlineQueryResultArticle(
          i.toString,
          phrase,
          InputTextMessageContent(phrase))
        })

      sender(answer)
    case ReceiveMessage(message @ IsNotPrivate(_)) =>
      sender(SendMessage(message.chat.id, "Бот доступен только в личных беседах"))
    case ReceiveMessage(message @ HasText("/start")) =>
      sender(SendMessage(message.chat.id, "Аутентифицированные пользователи могут задать боту новые переводы"))
    case ReceiveMessage(msg) =>
      authenticate(msg)(commands)
  }

  private[this] def commands(user: AuthorizedUser, botState: ReplacerBotState, message: Message): F[Unit] = (botState, message) match {
    case (_, HasText("/exit")) =>
      chatStateRepository.set(message.chat.id, Nop) >>
      sender(SendMessage(message.chat.id, "Действие отменено"))

    case (Nop, HasText("/help")) =>
      sender(SendMessage(message.chat.id,
        """
          |/add - добавить новый перевод
          |/remove - убрать перевод по ключу
          |/update - обновить кэш
          |/all - показать все переводы
          |/exit - выйти из текущей команды
        """.stripMargin))
    case (Nop, HasText("/all")) =>
      val values = !Monadic(replacer.all())

      val text = values
        .map(pair => s"Ключ: '${pair._1}' Значение: '${pair._2}'")
        .fold("Переводы: \n") { case (row, acc) => row + "\n" + acc }

      sender(SendMessage(message.chat.id, text))

    case (Nop, HasText("/add")) =>
      chatStateRepository.set(message.chat.id, WaitingForKeyToAdd) >>
      sender(SendMessage(message.chat.id, "Введите ключ для поиска"))

    case (WaitingForKeyToAdd, HasText(key)) =>
      chatStateRepository.set(message.chat.id, WaitingForValue(key)) >>
      sender(SendMessage(message.chat.id, "Введите значение"))

    case (WaitingForValue(key), HasText(value)) =>
      replacer.add(key -> value, user) >>
      chatStateRepository.set(message.chat.id, Nop) >>
      sender(SendMessage(message.chat.id, "Ключ и значение добавлены"))

    case (Nop, HasText("/remove")) =>
      chatStateRepository.set(message.chat.id, WaitingForKeyToDelete) >>
      sender(SendMessage(message.chat.id, "Введите ключ для удаления"))

    case (WaitingForKeyToDelete, HasText(key)) =>
      replacer.remove(key) >>
      chatStateRepository.set(message.chat.id, Nop) >>
      sender(SendMessage(message.chat.id, "Перевод удален"))

    case (Nop, HasText("/update")) =>
      replacer.updateCache() >>
      sender(SendMessage(message.chat.id, "Кэш обновлен"))

    case (Nop, _) =>
      sender(SendMessage(message.chat.id, "Неизвестная комманда"))

  }

  private[this] def authenticate(message: Message)(onSuccess: (AuthorizedUser,ReplacerBotState, Message) => F[Unit]): F[Unit] = {
    val user = !Monadic(userRepository.findByChatId(message.chat.id))

    if(user.nonEmpty) {
      val state = !Monadic(chatStateRepository.get(message.chat.id))

      onSuccess(user.get, state, message)
    } else {
      sender(SendMessage(message.chat.id, s"Пользователь с id: ${message.chat.id} не аутентифицирован"))
    }
  }

  private object HasText {
    def unapply(arg: Message): Option[String] =
      arg.text
  }

  private object IsNotPrivate {
    def unapply(arg: Message): Option[Message] =
      if(arg.chat.`type` != ChatType.Private) {
        Some(arg)
      } else {
        None
      }
  }

}

object ReplacerBot {
  sealed trait ReplacerBotState

  object Nop extends ReplacerBotState
  object WaitingForKeyToAdd extends ReplacerBotState
  object WaitingForKeyToDelete extends ReplacerBotState
  final case class WaitingForValue(key: String) extends ReplacerBotState

}