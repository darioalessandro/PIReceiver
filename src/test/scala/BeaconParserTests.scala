/**
  * Created by darioalessandro on 6/5/16.
  */

import org.scalatest._


class BeaconParserTests extends FlatSpec with Matchers {

  "BeaconParser" should "parse beacon" in {

    val v = BeaconUpdateGenerator.parse("UUID: B9407F30-F5F8-466E-AFF9-25556B57FE6D MAJOR: 16914 MINOR: 22626 POWER: -68")
    val vv = v.get

    vv.uuid shouldEqual "B9407F30-F5F8-466E-AFF9-25556B57FE6D"
    vv.major shouldEqual "16914"
    vv.minor shouldEqual "22626"
    vv.power shouldEqual "-68"
  }
}
