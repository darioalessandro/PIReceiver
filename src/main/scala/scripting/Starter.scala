package scripting

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Date
import com.typesafe.config.ConfigFactory

import scala.util.{Failure, Success, Try}

/**
 * Created by Dario Talarico on 7/16/15.
 */

object Starter extends Utils {

  val conf = ConfigFactory.load("start.conf")
  val workingDirectory = conf.getString("workingDirectoryPath")
  val httpPort = conf.getString("httpPort")
  val otherParams = conf.getString("otherParams")
  val currentDirPrefix = "current-"
  val tempDir = "temp"
  val zipDir = "zip"

  println(s"workingDirectory $workingDirectory")

  def showHelp = Failure(new Throwable("available commands= start, stop, restart, deploy {buildnumber}, deployandstart {buildnumber}"))

  def argsParser(a : List[String]) : Try[Any] = {
    a match {
      case List("start") =>
        startCurrentWithOptions(otherParams)

      case List("stop") =>
        killProcessesRunningOnPorts(List(9000))
      case List("restart") =>
        for{ kill <- argsParser(List("stop"))
             start <- argsParser(List("start"))} yield start

      case List("deploy", number) =>
        val b = backupOp
        for{
          unzip <- unzipAndMakeItCurrent(number.toLong)
        } yield unzip

      case List("deployandstart", number) =>
        for{deploy <- argsParser(List("deploy", number))
            stop <- argsParser(List("stop")).recover{case e : Throwable => Success(e)}
            start <- argsParser(List("start"))
        } yield start

      case _ =>
        showHelp
    }
  }

  def startCurrentWithOptions(opts : String) : Try[File] = {

    def spotify(intermediateDir : File) = {
      val binFolder = new File(intermediateDir.getAbsolutePath + "/bin")
      toTry(binFolder.listFiles().find(file => !file.getName.endsWith("bat")),new Throwable("Unable to find play start script"))
    }

    val startScript = for{
      c <- getCurrentFolder
      intermediateDir <- toTry(c.listFiles().headOption, new Throwable("unable to find app directory inside current"))
      _startScript <- spotify(intermediateDir)
    } yield _startScript

    startScript match {
      case Success(s) =>
        start(s,opts)

      case Failure(f) =>
        Failure(f)
    }
  }

  def backupOp = dirInWorkingDirectory.flatMap { files =>
    val current = getCurrentFolder

    val temp : File = files.find(_.getName.equalsIgnoreCase(tempDir)).getOrElse {
      println("creating directory "+workingDirectory+"/"+tempDir)
      Files.createDirectory(Paths.get(workingDirectory+"/"+tempDir)).toFile
    }

    current.map { c =>
      val version = c.getName.replaceFirst(currentDirPrefix,"")
      val dest = Paths.get(temp.getAbsolutePath + "/" +  version + new Date().toString)

      println(s"moving ${c.toPath} to $dest")
      Files.move(c.toPath, dest)

    }
  }

  def dirInWorkingDirectory = getListOfDirectories(workingDirectory)

  def unzipAndMakeItCurrent(version : Long) = {

    val zipsTry = dirInWorkingDirectory.flatMap{ l =>
      toTry(l.find(_.getName.equalsIgnoreCase(zipDir)),
        new Throwable("unable to find zips"))
    }

    val requiredZip = for {zipsDir <- zipsTry
                           zips <- getListOfFiles(zipsDir.getPath)
                           res <- toTry(zips.find(_.getName.endsWith(s"$version-CI.zip")),
                             new Throwable(s"Unable to find zip for version $version")
                           )

    } yield res

    val s= getCurrentFolder match {
      case Failure(f) =>
        requiredZip.map{ r =>
          unzip(r,new File(workingDirectory+"/"+currentDirPrefix+version))
        }

      case Success(w)=>
        Failure(new Throwable("unable to unzip because there's a current version file already"))
    }

    s.flatMap(a=>a)
  }

  def getCurrentFolder = getListOfDirectories(workingDirectory).flatMap { files =>
    toTry(
      files.find(_.getName.startsWith(currentDirPrefix)),
      new CantFindCurrent(s"current could not be found in $workingDirectory")
    )
  }

}
