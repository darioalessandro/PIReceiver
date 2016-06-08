/**
  * Created by darioalessandro on 6/5/16.
  */

import org.scalatest._


class BeaconParserTests extends FlatSpec with Matchers {

  "BeaconParser" should "parse beacon" in {
    val v = BeaconUpdateGenerator.parse("> 04 3E 2A 02 01 00 01 12 42 62 58 4F DE 1E 02 01 06 1A FF 4C 00 02 15 B9 40 7F 30 F5 F8 46 6E AF F9 25 55 6B 57 FE 6D 42 12 58 62 BC C3")
    val vv = v.get

    vv.uuid shouldEqual "B9407F30-F5F8-466E-AFF9-25556B57FE6D"
    vv.major shouldEqual "16914"
    vv.minor shouldEqual "22626"
    vv.power shouldEqual "-68"
  }

  "BeaconParser" should "parse beacon 2" in {
    val v = BeaconUpdateGenerator.parse("> 04 3E 2A 02 01 00 01 12 42 62 58 4F DE 1E 02 01 06 1A FF 4C   00 02 15 B9 40 7F 30 F5 F8 46 6E AF F9 25 55 6B 57 FE 6D 42   12 58 62 BC D1")
    val vv = v.get

    vv.uuid shouldEqual "B9407F30-F5F8-466E-AFF9-25556B57FE6D"
    vv.major shouldEqual "16914"
    vv.minor shouldEqual "22626"
    vv.power shouldEqual "-68"
  }
}


