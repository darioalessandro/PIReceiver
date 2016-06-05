import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.http.scaladsl.model.ws.{TextMessage, Message}
import akka.stream.ActorMaterializer
import sun.plugin.dom.exception.InvalidStateException
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by darioalessandro on 6/5/16.
  */
class SocketSupervisor (materializer : ActorMaterializer) extends Actor  with ActorLogging {

  def receive = {
    case s : String =>
      throw new InvalidStateException("")
  }

  override def preStart = {
    context.become(disconnected, discardOld = true)
  }

  def disconnected : Receive = {

    case s : String =>
      log.debug("ignoring message, disconnected")

    case t : Terminated =>
      println("web socket connection failed")
      context.system.scheduler.scheduleOnce(2 seconds, self, Connect)

    case Connect =>
      val websocket = context.actorOf( Props(new WebSocketConnection(materializer)), "WebSocketConnection")
      context.watch(websocket)
      websocket ! Connect
      context.become(connecting(websocket), discardOld = true)
  }

  def connecting(websocket : ActorRef) : Receive = {

    case s : String =>
      log.debug("ignoring message, connecting")

    case OnConnect =>
      log.debug("connected")
      context.become(connected(websocket), discardOld = false)

    case t : Terminated =>
      println("disconnected")
      context.become(disconnected, discardOld = false)
      context.system.scheduler.scheduleOnce(2 seconds, self, Connect)
  }

  def connected(websocket : ActorRef) : Receive = {

    case s : String =>
      log.debug(s"got message $s")
      websocket ! s

    case t : Terminated =>
      log.debug("disconnected")
      context.become(disconnected, discardOld = false)
      context.system.scheduler.scheduleOnce(2 seconds, self, Connect)

  }



}
