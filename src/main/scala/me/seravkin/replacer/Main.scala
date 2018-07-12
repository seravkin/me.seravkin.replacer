package me.seravkin.replacer

import java.nio.file.{Files, Paths}

import cats._
import cats.implicits._
import cats.data._
import cats.effect._
import cats.effect.concurrent.MVar
import com.rklaehn.radixtree.RadixTree
import infrastructure._
import com.thoughtworks.dsl.keywords.Monadic
import com.thoughtworks.dsl.domains.cats._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.util.transactor.Transactor
import info.mukel.telegrambot4s.api.Polling
import me.seravkin.replacer.ReplacerBot.{Nop, ReplacerBotState}
import me.seravkin.replacer.config.ReplacerConfiguration
import me.seravkin.replacer.persistance.doobie.{DoobieReplacerRepository, DoobieUserRepository}
import me.seravkin.replacer.persistance.memory.{MVarChatStateRepository, MVarReplacerRepository}
import me.seravkin.tg.adapter.TelegramBotAdapter
import me.seravkin.tg.adapter.requests.RequestHandlerAdapter


object Main extends IOApp {

  private def path: IO[String] = IO {
    Option("replacer.conf")
      .filter(x => Files.exists(Paths.get(x)))
      .getOrElse(System.getenv("REPLACER_CONFIG_PATH"))
  }

  private def config: IO[ReplacerConfiguration] =
    path.map(filename => pureconfig.loadConfigOrThrow[ReplacerConfiguration](Paths.get(filename), "replacer"))

  private def hikari(replacerConfiguration: ReplacerConfiguration): IO[HikariDataSource] = IO {
    val config = new HikariConfig()

    config.setDriverClassName(replacerConfiguration.driver)
    config.setJdbcUrl(replacerConfiguration.jdbcUrl)
    config.setUsername(replacerConfiguration.username)
    config.setPassword(replacerConfiguration.password)
    config.addDataSourceProperty("cachePrepStmts", "true")
    config.addDataSourceProperty("prepStmtCacheSize", "250")
    config.addDataSourceProperty("prepStmtCacheSqlLimit", 2000)

    new HikariDataSource(config)
  }

  override def run(args: List[String]): IO[ExitCode] = {

    val conf = !Monadic(config)
    val ds = !Monadic(hikari(conf))

    val interpreter = new IoBracketedInterpreter(
      ds.getConnection
    )

    val xa = Transactor.fromDriverManager[IO](
      ds.getDriverClassName,
      ds.getJdbcUrl,
      ds.getUsername,
      ds.getPassword
    )

    val replacerRepository = new DoobieReplacerRepository[IO]

    val pairs = !Monadic(replacerRepository.all()(xa))

    val radix = !Monadic(MVar.of[ReplacerIO, RadixTree[String, String]](RadixTree(pairs: _*)).apply(xa))
    val state = !Monadic(MVar.of[ReplacerIO, Map[Long, ReplacerBotState]](Map.empty).apply(xa))

    val users = new DoobieUserRepository[IO]()
    val replacer = new MVarReplacerRepository(radix, replacerRepository)
    val chatStateRepository = new MVarChatStateRepository(state, Nop)

    val adapter =
      new TelegramBotAdapter(conf.token, handler =>
        Kleisli(
          new ReplacerBot(replacer,
            users,
            chatStateRepository,
            new RequestHandlerAdapter[ReplacerIO](handler)))
        .mapK(interpreter)
      ) with Polling

    !Monadic(adapter.runSafe())

    IO.pure(ExitCode.Success)
  }


}
