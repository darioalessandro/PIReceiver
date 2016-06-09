import java.security.Timestamp
import java.util.Date

/**
  * Created by darioalessandro on 6/5/16.
  */

//UUID: B9407F30-F5F8-466E-AFF9-25556B57FE6D MAJOR: 16914 MINOR: 22626 POWER: -68

case class BeaconUpdate(uuid:String, major:String, minor:String,power:String,timestamp: Date = new Date()) {
  def toJson = s"""[{"uuid":"$uuid","major":"$major","minor":"$minor","power":"$power","timestamp":"${timestamp.getTime}"}]"""
}

object BeaconUpdateGenerator {

  val POWER = " POWER: "
  val MINOR = " MINOR: "
  val MAJOR = " MAJOR: "
  val UUID = "UUID: "

  def parse(raw : String) : Option[BeaconUpdate] = {

    val bytes = raw.split(" ").filter(c => c != "")
    if(!raw.startsWith("> 04 3E 2A 02 01") || bytes.length != 46) {
      None
    } else {
      val uuid = bytes.slice(24,24+4).mkString+'-'+bytes.slice(24+4,24+4+2).mkString+"-"+bytes.slice(24+4+2,24+4+2+2).mkString+"-"+bytes.slice(24+4+2+2,24+4+2+2+2).mkString+"-"+bytes.slice(24+4+2+2+2,24+4+2+2+2+6).mkString
      val major = Integer.parseInt(bytes.slice(24+16, 24+18).mkString,16).toString
      val minor = Integer.parseInt(bytes.slice(24+18, 24+20).mkString,16).toString
      val rssi =  ( Integer.parseInt(bytes.slice(24+21,24+22).mkString,16) -256 ).toString
      Some(BeaconUpdate(uuid = uuid,
                   major = major,
                   minor = minor,
                   power = rssi))

    }
  }
}
