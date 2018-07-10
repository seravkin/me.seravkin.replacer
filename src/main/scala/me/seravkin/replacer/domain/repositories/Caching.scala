package me.seravkin.replacer.domain.repositories

/**
  * Trait for repositories with caching support
  * @tparam F effect
  */
trait Caching[F[_]] {
  /**
    * Fully update cache from database
    */
  def updateCache(): F[Unit]
}
