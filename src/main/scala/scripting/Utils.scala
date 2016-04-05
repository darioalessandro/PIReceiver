package scripting

import java.io.File
import java.security.InvalidParameterException
import scala.concurrent.{Await, Future}
import scala.sys.process._
import scala.util.{Success, Failure, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


trait Utils {


  class CantFindCurrent(message: String = "unable to find current version", cause: Throwable = null) extends RuntimeException(message, cause)

  def fileExists(name: String) = Seq("test", "-f", name).! == 0

  def sourceFilesAt(baseDir: String): Stream[String] = {
    val cmd = Seq("find", baseDir, "-name", "*.scala", "-type", "f")
    cmd.lines
  }

  def listProcessesRunningOnPort(port : Long) : Try[Array[Long]] = {
    try {
      val cmd = exec( s"""fuser -n tcp $port""")
      Success(cmd.split(" ").filter(p => !p.isEmpty).map(n => n.replaceAll("[^0-9]+", "").toLong))
    }catch {
      case e : Throwable =>
        Failure(e)
    }
  }

  def killProcess(port : Long) : Try[String] = {
    try{
      val cmd = exec(s"""kill -SIGTERM $port""")
      Success(cmd)
    }catch {
      case t : Throwable =>
        Failure(t)
    }
  }

  def killProcessesRunningOnPorts(ports : List[Long]) : Try[List[String]] = {
    def processesToKill = ports.map(listProcessesRunningOnPort).filter{ p => p.isSuccess}.map{_.get}

    val f = Future.sequence(processesToKill.flatten.map(l => Future.fromTry(killProcess(l)))).map{
      Success(_)
    }.recover{
      case f : Throwable =>
      Failure(f)
    }
    Await.result(f, 10 seconds)
  }

  def getListOfFiles(dir: String):Try[List[File]] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory)
      scala.util.Success(d.listFiles.filter(_.isFile).toList)
    else
      Failure(new InvalidParameterException("Directory does not exist"))
  }

  def getListOfDirectories(dir: String):Try[List[File]] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory)
      scala.util.Success(d.listFiles.filter(_.isDirectory).toList)
    else
      Failure(new InvalidParameterException("Directory does not exist"))
  }

  def exec(c : String) = {
    println("executing "+c)
    val cmd = c.!!
    println(cmd)
    cmd
  }

  def execAsync(c : String) = {
    println("executing "+c)
    val cmd = c.run()
    println(cmd)
    cmd
  }

  def unzip(origin : File, destination : File) : Try[File] = {
    if(origin.isFile) {
      try {
        val cmd = exec(s"unzip ${origin.getAbsolutePath} -d ${destination.getAbsolutePath}")
        Success(destination)
      } catch {
        case e: Throwable =>
          Failure(e)
      }
    }else{
      Failure(new InvalidParameterException("the origin is not a file"))
    }
  }

  def toTry[A](o : Option[A], t : Throwable) : Try[A] = {
    o.map {
      Success(_)
    }.getOrElse {
      Failure(t)
    }
  }

  def start(startScript : File, opts : String) : Try[File]= {
    try{
      exec(s"""${startScript.getAbsolutePath} $opts""")
      Success(startScript)
    }catch{
      case e:Throwable =>
        Failure(e)
    }
  }

  def startNonBlocking(startScript : File, opts : String) : Try[File]= {
    try{
      val p = execAsync(s"""${startScript.getAbsolutePath} $opts &""")


      Success(startScript)
    }catch{
      case e:Throwable =>
        Failure(e)
    }
  }




}
