package me.seravkin.replacer.persistance.doobie

import cats._
import cats.implicits._
import cats.effect._
import cats.data._
import doobie._
import doobie.implicits.{toConnectionIOOps, _}
import me.seravkin.replacer.domain.AuthorizedUser
import me.seravkin.replacer.domain.repositories.UserRepository

/**
  * Doobie implementation for user repository
  * @tparam F effect
  */
final class DoobieUserRepository[F[_]: Monad] extends UserRepository[ReaderT[F, Transactor[F], ?]] {

  /** @inheritdoc */
  override def findByChatId(long: Long): ReaderT[F, Transactor[F], Option[AuthorizedUser]] = ReaderT { transactor =>
    sql"select user_id, chat_id from authorized_user where chat_id = $long"
      .query[AuthorizedUser]
      .to[List]
      .transact(transactor)
      .map(_.headOption)
  }

}
