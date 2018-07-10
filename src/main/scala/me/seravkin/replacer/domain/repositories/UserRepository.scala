package me.seravkin.replacer.domain.repositories

import me.seravkin.replacer.domain.AuthorizedUser

/**
  * Repository for finding authorized users by their chat id
  * @tparam F effect
  */
trait UserRepository[F[_]] {
  /**
    * Find authorized users by their chat id
    * @param chatId user's chat id
    * @return authorized user if found
    */
  def findByChatId(chatId: Long): F[Option[AuthorizedUser]]
}
