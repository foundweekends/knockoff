import sbtrelease.ReleaseStateTransformations._

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value else version.value}"
}

val Scala212 = "2.12.21"

val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

val unusedWarnings = Def.setting(
  Seq("-Ywarn-unused:imports")
)

val scalaVersions = Seq(Scala212, "2.13.18", "3.3.7")

val commonSettings = Def.settings(
  releaseTagName := tagName.value,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("publishSigned"),
    releaseStepCommandAndRemaining("sonaRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  organization := "org.foundweekends",
  (Compile / doc / scalacOptions) ++= {
    val base = (LocalRootProject / baseDirectory).value.getAbsolutePath
    Seq(
      "-sourcepath",
      base,
      "-doc-source-url",
      "https://github.com/foundweekends/knockoff/tree/" + tagOrHash.value + "€{FILE_PATH}.scala"
    )
  },
  scalacOptions ++= unusedWarnings.value,
  scalacOptions ++= Seq("-language:_", "-deprecation", "-Xlint"),
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "3" =>
        Nil
      case "2.13" =>
        Seq("-Xsource:3-cross")
      case _ =>
        Seq("-Xsource:3")
    }
  },
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq("-Xfuture")
      case _ =>
        Nil
    }
  },
  Seq(Compile, Test).flatMap(c => (c / console / scalacOptions) --= unusedWarnings.value),
)

commonSettings

val knockoff = projectMatrix
  .in(file("."))
  .defaultAxes()
  .enablePlugins(BuildInfoPlugin)
  .settings(
    commonSettings,
    Compile / scalaSource := file("shared/src/main/scala").getAbsoluteFile,
    Test / scalaSource := file("shared/src/test/scala").getAbsoluteFile,
    buildInfoPackage := "knockoff",
    buildInfoObject := "KnockoffBuildInfo",
    name := "knockoff",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest-funspec" % "3.2.20" % "test",
      "org.scalatest" %%% "scalatest-shouldmatchers" % "3.2.20" % "test",
    ),
    libraryDependencies ++= Seq(
      "net.sf.jtidy" % "jtidy" % "r938" % "test"
    ),
    publishMavenStyle := true,
    publishTo := (if (isSnapshot.value) None else localStaging.value),
    Test / publishArtifact := false,
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <url>https://github.com/foundweekends/knockoff</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>https://opensource.org/licenses/BSD-3-Clause</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:foundweekends/knockoff.git</url>
        <connection>scm:git:git@github.com:foundweekends/knockoff.git</connection>
      </scm>
      <developers>
        <developer>
          <id>xuwei-k</id>
          <name>Kenji Yoshida</name>
          <url>https://github.com/xuwei-k</url>
        </developer>
        <developer>
          <id>tjuricek</id>
          <name>Tristan Juricek</name>
          <url>https://tristanjuricek.com</url>
        </developer>
      </developers>
    ),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-xml" % "2.4.0",
      "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.4.0"
    )
  )
  .jvmPlatform(
    scalaVersions,
    Def.settings(
      Compile / unmanagedSourceDirectories += file("jvm/src/main/scala").getAbsoluteFile,
      Test / unmanagedSourceDirectories += file("jvm/src/test/scala").getAbsoluteFile,
    )
  )
  .jsPlatform(
    scalaVersions,
    scalacOptions += {
      val a = (LocalRootProject / baseDirectory).value.toURI.toString
      val g = "https://raw.githubusercontent.com/foundweekends/knockoff/" + tagOrHash.value
      val key = CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) =>
          "-scalajs-mapSourceURI"
        case _ =>
          "-P:scalajs:mapSourceURI"
      }
      s"${key}:$a->$g/"
    },
  )
  .nativePlatform(
    scalaVersions,
    Def.settings(
      evictionErrorLevel := Level.Warn,
    ),
  )

lazy val notPublish = Seq(
  publish / skip := true,
  publishArtifact := false,
  publish := {},
  publishLocal := {},
  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {}
)

notPublish
