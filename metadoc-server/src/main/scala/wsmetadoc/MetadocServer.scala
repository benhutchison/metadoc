package wsmetadoc

import cats.effect._
import cats.implicits._
import cats.effect.implicits._
import factor._
import factor.http4s._
import mouse.all._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.headers._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import java.io._
import java.nio.file._

import cats.data._
import com.google.protobuf.ByteString
import io.circe._
import io.circe.syntax._
import io.circe.parser._
//import io.circe.generic.auto._
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration

import metadoc.{schema => d}


class MetadocServer(baseDir: String, requestShutdown: IO[Unit]) extends org.http4s.dsl.Http4sDsl[IO] {


  implicit val genDevConfig: Configuration = Configuration.default.withDiscriminator("type_discriminator")

  import org.http4s._
  import org.http4s.websocket.WebsocketBits._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  implicit val timeout: FiniteDuration = 30.second

  val localAddress = SystemAddress("Compiler", "127.0.0.1", 3678)
  val system = new FactorSystem(localAddress)


  def routes(serverBaseUrl: String)(implicit timer: Timer[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "compilerws"  =>
      println(s"compilerWebsocket")
      compilerWebsocket()

    case GET -> Root / "shutdown" =>
      requestShutdown >> Ok("Shutting down")
  }

  def compilerWebsocket(): IO[Response[IO]] = {
    val handler = (url: String) => (e: Unit, s: Unit) => IO {

      println(s"WS reading file: $url")
      try {
        val data = Files.readAllBytes(Paths.get(s"$baseDir/$url"))
        ((), Some(Ior.Right((url, data))))
      } catch {
        case ex => ex.printStackTrace()
          throw ex
      }
    }

    val wsdecode: WebSocketFrame => Either[String, String] = {
      case Binary(bytes, true)=> Right(d.FetchBytesRequest.parseFrom(bytes).path)
      case other =>
        println(s"Unrecognized frame: $other");
        Either.left(s"Unrecognized frame: $other")
    }
    val wsencode = (reply: (String, Array[Byte])) =>
      Binary(d.FetchBytesResponse(reply._1, ByteString.copyFrom(reply._2)).toByteArray)
    val initMsg = Option.empty

    for {
      factor <- FactorWebSocket.handleWithFactorDecodeToStderr(system, s"MetadocFetcher", localAddress,
        env = (), initState = (),
        handler, wsdecode, wsencode, initMsg)
    } yield factor
  }

}