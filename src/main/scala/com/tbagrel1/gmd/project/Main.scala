package com.tbagrel1.gmd.project

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.Source
import com.tbagrel1.gmd.project.WebServer.ProcessProgress
import com.tbagrel1.gmd.project.sources.SourceCatalog

import scala.collection.mutable

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("MediNodeActorSystem")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val (progressActor, progressSource) = Source
      .actorRef[ProcessProgress](32, OverflowStrategy.dropNew)
      .preMaterialize()
    progressSource.runForeach(println)

    val sources = new SourceCatalog
    if (args.nonEmpty) {
      sources.createIndex(true)
    }
    sources.printStats // Test Tim
    val graph = new DataGraph(sources, mutable.Set(
      ("Penis di.+der", "nameRegex", 1.0),
    ), progressActor)
    graph.sendLight()
    println("Disease causes\n--------------------------------------")
    println(graph.causes(Parameters.TEXT_RESULT_LIMIT))
    println("\n\nSide-effects causes\n--------------------------------------")
    println(graph.sideEffectSources(Parameters.TEXT_RESULT_LIMIT))
    println("\n\nCures\n--------------------------------------")
    println(graph.cures(Parameters.TEXT_RESULT_LIMIT))
    graph.createDotFile(1)
  }
}
