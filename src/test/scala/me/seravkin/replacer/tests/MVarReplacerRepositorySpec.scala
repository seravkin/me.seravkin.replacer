package me.seravkin.replacer.tests

import cats._
import cats.implicits._
import cats.effect._
import cats.effect.concurrent.MVar
import cats.effect.internals.{IOTimer, IOTimerRef}
import com.rklaehn.radixtree.RadixTree
import me.seravkin.replacer.domain.AuthorizedUser
import me.seravkin.replacer.domain.repositories.ReplacerRepository
import me.seravkin.replacer.persistance.memory.{MVarChatStateRepository, MVarReplacerRepository}
import me.seravkin.replacer.tests.mocks.MockReplacerRepository

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext

class MVarReplacerRepositorySpec extends FlatSpec with Matchers {

  private[this] val example = RadixTree(
    "test"  -> "testZero",
    "test1" -> "testOne",
    "xxx"   -> "testTwo"
  )

  private[this] val mockRepository = new MockReplacerRepository[IO](example)
  private[this] val createExampleRepo: IO[MVarReplacerRepository[IO]] =
    MVar.of[IO, RadixTree[String, String]](example).map(mvar => new MVarReplacerRepository(mvar, mockRepository))

  "A MVar backed cache" should "find multiple items by prefix if they exists" in {

    val values = createExampleRepo.flatMap(_.findSuitableWords("test")).unsafeRunSync()

    values should equal (List("testZero", "testOne"))

  }

  it should "show all items on empty query" in {
    val values = createExampleRepo.flatMap(_.findSuitableWords("")).unsafeRunSync()

    values should equal (List("testZero", "testOne", "testTwo"))
  }

  it should "find no items if they are not in cache" in {

    val values = createExampleRepo.flatMap(_.findSuitableWords("yyy")).unsafeRunSync()

    values shouldBe empty

  }

  it should "add item and find it later" in {
    val updateAndFind = for(
      repo <- createExampleRepo;
      _    <- repo.add("yyy" -> "testThree", AuthorizedUser(0,0));
      item <- repo.findSuitableWords("yyy")
    ) yield item

    val values = updateAndFind.unsafeRunSync()

    values should equal (List("testThree"))
  }

  it should "remove item and not find it later" in {
    val deleteAndTryFind = for(
      repo <- createExampleRepo;
      _    <- repo.remove("xxx");
      item <- repo.findSuitableWords("xxx")
    ) yield item

    val values = deleteAndTryFind.unsafeRunSync()

    values shouldBe empty
  }

}
