package me.seravkin.replacer

import cats.data.{Kleisli, ReaderT}
import cats.effect.IO
import doobie.util.transactor.Transactor
import info.mukel.telegrambot4s.models._

package object infrastructure {

  sealed trait BotEvent

  final case class ReceiveMessage(message: Message) extends BotEvent
  final case class ReceiveEditedMessage(message: Message) extends BotEvent
  final case class ReceiveChannelPost(message: Message) extends BotEvent
  final case class ReceiveEditedChannelPost(message: Message) extends BotEvent
  final case class ReceiveInlineQuery(inlineQuery: InlineQuery) extends BotEvent
  final case class ReceiveChosenInlineResult(chosenInlineResult: ChosenInlineResult) extends BotEvent
  final case class ReceiveCallbackQuery(callbackQuery: CallbackQuery) extends BotEvent
  final case class ReceiveShippingQuery(shippingQuery: ShippingQuery) extends BotEvent
  final case class ReceivePreCheckoutQuery(preCheckoutQuery: PreCheckoutQuery) extends BotEvent

  type Bot[F[_]] = Kleisli[F, BotEvent, Unit]

  type ReplacerIO[A] = ReaderT[IO, Transactor[IO], A]

}
