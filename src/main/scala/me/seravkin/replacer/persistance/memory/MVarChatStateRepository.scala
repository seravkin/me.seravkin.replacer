package me.seravkin.replacer.persistance.memory

import cats._
import cats.effect.concurrent.MVar
import cats.implicits._
import com.thoughtworks.dsl.keywords.Monadic
import com.thoughtworks.dsl.domains.cats._
import me.seravkin.replacer.domain.repositories.ChatStateRepository

/**
  * MVar based threadsafe chat state repository
  * @param state MVar storage for state
  * @param defaultState default state for user
  * @tparam F effect
  * @tparam S state
  */
final class MVarChatStateRepository[F[_]: Monad, S](state: MVar[F, Map[Long,S]], defaultState: S) extends ChatStateRepository[F, S] {

  /** @inheritdoc */
  override def get(chatId: Long): F[S] =
    state.read.map(_.getOrElse(chatId, defaultState))

  /** @inheritdoc */
  override def set(chatId: Long, s: S): F[Unit] = {
    val current = !Monadic(state.take)

    state.put(current + (chatId -> s))
  }

}
