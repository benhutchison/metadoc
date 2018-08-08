import scalapb.compiler.Version.scalapbVersion
import scalajsbundler.util.JSON._

inThisBuild(
  List(
    version ~= { old =>
      val suffix =
        if (sys.props.contains("metadoc.snapshot")) "-SNAPSHOT" else ""
      old.replace('+', '-') + suffix
    },
    organization := "org.scalameta",
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("https://github.com/scalameta/metadoc")),
    autoAPIMappings := true,
    apiURL := Some(url("https://scalameta.github.io/metadoc")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/scalameta/metadoc"),
        "scm:git:git@github.com:scalameta/metadoc.git"
      )
    ),
    publishTo := Some {
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    },
    developers := List(
      Developer(
        "olafurpg",
        "Ólafur Páll Geirsson",
        "olafurpg@users.noreply.github.com",
        url("https://geirsson.com")
      ),
      Developer(
        "jonas",
        "Jonas Fonseca",
        "jonas@users.noreply.github.com",
        url("https://github.com/jonas")
      )
    ),
  )
)

lazy val Version = new {
  def scala = "2.12.4"
  def scala210 = "2.10.6"
  def scalameta = "3.7.0"
  def sbt = "1.2.0"
  def sbt013 = "0.13.17"
  def circe = "0.9.3"
}

lazy val allSettings = Seq(
  scalaVersion := Version.scala,
  crossScalaVersions := Seq(Version.scala),
  crossSbtVersions := Nil,
  scalacOptions := Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked"
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.0.3" % Test,
    "org.scalacheck" %%% "scalacheck" % "1.13.5" % Test
  )
)




lazy val js = project
  .in(file("metadoc-js"))
  .disablePlugins(ScriptedPlugin) // sbt/sbt#3514 fixed in sbt 1.2
  .settings(
    noPublish,
    moduleName := "metadoc-js",
    additionalNpmConfig in Compile := Map("private" -> bool(true)),
    additionalNpmConfig in Test := additionalNpmConfig.in(Test).value,
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",
    scalaJSUseMainModuleInitializer := true,
    version in webpack := "3.10.0",
    version in startWebpackDevServer := "2.11.1",
    useYarn := true,
    emitSourceMaps := false, // Disabled to reduce warnings
    webpackConfigFile := Some(baseDirectory.value / "webpack.config.js"),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.2"
    ),
    npmDevDependencies in Compile ++= Seq(
      "copy-webpack-plugin" -> "4.3.1",
      "css-loader" -> "0.28.9",
      "extract-text-webpack-plugin" -> "3.0.2",
      "file-loader" -> "1.1.6",
      "html-webpack-plugin" -> "2.30.1",
      "image-webpack-loader" -> "4.1.0",
      "material-design-icons" -> "3.0.1",
      "material-components-web" -> "0.21.1",
      "node-sass" -> "4.7.2",
      "sass-loader" -> "6.0.6",
      "style-loader" -> "0.20.1",
      "ts-loader" -> "3.4.0",
      "typescript" -> "2.6.2",
      "webpack-merge" -> "4.1.1"
    ),
    npmDependencies in Compile ++= Seq(
      "monaco-editor" -> "0.10.1",
      "roboto-fontface" -> "0.7.0",
      "js-sha512" -> "0.4.0"
    )
  )
  .dependsOn(coreJS)
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)

lazy val core = crossProject
  .crossType(CrossType.Pure)
  .in(file("metadoc-core"))
  .disablePlugins(ScriptedPlugin) // sbt/sbt#3514 fixed in sbt 1.2
  .jsSettings(noPublish)
  .settings(
    allSettings,
    moduleName := "metadoc-core",
    PB.targets.in(Compile) := Seq(
      scalapb.gen(
        flatPackage = true // Don't append filename to package
      ) -> sourceManaged.in(Compile).value./("protobuf")
    ),
    PB.protoSources.in(Compile) := Seq(
      // necessary workaround for crossProjects.
      baseDirectory.value./("../src/main/protobuf")
    ),
    libraryDependencies ++= List(
      "io.suzaku" %%% "boopickle" % "1.3.0",
      "org.scalameta" %%% "langmeta" % Version.scalameta,
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapbVersion,

    )
  )
lazy val coreJVM = core.jvm
lazy val coreJS = core.js


lazy val noPublish = allSettings ++ Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)
