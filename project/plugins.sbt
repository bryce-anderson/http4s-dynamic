resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

//addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
addSbtPlugin("com.eed3si9n" % "bintray-sbt" % "0.3.0-a1934a5457f882053b08cbdab5fd4eb3c2d1285d")
resolvers += Resolver.url("bintray-eed3si9n-sbt-plugins", url("https://dl.bintray.com/eed3si9n/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.3")
