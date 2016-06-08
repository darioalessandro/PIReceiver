import java.io.{InputStreamReader, BufferedReader, InputStream}

import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisherMessage

/**
  * Created by darioalessandro on 6/5/16.
  */

class InputStreamPublisher(is: InputStream, chunkSize: Int = 100)
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
    val streamReader = new BufferedReader(new InputStreamReader(is))

    def readOne(buffer:String) {
      val chunk: String = streamReader.readLine()
      log.debug(s"chunk $chunk")
      if(chunk == null) onNext(buffer)

      chunk.head match {

        case '>' ⇒
          // had nothing to read into this chunk
          log.debug("new message, flush previous")
          if(buffer.length > 0) onNext(buffer) else readOne(buffer + chunk)


        case _ ⇒
          readOne(buffer + chunk)
      }
    }
    readOne("")

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
