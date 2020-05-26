package com.tbagrel1.gmd.project

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.scaladsl._
import scala.concurrent._
import com.tbagrel1.gmd.project.sources.SourceCatalog

import scala.collection.mutable
import scala.language.postfixOps
import spray.json._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

object WebServer {
  implicit val system: ActorSystem = ActorSystem("MediNodeActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val sources = new SourceCatalog

  type GraphRepr = String
  case class Symptom(value: String, nodeType: String, weight: Double)
  case class Cure(name: String, activation: Double)
  case class SideEffectSource(name: String, activation: Double)
  case class Cause(name: String, activation: Double, level: Int)
  case class ProcessProgress(var stepName: String, var stepNo: Int, var totalStepNb: Int, var symptomDiameters: Map[String, Int], var drugDiameters: Map[String, Int], var symptomQueueSize: Int, var drugQueueSize: Int, var currentlyProcessed: String, var extendedSymptomNames: Option[Seq[String]])
  case class ProcessResult(cures: Seq[Cure], sideEffectSources: Seq[SideEffectSource], causes: Seq[Cause], graphRepr: GraphRepr)
  case class KeepAlive(data: String)

  object JsonProtocol extends DefaultJsonProtocol {
    implicit val symptomFormat: RootJsonFormat[Symptom] = jsonFormat3(Symptom.apply)
    implicit val cureFormat: RootJsonFormat[Cure] = jsonFormat2(Cure.apply)
    implicit val sideEffectSourceFormat: RootJsonFormat[SideEffectSource] = jsonFormat2(SideEffectSource.apply)
    implicit val causeFormat: RootJsonFormat[Cause] = jsonFormat3(Cause.apply)
    implicit val processProgressFormat: RootJsonFormat[ProcessProgress] = jsonFormat9(ProcessProgress.apply)
    implicit val processResultFormat: RootJsonFormat[ProcessResult] = jsonFormat4(ProcessResult.apply)
    implicit val keepAliveFormat: RootJsonFormat[KeepAlive] = jsonFormat1(KeepAlive.apply)
  }

  import JsonProtocol._

  val (progressActor, progressSource) = Source
    .actorRef[ProcessProgress](32, OverflowStrategy.dropNew)
    .preMaterialize()

  val (resultActor, resultSource) = Source
    .actorRef[ProcessResult](32, OverflowStrategy.dropNew)
    .preMaterialize()

  def dotToSvg: GraphRepr = {
    import sys.process._
    "dot -Tsvg graph_output/graph.dot" !!
  }

  def processSymptoms(symptoms: Seq[Symptom]): ProcessResult = {
    val symptomSet = mutable.Set.from(symptoms)
    val graph = new DataGraph(sources, symptomSet.map { case Symptom(value, nodeType, weight) => (value, nodeType, weight) }, progressActor)
    graph.sendLight()
    graph.createDotFile(Parameters.GRAPH_RESULT_LIMIT)
    ProcessResult(
      graph.cures(Parameters.TEXT_RESULT_LIMIT).map { case (v, w, _) => Cure(v, w) },
      graph.sideEffectSources(Parameters.TEXT_RESULT_LIMIT).map { case (v, w, _) => SideEffectSource(v, w) },
      graph.causes(Parameters.TEXT_RESULT_LIMIT).map { case (v, w, l, _) => Cause(v, w, l) },
      dotToSvg
    )
  }

  val handleRequests: Sink[Message, Future[Done]] = Sink.foreach {
      case TextMessage.Strict(text) => {
        try {
          val ast = text.parseJson
          val symptoms = ast.convertTo[Seq[Symptom]]
          println(text)

          Future {
            processSymptoms(symptoms)
          } onComplete {
            tryResult => tryResult.map(result => resultActor ! result)
          }
        } catch {
          case e: Exception => {
            try {
              val ast = text.parseJson
              val pong = ast.convertTo[KeepAlive]
              println("Pong received!")
            } catch {
              case e: Exception => {
                println(s"Received: ${text}")
                e.printStackTrace()
              }
            }
          }
        }
      }
      case _ => throw new Exception("Message type not supported")
    }

  val sendResponses: Source[Message, NotUsed] = progressSource.map(progress => try { val res = TextMessage(progress.toJson.compactPrint); println(res); res } catch { case e: Exception => e.printStackTrace(); TextMessage("Erreur") } ) merge
    resultSource.map(result => try { val res = TextMessage(result.toJson.compactPrint); println(res); res } catch { case e: Exception => e.printStackTrace(); TextMessage("Erreur") }) merge
    Source.fromIterator(() => Iterator.continually(TextMessage(KeepAlive("ping").toJson.compactPrint))).throttle(1, FiniteDuration(1, "second"))

  val route: Route = get {
    path ("websocket") {
      handleWebSocketMessages(Flow.fromSinkAndSource(handleRequests, sendResponses))
    }
  }

  def main(args: Array[String]): Unit = {
    if (args.nonEmpty) {
      sources.createIndex(true)
    }
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
