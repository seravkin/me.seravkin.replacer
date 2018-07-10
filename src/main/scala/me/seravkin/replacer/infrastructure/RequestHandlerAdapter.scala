package me.seravkin.replacer.infrastructure

import cats._
import cats.implicits._
import cats.effect.{Concurrent, IO}
import info.mukel.telegrambot4s.api.RequestHandler
import info.mukel.telegrambot4s.methods.ApiRequest


/**
  * Adapter for tg4s RequestHandler
  * @tparam F effect
  */
final class RequestHandlerAdapter[F[_] : Concurrent](requestHandler: RequestHandler) extends RequestHandlerF[F] {

  /** @inheritdoc */
  override def ask[R: Manifest](apiRequest: ApiRequest[R]): F[R] =
    Concurrent[F].liftIO(IO.fromFuture(IO.pure(requestHandler(apiRequest))))

  /** @inheritdoc */
  override def tell[R: Manifest](apiRequest: ApiRequest[R]): F[Unit] =
    Concurrent[F].liftIO(IO.fromFuture(IO.pure(requestHandler(apiRequest))) >> IO.unit)

}
