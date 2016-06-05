import java.security.Timestamp
import java.util.Date

/**
  * Created by darioalessandro on 6/5/16.
  */

//UUID: B9407F30-F5F8-466E-AFF9-25556B57FE6D MAJOR: 16914 MINOR: 22626 POWER: -68

case class BeaconUpdate(uuid:String, major:String, minor:String,power:String,timestamp: Date = new Date()) {
  def toJson = s"""{"uuid":"$uuid","major":"$major","minor":"$minor","power":"$power","timestamp","${timestamp.getTime}"}"""
}

object BeaconUpdateGenerator {

  val POWER = " POWER: "
  val MINOR = " MINOR: "
  val MAJOR = " MAJOR: "
  val UUID = "UUID: "

  def parse(string : String) : Option[BeaconUpdate] = {
    val powerStIdx = string.indexOf(POWER)
    val minorStIdx = string.indexOf(MINOR)
    val majorStIdx = string.indexOf(MAJOR)
    val uuidStIdx = string.indexOf(UUID)

    val powerEndIdx = powerStIdx + POWER.length
    val minorEndIdx = minorStIdx + MINOR.length
    val majorEndIdx = majorStIdx + MAJOR.length
    val uuidEndIdx = uuidStIdx + UUID.length

    val length = string.length

    if( powerStIdx == -1 || minorStIdx == -1 || majorStIdx  == -1 || uuidStIdx == -1) {
      None
    } else {
      Some(BeaconUpdate(uuid = string.substring(uuidEndIdx, majorStIdx),
                   major = string.substring(majorEndIdx, minorStIdx),
                   minor = string.substring(minorEndIdx, powerStIdx),
                   power = string.substring(powerEndIdx, length)))


    }
  }
}
