package com.tbagrel1.gmd.project

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl._
import com.tbagrel1.gmd.project.sources.SourceCatalog

import scala.collection.mutable
import scala.io.StdIn

object WebServer {
  import spray.json._

  implicit val system = ActorSystem("MediNodeActorSystem")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val sources = new SourceCatalog

  type Cures = Seq[(String, Double)]
  type SideEffectSources = Seq[(String, Double)]
  type Causes = Seq[(String, Double, Int)]
  type Symptoms = Seq[(String, String, Double)]
  type GraphRepr = String

  case class ProcessProgress(var stepName: String, var stepNo: Int, var totalStepNb: Int, var symptomDiameters: Map[String, Int], var drugDiameters: Map[String, Int], var symptomQueueSize: Int, var drugQueueSize: Int, var currentlyProcessed: String, var extendedSymptomNames: Option[Seq[String]])
  case class ProcessResult(cures: Cures, sideEffectSources: SideEffectSources, causes: Causes, graphRepr: GraphRepr)

  object JsonProtocol extends DefaultJsonProtocol {
    implicit val processProgressFormat: RootJsonFormat[ProcessProgress] = jsonFormat9(ProcessProgress.apply)
    implicit val processResultFormat: RootJsonFormat[ProcessResult] = jsonFormat4(ProcessResult.apply)
  }

  import JsonProtocol._

  val (progressActor, progressSource) = Source
    .actorRef[ProcessProgress](32, OverflowStrategy.dropNew)
    .preMaterialize()

  val (resultActor, resultSource) = Source
    .actorRef[ProcessResult](32, OverflowStrategy.dropNew)
    .preMaterialize()

  def wsService: Flow[Message, Message, _] = Flow.fromSinkAndSource(
    handleRequests,
    progressSource.map(progress => TextMessage(progress.toJson.compactPrint)) merge
      resultSource.map(result => TextMessage(result.toJson.compactPrint))
  )

  def dotToSvg: GraphRepr = {
    import sys.process._
    "dot -Tsvg graph_output/graph.dot" !!
  }

  def processSymptoms(symptoms: Symptoms): ProcessResult = {
    val symptomSet = mutable.Set.from(symptoms)
    val graph = new DataGraph(sources, symptomSet, progressActor)
    graph.sendLight()
    graph.createDotFile(Parameters.GRAPH_RESULT_LIMIT)
    ProcessResult(
      graph.cures(Parameters.TEXT_RESULT_LIMIT).map { case (v, w, _) => (v, w) },
      graph.sideEffectSources(Parameters.TEXT_RESULT_LIMIT).map { case (v, w, _) => (v, w) },
      graph.causes(Parameters.TEXT_RESULT_LIMIT).map { case (v, w, l, _) => (v, w, l) },
      dotToSvg
    )
  }

  def handleRequests: Sink[Message, _] = {
    Sink.foreach {
      case TextMessage.Strict(text) => {
        val ast = text.parseJson
        val symptoms = ast.convertTo[Symptoms]
        resultActor ! processSymptoms(symptoms)
      }
      case _ => throw new Exception("Message type not supported")
    }
  }

  val route = {
    pathEndOrSingleSlash {
      get {
        complete("Welcome to MediNode! The websocket is available at /websocket to make requests.")
      }
    } ~
      path("websocket") {
        get {
          handleWebSocketMessages(wsService)
        }
      }
  }

  def main(args: Array[String]): Unit = {
    if (args.nonEmpty) {
      sources.createIndex(true)
    }
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println("Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
