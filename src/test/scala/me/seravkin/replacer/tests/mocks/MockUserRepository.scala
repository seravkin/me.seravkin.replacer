package me.seravkin.replacer.tests.mocks

import cats._
import cats.implicits._

import me.seravkin.replacer.domain.AuthorizedUser
import me.seravkin.replacer.domain.repositories.UserRepository

class MockUserRepository[F[_]: Monad](users: Map[Long, AuthorizedUser]) extends UserRepository[F] {
  override def findByChatId(long: Long): F[Option[AuthorizedUser]] =
    Monad[F].pure(users.get(long))
}
