package me.seravkin.replacer.persistance.doobie

import cats._
import cats.implicits._
import cats.effect._
import cats.data._
import doobie._
import doobie.implicits.{toConnectionIOOps, _}
import me.seravkin.replacer.domain.AuthorizedUser
import me.seravkin.replacer.domain.repositories.ReplacerRepository

/**
  * Doobie implementation for replacer repository
  * @tparam F effect
  */
final class DoobieReplacerRepository[F[_]: Monad] extends ReplacerRepository[ReaderT[F, Transactor[F], ?]] {

  /** @inheritdoc */
  override def findSuitableWords(query: String): ReaderT[F, Transactor[F], List[String]] = ReaderT { transactor =>
    sql"select value_text from text_to_replace where key_text like $query || '%%'"
      .query[String]
      .to[List]
      .transact(transactor)
  }

  /** @inheritdoc */
  override def add(pair: (String, String), authorizedUser: AuthorizedUser): ReaderT[F, Transactor[F], Unit] = ReaderT { transactor =>
    sql"""insert into text_to_replace
          (key_text, value_text, creator_id, creation_date, modifier_id, modification_date)
          values (${pair._1}, ${pair._2}, ${authorizedUser.userId}, current_timestamp, ${authorizedUser.userId}, current_timestamp)
          on conflict (key_text)
          do update
          set value_text = EXCLUDED.value_text, modifier_id = ${authorizedUser.userId}, modification_date = current_timestamp"""
      .update
      .run
      .map(_ => ())
      .transact(transactor)
  }

  /** @inheritdoc */
  override def remove(key: String): ReaderT[F, Transactor[F], Unit] = ReaderT { transactor =>
    sql"delete from text_to_replace where key_text = $key"
      .update
      .run
      .map(_ => ())
      .transact(transactor)
  }

  /** @inheritdoc */
  override def all(): ReaderT[F, Transactor[F], List[(String, String)]] = ReaderT { transactor =>
    sql"select key_text, value_text from text_to_replace"
      .query[(String, String)]
      .to[List]
      .transact(transactor)
  }
}
