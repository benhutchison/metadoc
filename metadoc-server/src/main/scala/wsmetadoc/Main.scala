package wsmetadoc

import cats.effect._
import fs2._
import org.http4s.Response
import org.http4s.Status
import org.http4s.server.blaze._

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends fs2.StreamApp[IO] {
  val Port = 8383

  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, fs2.StreamApp.ExitCode] = {

    val server = new MetadocServer("/Users/ben/xylem/wsmetadoc/target/metadoc", requestShutdown)

    val timer = Timer[IO]
    for {
      exitCode <- BlazeBuilder[IO]
        .bindHttp(8383)
        .withWebSockets(true)
        .mountService(server.routes(s"http://localhost:$Port")(timer), "/")
        .withServiceErrorHandler(_ => { case ex => IO {ex.printStackTrace(); Response[IO](Status.InternalServerError)}})
        .serve
    } yield exitCode
  }

}
