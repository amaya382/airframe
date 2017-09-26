import ReleaseTransformations._

val SCALA_2_12 = "2.12.3"
val SCALA_2_11 = "2.11.11"
scalaVersion in ThisBuild := SCALA_2_12

val buildSettings = Seq[Setting[_]](
  scalaVersion := SCALA_2_12,
  crossScalaVersions := Seq(SCALA_2_12, SCALA_2_11),
  organization := "org.wvlet",
  crossPaths := true,
  publishMavenStyle := true,
  logBuffered in Test := false,
  updateOptions := updateOptions.value.withCachedResolution(true),
  scalacOptions ++= Seq("-feature", "-deprecation"),
  sonatypeProfileName := "org.wvlet",
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("https://github.com/wvlet/airframe")),
  pomExtra := {
    <scm>
      <connection>scm:git:github.com/wvlet/airframe.git</connection>
      <developerConnection>scm:git:git@github.com:wvlet/airframe.git</developerConnection>
      <url>github.com/wvlet/airframe.git</url>
    </scm>
      <developers>
        <developer>
          <id>leo</id>
          <name>Taro L. Saito</name>
          <url>http://xerial.org/leo</url>
        </developer>
      </developers>
  },
  // Use sonatype resolvers
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  // Release settings
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseTagName := { (version in ThisBuild).value },
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges
  ),
  publishTo := Some(
    if (isSnapshot.value) {
      Opts.resolver.sonatypeSnapshots
    } else {
      Opts.resolver.sonatypeStaging
    }
  )
// Uncomment this after scalafmt-1.3.0 is released
// scalafmtOnCompile in ThisBuild := true,
// scalafmtVersion := "1.3.0"
)

val noPublish = Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)

lazy val airframeRoot =
  Project(id = "airframe-root", base = file("."))
    .settings(buildSettings)
    .settings(noPublish)
    .aggregate(airframeJVM, airframeMacrosJVM, airframeJS, airframeMacrosJS, surfaceJVM, surfaceJS, airframeConfig, jmx, logJVM, logJS, opts)

lazy val projectJVM =
  project.settings(noPublish).aggregate(airframeJVM, surfaceJVM, airframeConfig, jmx, logJVM, opts)

lazy val projectJS =
  project.settings(noPublish).aggregate(airframeJS, surfaceJS, logJS)

lazy val docs = (project in file("docs"))
  .settings(moduleName := "docs")
  .settings(
    publishArtifact := false,
    publish := {},
    publishLocal := {},
    // Necessary for publishMicrosite
    git.remoteRepo := "git@github.com:wvlet/airframe.git",
    ghpagesNoJekyll := false,
    micrositeName := "Airframe",
    micrositeDescription := "Best Practice of Building Service Objects in Scala",
    micrositeAuthor := "Taro L. Saito",
    micrositeOrganizationHomepage := "https://github.com/wvlet",
    micrositeHighlightTheme := "ocean",
    micrositeGithubOwner := "wvlet",
    micrositeGithubRepo := "airframe",
    micrositeBaseUrl := "airframe",
    micrositeAnalyticsToken := "UA-98364158-1",
    micrositeDocumentationUrl := "docs",
    //micrositePushSiteWith := GitHub4s,
    //micrositeGithubToken := sys.env.get("GITHUB_REPO_TOKEN"),
    micrositePalette ++= Map(
      "brand-primary"   -> "#2582AA",
      "brand-secondary" -> "#143F56",
      "brand-tertiary"  -> "#042F46",
      "gray-dark"       -> "#453E46",
      "gray"            -> "#534F54"
    )
  )
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(TutPlugin)
  .enablePlugins(GhpagesPlugin)

lazy val airframe =
  crossProject
    .in(file("airframe"))
    .settings(buildSettings)
    .settings(
      name := "airframe",
      description := "Dependency injection library tailored to Scala",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        // scalatest
        "org.scalatest" %%% "scalatest" % "3.0.1" % "test"
      )
    )
    .jvmSettings(
      // include the macro classes and resources in the main jar
      mappings in (Compile, packageBin) ++= mappings.in(airframeMacrosJVM, Compile, packageBin).value,
      // include the macro sources in the main source jar
      mappings in (Compile, packageSrc) ++= mappings.in(airframeMacrosJVM, Compile, packageSrc).value
    )
    .jsSettings(
      // Workaround for ' JSCom has been closed' issue
      parallelExecution in ThisBuild := false,
      mappings in (Compile, packageBin) ++= mappings.in(airframeMacrosJS, Compile, packageBin).value.filter(x => x._2 != "JS_DEPENDENCIES"),
      // include the macro sources in the main source jar
      mappings in (Compile, packageSrc) ++= mappings.in(airframeMacrosJS, Compile, packageSrc).value
    )
    .dependsOn(surface, airframeMacros % "compile-internal,test-internal")

lazy val airframeJVM = airframe.jvm
lazy val airframeJS  = airframe.js

// Airframe depends on Airframe Macros, so we needed to split the project
lazy val airframeMacros =
  crossProject
    .in(file("airframe-macros"))
    .settings(buildSettings)
    .settings(
      buildSettings,
      name := "airframe-macros",
      description := "Macros for Airframe",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
      ),
      publish := {},
      publishLocal := {}
    )

lazy val airframeMacrosJVM = airframeMacros.jvm
lazy val airframeMacrosJS  = airframeMacros.js

lazy val surface =
  crossProject
    .in(file("surface"))
    .settings(buildSettings)
    .settings(
      name := "surface",
      description := "A library for extracting object structure surface",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
        // scalatest
        "org.scalatest" %%% "scalatest" % "3.0.1" % "test"
      )
    )
    .jsSettings(
      // Workaround for 'JSCom has been closed' issue
      parallelExecution in ThisBuild := false
    )
    .dependsOn(log)

lazy val surfaceJVM = surface.jvm
lazy val surfaceJS  = surface.js

val wvletTest = "org.wvlet" %% "wvlet-test" % "0.27" % "test"

lazy val airframeConfig =
  Project(id = "airframe-config", base = file("airframe-config"))
    .settings(buildSettings)
    .settings(
      description := "airframe configuration module",
      libraryDependencies ++= Seq(
        "org.yaml" % "snakeyaml" % "1.14",
        wvletTest
      )
    )
    .dependsOn(surfaceJVM)

lazy val jmx =
  Project(id = "airframe-jmx", base = file("airframe-jmx"))
    .settings(buildSettings)
    .settings(
      description := "A library for exposing Scala object data through JMX",
      libraryDependencies ++= Seq(
        wvletTest
      )
    )
    .dependsOn(surfaceJVM)

lazy val opts =
  Project(id = "airframe-opts", base = file("airframe-opts"))
    .settings(buildSettings)
    .settings(
      description := "Command-line option parser",
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
        wvletTest
      )
    )
    .dependsOn(surfaceJVM)

lazy val log =
  crossProject
    .in(file("airframe-log"))
    .settings(buildSettings)
    .settings(
      name := "airframe-log",
      description := "Fancy logger for Scala",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
        "org.scalatest"  %%% "scalatest"   % "3.0.1"            % "test"
      )
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "ch.qos.logback" % "logback-core" % "1.1.7"
      )
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-java-logging" % "0.1.1"
      )
    )

lazy val logJVM = log.jvm
lazy val logJS  = log.js