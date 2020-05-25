package com.tbagrel1.gmd.project

import com.tbagrel1.gmd.project.sources.SourceCatalog

import scala.collection.mutable

object Main {
  def main(args: Array[String]): Unit = {
    val sources = new SourceCatalog
    sources.createIndex(true)
    println("\n\n\n")
    val graph = new DataGraph(sources, mutable.HashMap(
      (Utils.normalize("abdominal pain"), ("name", 2.0)),
      (Utils.normalize("headache"), ("name", 1.0))
    ))
    graph.sendLight()
    println("Disease causes\n--------------------------------------")
    val causes = graph.causes
    println(causes.slice(0, 10 min causes.length))
    println("\n\nSide-effects causes\n--------------------------------------")
    val sideEffectSources = graph.sideEffectSources
    println(sideEffectSources.slice(0, 10 min sideEffectSources.length))
    println("\n\nCures\n--------------------------------------")
    val cures = graph.cures
    println(cures.slice(0, 10 min cures.length))
  }
}
