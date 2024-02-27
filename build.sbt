import sbtrelease.ReleaseStateTransformations._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val tagName = Def.setting{
  s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value else version.value}"
}

val Scala212 = "2.12.19"

val tagOrHash = Def.setting {
  if(isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

val unusedWarnings = Def.setting(
  Seq("-Ywarn-unused:imports")
)

val commonSettings = Def.settings(
  releaseTagName := tagName.value,
  releaseCrossBuild := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(
      action = { state =>
        val extracted = Project extract state
        extracted.runAggregated(extracted.get(thisProjectRef) / (Global / PgpKeys.publishSigned), state)
      },
      enableCrossBuild = true
    ),
    releaseStepCommandAndRemaining("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  scalaVersion := Scala212,
  crossScalaVersions := Seq(Scala212, "2.13.13", "3.3.1"),
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
  Seq(Compile, Test).flatMap(c =>
    (c / console / scalacOptions) --= unusedWarnings.value
  ),
)

commonSettings

val knockoff = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    commonSettings,
    buildInfoPackage := "knockoff",
    buildInfoObject := "KnockoffBuildInfo",
    name := "knockoff",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest-funspec" % "3.2.18" % "test",
      "org.scalatest" %%% "scalatest-shouldmatchers" % "3.2.18" % "test",
    ),
    libraryDependencies ++= Seq(
      "net.sf.jtidy" % "jtidy" % "r938" % "test"
    ),
    publishMavenStyle := true,
    publishTo := sonatypePublishToBundle.value,
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
      "org.scala-lang.modules" %%% "scala-xml" % "2.2.0",
      "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.3.0"
    )
  )
  .jsSettings(
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

val jvm = knockoff.jvm
val js = knockoff.js

lazy val notPublish = Seq(
  publish / skip := true,
  publishArtifact := false,
  publish := {},
  publishLocal := {},
  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {}
)

notPublish
