name := "Squibbly"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "mysql" % "mysql-connector-java" % "5.1.27",
  "org.mindrot" % "jbcrypt" % "0.3m",
    "commons-io" % "commons-io" % "2.4",
    "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0"
)     

play.Project.playJavaSettings
