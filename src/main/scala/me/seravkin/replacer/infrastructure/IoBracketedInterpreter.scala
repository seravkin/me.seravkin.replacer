package me.seravkin.replacer.infrastructure

import java.sql.Connection

import cats._
import cats.data.ReaderT
import cats.effect.ExitCase.Completed
import cats.effect.IO
import doobie.free.KleisliInterpreter
import doobie.free.connection.unit
import doobie.util.transactor.{Strategy, Transactor}

/**
  * Interpreter for application's IO. Uses single connection and transaction for one request. Rollbacks on error.
  * @param xa connection factory
  */
final class IoBracketedInterpreter(xa: => Connection) extends (ReplacerIO ~> IO) {
  override def apply[A](fa: ReaderT[IO, Transactor[IO], A]): IO[A] =
    IO(xa).bracketCase { connection =>

      connection.setAutoCommit(false)

      val transactor = Transactor[IO, Connection](
        connection,
        IO.pure[Connection],
        KleisliInterpreter[IO].ConnectionInterpreter,
        Strategy.default.copy(always = unit, after = unit, oops = unit))

      fa(transactor)

    } {
      case (connection, Completed) => IO {
        connection.commit()
        connection.close()
      }
      case (connection, _) => IO {
        connection.rollback()
        connection.close()
      }
    }
}
