import sbtrelease.ReleaseStateTransformations._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val tagName = Def.setting{
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}

val Scala212 = "2.12.12"

val tagOrHash = Def.setting {
  if(isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

val unusedWarnings = Def.setting(
  Seq("-Ywarn-unused:imports")
)

val parserCombinatorsVersion = settingKey[String]("")
val xmlVersion = settingKey[String]("")

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
        extracted.runAggregated(PgpKeys.publishSigned in Global in extracted.get(thisProjectRef), state)
      },
      enableCrossBuild = true
    ),
    releaseStepCommandAndRemaining("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  scalaVersion := Scala212,
  crossScalaVersions := Seq(Scala212, "2.13.4"),
  organization := "org.foundweekends",
  scalacOptions in (Compile, doc) ++= {
    val base = (baseDirectory in LocalRootProject).value.getAbsolutePath
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
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq("-Xfuture")
      case _ =>
        Nil
    }
  },
  Seq(Compile, Test).flatMap(c =>
    scalacOptions in (c, console) --= unusedWarnings.value
  ),
)

commonSettings

val knockoff = crossProject(JVMPlatform, JSPlatform)
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    commonSettings,
    buildInfoPackage := "knockoff",
    buildInfoObject := "KnockoffBuildInfo",
    name := "knockoff",
    libraryDependencies ++= {
      if (isDottyJS.value) {
        Nil
      } else {
        Seq("org.scalatest" %%% "scalatest" % "3.2.3" % "test")
      }
    },
    libraryDependencies ++= Seq(
      "net.sf.jtidy" % "jtidy" % "r938" % "test"
    ),
    publishMavenStyle := true,
    publishTo := sonatypePublishToBundle.value,
    publishArtifact in Test := false,
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
          <url>http://github.com/xuwei-k</url>
        </developer>
        <developer>
          <id>tjuricek</id>
          <name>Tristan Juricek</name>
          <url>http://tristanjuricek.com</url>
        </developer>
      </developers>
    ),
    parserCombinatorsVersion := "1.1.2",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-xml" % xmlVersion.value,
      "org.scala-lang.modules" %%% "scala-parser-combinators" % parserCombinatorsVersion.value
    ).map(_.withDottyCompat(scalaVersion.value))
  )
  .jsSettings(
    xmlVersion := "2.0.0-M3",
    scalacOptions ++= {
      val a = (baseDirectory in LocalRootProject).value.toURI.toString
      val g = "https://raw.githubusercontent.com/foundweekends/knockoff/" + tagOrHash.value
      if (isDottyJS.value) {
        // TODO
        // https://github.com/lampepfl/dotty/blob/4c99388e77be12ee6cc/compiler/src/dotty/tools/backend/sjs/JSPositions.scala#L64-L69
        Nil
      } else {
        Seq(s"-P:scalajs:mapSourceURI:$a->$g/")
      }
    },
  )
  .jvmSettings(
    xmlVersion := "1.3.0",
  )

val jvm = knockoff.jvm
val js = knockoff.js

lazy val notPublish = Seq(
  skip in publish := true,
  publishArtifact := false,
  publish := {},
  publishLocal := {},
  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {}
)

notPublish
