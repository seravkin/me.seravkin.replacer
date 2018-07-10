package me.seravkin.replacer.domain

/**
  * Authorized user
  * @param userId primary key
  * @param chatId tg chat id
  */
final case class AuthorizedUser(userId: Long, chatId: Long)