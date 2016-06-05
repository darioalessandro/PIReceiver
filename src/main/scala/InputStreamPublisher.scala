import java.io.{InputStreamReader, BufferedReader, InputStream}

import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisherMessage

/**
  * Created by darioalessandro on 6/5/16.
  */

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
