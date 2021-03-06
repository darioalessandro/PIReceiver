import java.io.{InputStreamReader, BufferedReader, InputStream}
import java.net.NetworkInterface
import java.util

import akka.NotUsed
import akka.stream.actor.{ActorPublisher, ActorPublisherMessage}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.reactivestreams.Publisher
import scripting.Utils
import akka.actor._
import akka.stream._
import akka.stream.scaladsl.Source
import scala.concurrent.duration._
import scala.concurrent.{Promise, Future}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
case class Continue()


object CmdRunner extends App with Utils {

  val conf = ConfigFactory.load()

  implicit val system = ActorSystem("BeaconListener")
  implicit val materializer = ActorMaterializer()
  import scala.sys.process._

  var terminalConnection : Source[String,NotUsed] = Source.empty

  var dataPublisherRef : ActorRef = ActorRef.noSender



  val connectionRequest = WebSocketConnectionRequest(url= conf.getString("backend.url"),
    receiverId=conf.getString("backend.receiverId") ,
    username =conf.getString("backend.username"))
  val connection = system.actorOf( Props(new SocketSupervisor(materializer, connectionRequest)), "WebSocketConnection")

  connection ! Connect


  import scala.sys.process.ProcessIO

  val pio = new ProcessIO(_ => (), { stdout =>
    dataPublisherRef = system.actorOf(Props(new InputStreamPublisher(stdout, 10)))

    terminalConnection = Source.fromPublisher(ActorPublisher[String](dataPublisherRef))

    terminalConnection.runForeach { (beaconUpdate: String) =>
        connection ! beaconUpdate
//        println(s"Data from $beaconUpdate")
      }
      .onComplete(_ => print("complete"))
    dataPublisherRef ! Continue()
  },
    error => scala.io.Source.fromInputStream(error)
      .getLines.foreach(println))

  var cmd = args(0)
  println(s"executing $cmd")
  cmd.run(pio)
}
