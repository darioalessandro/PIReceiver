import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.actor.ActorPublisher
import akka.{NotUsed, Done}
import akka.actor.{ActorLogging, Actor}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{TextMessage, Message, WebSocketRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import sun.plugin.dom.exception.InvalidStateException
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by darioalessandro on 4/6/16.
  */

case object Connect
case object OnDisconnect


class WebSocketConnection(materializer : ActorMaterializer) extends akka.stream.actor.ActorPublisher[Message]  with ActorLogging {

  def receive = {
    case s : String =>
      throw new InvalidStateException("")

  }


  override def preStart = {
    context.become(disconnected, discardOld = true)
  }

  val helloSource: Source[Message, NotUsed] =
    Source.fromPublisher(ActorPublisher[Message](self))

  val printSink: Sink[Message, Future[Done]] =
    Sink.foreach {
      case message: TextMessage.Strict =>
        println(message.text)
    }

  val flow: Flow[Message, Message, Future[Done]] =
    Flow.fromSinkAndSourceMat(printSink, helloSource)(Keep.left)


  val userId = HttpHeader.parse("receiverId", "134") match {
    case ParsingResult.Ok(header, error) =>
      header
  }

  val username = HttpHeader.parse("username", "134") match {
    case ParsingResult.Ok(header, error) =>
      header
  }

  def disconnected : Receive = {

    case s : String =>
      print(s"ignoring message $s")

    case Connect =>
      log.debug("connect request")
      implicit val system = context.system
      implicit val mat = materializer
      val (upgradeResponse, closed) =
        Http().singleWebSocketRequest(WebSocketRequest("ws://localhost:9001/receiverSocket",
          extraHeaders = scala.collection.immutable.Seq(userId, username)),
          flow)

      val connected = upgradeResponse.flatMap { upgrade =>
        if (upgrade.response.status == StatusCodes.OK) {
          log.debug("connected")

          Future.successful(Done)
        } else {
          log.debug("error connecting")
          //context.system.scheduler.scheduleOnce(2 seconds, self, Connect)
          Future.successful(Done)
        }
      }

      connected.onFailure { case f =>
        log.debug("error connecting")
        context.system.scheduler.scheduleOnce(2 seconds, self, Connect)
        Future.successful(Done)
      }

      connected.onComplete { m =>
        log.debug("onComplete " + m)
        this.context.become(this.connected, discardOld = true)
      }

      closed.foreach{ u =>
        log.debug("on close")
        this.context.become(this.disconnected, discardOld = true)
        context.system.scheduler.scheduleOnce(2 seconds, self, Connect)
      }
  }

  def connected : Receive = {
    case OnDisconnect =>
      log.debug("OnDisconnect")
      context.become(disconnected, discardOld = true)
      context.system.scheduler.scheduleOnce(2 seconds, self, Connect)

    case s : String =>
      onNext(TextMessage(s))
  }


}
