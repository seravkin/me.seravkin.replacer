package me.seravkin.replacer.domain.repositories

/**
  * Interface for state of current chat
  * @tparam F effect
  * @tparam S state
  */
trait ChatStateRepository[F[_], S] {
  /**
    * Get current state by chat id
    * @param chatId unique chat id
    */
  def get(chatId: Long): F[S]

  /**
    * Set current state by chat id
    * @param chatId unique chat id
    * @param s state to be set
    */
  def set(chatId: Long, s: S): F[Unit]
}
