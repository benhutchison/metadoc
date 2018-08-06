package metadoc

import java.nio._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.meta.internal.{semanticdb3 => s}
import metadoc.MetadocApp._
import metadoc.{schema => d}
import org.scalajs.dom._

import scala.scalajs.js.typedarray._
import scala.scalajs.js.typedarray.TypedArrayBufferOps._

import boopickle.Default._

import collection.mutable.Map


object MetadocFetch {

  def init(url: String = "ws://localhost:8383/compilerws"): Future[MetadocFetch] = {
    val prom = Promise[MetadocFetch]

    println("creating socket...")
    val socket = new WebSocket(url)
    socket.binaryType = "arraybuffer"

    socket.onerror = (e: ErrorEvent) => prom.failure(new Exception(s"WebSocket url=$url: error=${e}"))

    socket.onopen = (e: Event) => {
      println(s"Socket onopen: ${socket.protocol}  ${socket.readyState}")
      prom.success(MetadocFetch(socket))
    }

    prom.future
  }

}

case class MetadocFetch(socket: WebSocket, fetches: Map[String, Promise[Option[Array[Byte]]]] = Map.empty) {

  socket.onmessage = (e: MessageEvent) => {
    val data = e.data
    println(s"Socket on message, data: $data")
    data match {
      case buffer: ArrayBuffer =>
        val bytes = Array.ofDim[Byte](buffer.byteLength)
        TypedArrayBuffer.wrap(buffer).get(bytes)
        val fetchResponse = Unpickle[FetchResponse].fromBytes(ByteBuffer.wrap(bytes))
        fetches.remove(fetchResponse.path).foreach(prom => prom.success(fetchResponse.optData))
      case other =>
        println(s"WS msg is not an ArrayBuffer. ignoring: $other")
    }
  }

  socket.onclose = (e: CloseEvent) => {
    val msg = s"Socket onclose, code: ${e.code} reason: ${e.reason} clean: ${e.wasClean}"
    fetches.values.foreach(_.failure(new Exception(msg)))
    fetches.clear()
    println(msg)
  }

  def fetchByter(path: String): Future[Option[Array[Byte]]] = {
    val prom = Promise[Option[Array[Byte]]]
    fetches.put(path, prom)
    socket.send(Pickle.intoBytes(FetchRequest(path)).arrayBuffer())
    prom.future
  }

  def symbol(symbolId: String): Future[Option[d.SymbolIndex]] = {
    val url = "symbol/" + JSSha512.sha512(symbolId)
    for {
      optBytes <- fetchByter(url)
    } yield {
      optBytes.map(d.SymbolIndex.parseFrom(_))
    }
  }

  def document(filename: String): Future[Option[s.TextDocument]] = {
    val url = "semanticdb/" + filename + ".semanticdb"
    for {
      optBytes <- fetchByter(url)
    } yield {
      optBytes.map((s.TextDocuments.parseFrom(_).documents.head))
    }
  }

  def workspace(): Future[d.Workspace] = {
    for {
      optBytes <- fetchByter("index.workspace")
    } yield {
      optBytes.map(d.Workspace.parseFrom(_)).get
    }
  }

}
