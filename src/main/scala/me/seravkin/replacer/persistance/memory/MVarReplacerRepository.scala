package me.seravkin.replacer.persistance.memory

import cats._
import cats.effect.concurrent.MVar
import cats.implicits._
import com.rklaehn.radixtree.RadixTree
import com.thoughtworks.dsl.keywords.Monadic
import com.thoughtworks.dsl.domains.cats._
import me.seravkin.replacer.domain.AuthorizedUser
import me.seravkin.replacer.domain.repositories.{Caching, ReplacerRepository}

/**
  * MVar threadsafe cache for replacer repository
  * @param radix radix tree for fast prefix search
  * @param repository repository to cache
  * @tparam F effect
  */
final class MVarReplacerRepository[F[_]: Monad](radix: MVar[F, RadixTree[String, String]],
                                                repository: ReplacerRepository[F]) extends ReplacerRepository[F] with Caching[F] {

  /** @inheritdoc */
  override def findSuitableWords(query: String): F[List[String]] =
    radix.read.map(_.filterPrefix(query).values.toList)

  /** @inheritdoc */
  override def add(pair: (String, String), authorizedUser: AuthorizedUser): F[Unit] = {
    val (key, value) = pair
    val currentTrie = !Monadic(radix.read)
    val mergedTrie = currentTrie.mergeWith(RadixTree(key.toLowerCase -> value), (_, newValue) => newValue)

    !Monadic(repository.add(pair, authorizedUser))

    replaceWith(mergedTrie)
  }

  /** @inheritdoc */
  override def remove(key: String): F[Unit] = {
    val currentTrie = !Monadic(radix.read)
    val trieWithRemovedElement = currentTrie.filter((k, _) => k != key)

    !Monadic(repository.remove(key))

    replaceWith(trieWithRemovedElement)
  }

  /** @inheritdoc */
  override def all(): F[List[(String, String)]] =
    radix.read.map(_.entries.toList)

  /** @inheritdoc */
  override def updateCache(): F[Unit] = {
    val pairs = !Monadic(repository.all())
    val trie = RadixTree(pairs: _*)

    replaceWith(trie)
  }

  private[this] def replaceWith(trie: RadixTree[String, String]): F[Unit] =
    radix.take >>
    radix.put(trie)
}
