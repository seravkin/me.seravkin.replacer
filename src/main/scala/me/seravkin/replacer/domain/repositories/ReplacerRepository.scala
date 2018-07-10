package me.seravkin.replacer.domain.repositories

import me.seravkin.replacer.domain.AuthorizedUser

/**
  * Repository for words to be replaced
  * @tparam F effect
  */
trait ReplacerRepository[F[_]] {
  /**
    * Find words by prefix of replaced word
    * @param query prefix of replaced word
    */
  def findSuitableWords(query: String): F[List[String]]

  /**
    * Add new word to be replaced
    * @param pair Pair - (word to be replaced, word replacing original)
    * @param authorizedUser user which was editing the word
    */
  def add(pair: (String, String), authorizedUser: AuthorizedUser): F[Unit]

  /**
    * Remove word to be replaced
    * @param key word to be replaced
    */
  def remove(key: String): F[Unit]

  /**
    * Get all pairs of replaced word and their substitute
    * @return Pairs - (word to be replaced, word replacing original)
    */
  def all(): F[List[(String, String)]]
}
