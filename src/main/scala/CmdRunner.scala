import java.io.{InputStreamReader, BufferedReader, InputStream}
import java.util

import akka.NotUsed
import akka.stream.actor.{ActorPublisher, ActorPublisherMessage}
import akka.util.ByteString
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

class InputStreamPublisher(is: InputStream, chunkSize: Int = 10)
  extends akka.stream.actor.ActorPublisher[String]
    with ActorLogging {

  var readBytesTotal = 0L

  def receive = {
    case ActorPublisherMessage.Request(elements) ⇒ readAndSignal()
    case Continue                                ⇒ readAndSignal()
    case ActorPublisherMessage.Cancel            ⇒ context.stop(self)
  }

  def readAndSignal(): Unit =
    if (isActive) {
      readAndEmit()
      if (totalDemand > 0) self ! Continue
    }

  def readAndEmit(): Unit = if (totalDemand > 0) try {
    // blocking read
    val buffer = new BufferedReader(new InputStreamReader(is)).readLine()

    buffer.length match {
      case -1 ⇒
        // had nothing to read into this chunk
        log.debug("No more bytes available to read (got `-1` from `read`)")
        onCompleteThenStop()

      case _ ⇒
        readBytesTotal += buffer.length

        // emit immediately, as this is the only chance to do it before we might block again
        onNext(buffer)
    }
  } catch {
    case ex: Exception ⇒
      onErrorThenStop(ex)
  }

  override def postStop(): Unit = {
    super.postStop()

    try {
      if (is ne null) is.close()
    } catch {
      case ex: Exception ⇒
       print(ex)
    }

  }
}

object CmdRunner extends App with Utils {

  implicit val system = ActorSystem("BeaconListener")
  implicit val materializer = ActorMaterializer()
  import scala.sys.process._

  var s : Source[String,NotUsed] = Source.empty

  var dataPublisherRef : ActorRef = ActorRef.noSender

  val connection = system.actorOf( Props(new WebSocketConnection(materializer)), "WebSocketConnection")


  connection ! Connect

  import scala.sys.process.ProcessIO
  val pio = new ProcessIO(_ => (), { stdout =>
    dataPublisherRef = system.actorOf(Props(new InputStreamPublisher(stdout, 10)))
    val dataPublisher = ActorPublisher[String](dataPublisherRef)

    s = Source.fromPublisher(dataPublisher)
    s.runForeach {
      (x: String) =>
        connection ! x
        println(s"Data from $x")
      }
      .onComplete(_ => print("complete"))
    dataPublisherRef ! Continue()
  },
    error => scala.io.Source.fromInputStream(error)
      .getLines.foreach(println))
  args(0).run(pio)
}
