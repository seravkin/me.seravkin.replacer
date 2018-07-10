package me.seravkin.replacer.tests.mocks

import cats.effect._
import cats.effect.concurrent.MVar
import cats._
import cats.implicits._
import info.mukel.telegrambot4s.methods.ApiRequest
import me.seravkin.replacer.infrastructure.RequestHandlerF

class MockRequestHandlerF[F[_]: Concurrent](mVar: MVar[F, List[ApiRequest[_]]]) extends RequestHandlerF[F] {
  override def tell[R: Manifest](apiRequest: ApiRequest[R]): F[Unit] =
    mVar.take.flatMap(list => mVar.put(apiRequest :: list))

  override def ask[R: Manifest](apiRequest: ApiRequest[R]): F[R] = ???
}
