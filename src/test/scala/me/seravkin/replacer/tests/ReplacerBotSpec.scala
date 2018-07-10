package me.seravkin.replacer.tests

import cats._
import cats.effect._
import cats.effect.concurrent.MVar
import cats.data._
import cats.implicits._
import com.rklaehn.radixtree.RadixTree
import info.mukel.telegrambot4s.methods.{AnswerInlineQuery, ApiRequest, SendMessage}
import info.mukel.telegrambot4s.models._
import com.thoughtworks.dsl.keywords.Monadic
import com.thoughtworks.dsl.domains.cats._
import me.seravkin.replacer.ReplacerBot
import me.seravkin.replacer.ReplacerBot.{Nop, ReplacerBotState}
import me.seravkin.replacer.domain.AuthorizedUser
import me.seravkin.replacer.infrastructure._
import me.seravkin.replacer.persistance.memory.{MVarChatStateRepository, MVarReplacerRepository}
import me.seravkin.replacer.tests.mocks.{MockReplacerRepository, MockRequestHandlerF, MockUserRepository}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class ReplacerBotSpec extends FlatSpec with Matchers {

  "Replacer bot" should "answer to inline query" in {
    val answer = createBotAndSendMessage(
      example,
      ReceiveInlineQuery(InlineQuery("1", defaultUser, query = "test", offset = ""))
    )

    val texts = answer
      .unsafeRunSync()
      .asInstanceOf[AnswerInlineQuery]
      .results
      .collect { case m: InlineQueryResultArticle => m.title }.toList

    texts should equal(List("testZero", "testOne"))

  }

  it should "show all alternatives if inline query is empty" in {
    val answer = createBotAndSendMessage(
      example,
      ReceiveInlineQuery(InlineQuery("1", defaultUser, query = "", offset = ""))
    )

    val texts = answer
      .unsafeRunSync()
      .asInstanceOf[AnswerInlineQuery]
      .results
      .collect { case m: InlineQueryResultArticle => m.title }.toList

    texts should equal(List("testZero", "testOne", "testTwo"))
  }

  it should "work only in private chat in bot mode" in {
    val answer = createBotAndSendMessage(
      example,
      ReceiveMessage(Message(1, Some(defaultUser), 1, Chat(1, ChatType.Group)))
    )

    val texts = answer
      .unsafeRunSync()
      .asInstanceOf[SendMessage]
      .text

    texts should equal("Бот доступен только в личных беседах")
  }

  it should "not work for not authorized user" in {
    val answer = createBotAndSendMessage(
      example,
      ReceiveMessage(Message(1, Some(User(1, isBot = false, "test")), 1, Chat(2, ChatType.Private)))
    )

    val texts = answer.unsafeRunSync().asInstanceOf[SendMessage].text

    texts should equal("Пользователь с id: 2 не аутентифицирован")
  }

  it should "show all alternatives by /all command" in {
    val answer = createBotAndSendMessage(
      example,
      ReceiveMessage(message("/all"))
    )

    val texts = answer.unsafeRunSync().asInstanceOf[SendMessage].text

    texts should include("testZero")
    texts should include("testOne")
    texts should include("testTwo")
  }

  it should "add by /add command" in {
    val answer = createBotAndSendMessage(
      example,
      ReceiveMessage(message("/add")),
      ReceiveMessage(message("yyy")),
      ReceiveMessage(message("testThree")),
      ReceiveMessage(message("/all"))
    )

    val texts = answer.unsafeRunSync().asInstanceOf[SendMessage].text

    texts should include("testZero")
    texts should include("testOne")
    texts should include("testTwo")
    texts should include("testThree")
  }

  it should "update by /add command" in {
    val answer = createBotAndSendMessage(
      example,
      ReceiveMessage(message("/add")),
      ReceiveMessage(message("xxx")),
      ReceiveMessage(message("XXX")),
      ReceiveMessage(message("/all"))
    )

    val texts = answer.unsafeRunSync().asInstanceOf[SendMessage].text

    texts should include("testZero")
    texts should include("testOne")
    texts should include("XXX")
  }

  it should "restore cache to database state by /update command" in {
    val answer = createBotAndSendMessage(
      example,
      ReceiveMessage(message("/remove")),
      ReceiveMessage(message("xxx")),
      ReceiveMessage(message("/update")),
      ReceiveMessage(message("/all"))
    )

    val texts = answer.unsafeRunSync().asInstanceOf[SendMessage].text

    texts should include("testZero")
    texts should include("testOne")
    texts should include("testTwo")
  }


  it should "remove by /remove command" in {
    val answer = createBotAndSendMessage(
      example,
      ReceiveMessage(message("/remove")),
      ReceiveMessage(message("xxx")),
      ReceiveMessage(message("/all"))
    )

    val texts = answer.unsafeRunSync().asInstanceOf[SendMessage].text

    texts should include("testZero")
    texts should include("testOne")
  }

  private[this] val example = RadixTree(
    "test"  -> "testZero",
    "test1" -> "testOne",
    "xxx"   -> "testTwo"
  )

  private[this] val defaultUser = User(1, isBot = false, "test")

  private[this] def message(text: String): Message =
    Message(1, Some(User(1, isBot = false, "test")), 1, Chat(1, ChatType.Private), text = Some(text))

  private[this] def createBot(radixTree: RadixTree[String, String]): IO[(MVar[IO, List[ApiRequest[_]]], ReplacerBot[IO])] = {
    val chatStateMVar = !Monadic(MVar.of[IO, Map[Long, ReplacerBotState]](Map.empty))
    val replacerMVar = !Monadic(MVar.of[IO, RadixTree[String, String]](radixTree))
    val messagesMVar = !Monadic(MVar.of[IO, List[ApiRequest[_]]](Nil))

    val userRepository = new MockUserRepository[IO](Map(1L -> AuthorizedUser(1, 1)))
    val replacerRepository = new MVarReplacerRepository[IO](replacerMVar, new MockReplacerRepository[IO](radixTree))
    val chatStateRepository = new MVarChatStateRepository(chatStateMVar, Nop)
    val handler = new MockRequestHandlerF(messagesMVar)

    IO.pure(
      messagesMVar,
      new ReplacerBot[IO](replacerRepository, userRepository, chatStateRepository, handler))
  }

  private[this] def createBotAndSendMessage(tree: RadixTree[String, String], events: BotEvent*): IO[ApiRequest[_]] =
    for (
      pair <- createBot(example);
      _    <- events.map(pair._2).toList.sequence_;
      ans  <- pair._1.read
    ) yield ans.head


}
