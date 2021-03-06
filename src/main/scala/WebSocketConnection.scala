import java.security.InvalidParameterException

import akka.actor.SupervisorStrategy.Stop
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.actor.ActorPublisher
import akka.{NotUsed, Done}
import akka.actor.{ActorLogging, Actor}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{WebSocketUpgradeResponse, TextMessage, Message, WebSocketRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by darioalessandro on 4/6/16.
  */

//UUID: B9407F30-F5F8-466E-AFF9-25556B57FE6D MAJOR: 16914 MINOR: 22626 POWER: -68

case object Connect
case object OnDisconnect
case object Timeout
case object OnConnect

case class WebSocketConnectionRequest(url : String, receiverId : String, username : String)

class WebSocketConnection(materializer : ActorMaterializer, request:WebSocketConnectionRequest) extends akka.stream.actor.ActorPublisher[Message]  with ActorLogging {

  def receive = {
    case s : String =>
      throw new InvalidParameterException()
  }

  override def preStart = {
    context.become(disconnected, discardOld = true)
  }

  def helloSource: Source[Message, NotUsed] =
    Source.fromPublisher(ActorPublisher[Message](self))

  val printSink: Sink[Message, Future[Done]] =
    Sink.foreach {
      case message: TextMessage.Strict =>
        println(message.text)
    }

  def flow: Flow[Message, Message, Future[Done]] =
    Flow.fromSinkAndSourceMat(printSink, helloSource)(Keep.left)

  val userId = HttpHeader.parse("receiverId", request.receiverId) match {
    case ParsingResult.Ok(header, error) =>
      header
  }

  val username = HttpHeader.parse("username", request.username) match {
    case ParsingResult.Ok(header, error) =>
      header
  }

  def disconnected : Receive = {

    case s : String =>
      println(s"ignoring message because I am disconnected")

    case Connect =>
      log.debug("connect request")
      implicit val system = context.system
      implicit val mat = materializer
      val (upgradeResponse, closed) =
        Http().singleWebSocketRequest(WebSocketRequest(request.url,
          extraHeaders = scala.collection.immutable.Seq(userId, username)),
          flow)

      val connected = upgradeResponse.flatMap { upgrade =>
        Future.successful(Done)
      }

      connected.onSuccess {
        case s =>
          log.debug("connected")
          context.parent ! OnConnect
          this.context.become(this.connected, discardOld = true)
      }

      upgradeResponse.onFailure {case f =>
        log.debug("error connecting")
        //context.system.scheduler.scheduleOnce(2 seconds, self, Connect)
        Future.successful(Done)
      }

      connected.onFailure { case f =>
        log.debug("error connecting")
        //context.system.scheduler.scheduleOnce(2 seconds, self, Connect)
        Future.successful(Done)
      }

      closed.foreach{ u =>
        log.debug("on close")
        context.stop(self)
      }

      closed.onFailure { case f =>
        log.debug("error connecting")
        context.stop(self)
        //Future.successful(Done)
      }

  }

  def connected : Receive = {
    case OnDisconnect =>
      log.debug("OnDisconnect")
      context.stop(self)

    case s : BeaconUpdate =>
      if(totalDemand > 0) {
        onNext(TextMessage(s.toJson))
      } else {
        log.debug("dropping message since total demand is 0")
      }
  }


}
