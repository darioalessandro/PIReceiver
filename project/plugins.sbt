logLevel := Level.Warn

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.1.0-RC2")
