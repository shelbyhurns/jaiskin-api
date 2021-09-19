import cats.effect._
import natchez.Trace.Implicits.noop
import org.typelevel.log4cats.slf4j.Slf4jLogger

import sql.PostgreSQL
import services.Services
import configuration.Config
import http.{ HttpAPI, HttpServer }

object Main extends IOApp.Simple {
  implicit val logger = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    Config.load[IO].flatMap { cfg =>
      logger.info("App configuration has been loaded") *>
        PostgreSQL
          .make[IO](cfg.psqlConfig)
          .use { res =>
            val services = Services.make[IO](res.postgresSql)
            val httpApi = HttpAPI.create[IO](services).routes

            HttpServer[IO]
              .addBlazeServer(httpApi, 8000)
              .flatMap(_.use(_ => IO.never))
          }
    }
}
