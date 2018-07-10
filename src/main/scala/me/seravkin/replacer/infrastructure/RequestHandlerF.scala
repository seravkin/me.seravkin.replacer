package me.seravkin.replacer.infrastructure

import info.mukel.telegrambot4s.methods.ApiRequest

/**
  * Adapter interface for tg4s RequestHandler
  * @tparam F effect
  */
trait RequestHandlerF[F[_]] {
  /**
    * Send request and ignore result
    */
  def tell[R: Manifest](apiRequest: ApiRequest[R]): F[Unit]

  /**
    * Send request and get result
    */
  def ask[R: Manifest](apiRequest: ApiRequest[R]): F[R]

  /**
    * Send request and ignore result
    */
  def apply[R: Manifest](apiRequest: ApiRequest[R]): F[Unit] = tell(apiRequest)
}
