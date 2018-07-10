package me.seravkin.replacer.tests.mocks

import cats._
import cats.implicits._
import cats.effect._
import com.rklaehn.radixtree.RadixTree
import me.seravkin.replacer.domain.AuthorizedUser
import me.seravkin.replacer.domain.repositories.ReplacerRepository

class MockReplacerRepository[F[_]: Monad](example: RadixTree[String, String]) extends ReplacerRepository[F] {
  override def findSuitableWords(query: String): F[List[String]] = Monad[F].pure(Nil)

  override def add(pair: (String, String), authorizedUser: AuthorizedUser): F[Unit] = Monad[F].unit

  override def remove(key: String): F[Unit] = Monad[F].unit

  override def all(): F[List[(String, String)]] = Monad[F].pure { example.entries.toList }
}
