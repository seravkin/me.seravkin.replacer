package me.seravkin.replacer

import cats.data.{Kleisli, ReaderT}
import cats.effect.IO
import doobie.util.transactor.Transactor
import info.mukel.telegrambot4s.models._

package object infrastructure {

  type ReplacerIO[A] = ReaderT[IO, Transactor[IO], A]

}
