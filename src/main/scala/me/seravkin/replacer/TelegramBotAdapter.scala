package me.seravkin.replacer

import cats.effect._
import info.mukel.telegrambot4s._
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.models._
import me.seravkin.replacer.infrastructure._

/**
  * Adapter from Bot[IO] to telegrambot4s bot API
  * @param token tg bot token
  * @param botFactory function to create bot from request handler
  */
final class TelegramBotAdapter(val token: String, botFactory: RequestHandler => Bot[IO]) extends TelegramBot with Polling {

  private[this] val constructed = botFactory(request)

  private[this] def run(botEvent: BotEvent): Unit =
    constructed(botEvent).unsafeRunSync()

  override def receiveMessage(message: Message): Unit =
    run(ReceiveMessage(message))

  override def receiveEditedMessage(editedMessage: Message): Unit =
    run(ReceiveEditedMessage(editedMessage))

  override def receiveChannelPost(message: Message): Unit =
    run(ReceiveChannelPost(message))

  override def receiveEditedChannelPost(message: Message): Unit =
    run(ReceiveEditedChannelPost(message))

  override def receiveInlineQuery(inlineQuery: InlineQuery): Unit =
    run(ReceiveInlineQuery(inlineQuery))

  override def receiveChosenInlineResult(chosenInlineResult: ChosenInlineResult): Unit =
    run(ReceiveChosenInlineResult(chosenInlineResult))

  override def receiveCallbackQuery(callbackQuery: CallbackQuery): Unit =
    run(ReceiveCallbackQuery(callbackQuery))

  override def receiveShippingQuery(shippingQuery: ShippingQuery): Unit =
    run(ReceiveShippingQuery(shippingQuery))

  override def receivePreCheckoutQuery(preCheckoutQuery: PreCheckoutQuery): Unit =
    run(ReceivePreCheckoutQuery(preCheckoutQuery))

  /** IO-based run method */
  def runSafe(): IO[Unit] = IO { run() }
}
