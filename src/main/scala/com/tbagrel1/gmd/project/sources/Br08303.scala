package com.tbagrel1.gmd.project.sources

import java.nio.file.Paths

import com.outr.lucene4s._
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.mapper.Searchable
import com.outr.lucene4s.query.SearchTerm
import com.tbagrel1.gmd.project.sources.Br08303.isContentLine
import com.tbagrel1.gmd.project.{DrugAtc, DrugName, Utils}

import scala.collection.mutable
import scala.io.Source

case class NameAtcRecord(id: Int, name: String, atc: String)

object Br08303Lucene extends DirectLucene(uniqueFields = List("id"), Option(Paths.get("indexes/br08303"))) {
  val nameAtcRecords: SearchableNameAtcRecord = create.searchable[SearchableNameAtcRecord]
}

trait SearchableNameAtcRecord extends Searchable[NameAtcRecord] {
  override def idSearchTerms(nameAtcRecord: NameAtcRecord): List[SearchTerm] = List(exact(id(nameAtcRecord.id)))

  def id: Field[Int]
  val name: Field[String] = Br08303Lucene.create.field("name", FieldType.Stored, false) // "in" matching
  val atc: Field[String]  = Br08303Lucene.create.field("atc", FieldType.Untokenized, false) // exact matching
}

object Br08303 {
  def main(args: Array[String]): Unit = {
    val br08303 = new Br08303
    br08303.createIndex(true)
    println(br08303.drugNameEqDrugAtc(DrugName(Utils.normalize("Poldine"))))
  }

  def isContentLine(line: String): Boolean = {
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ".contains(line.charAt(0)) && line.charAt(1) == ' '
  }
}

class Br08303 {
  import Br08303Lucene._

  def createIndex(verbose: Boolean = false): Unit = {
    var id = 0

    val bufferedFile = Source.fromFile("data_sources/br08303.keg")
    for (line <- bufferedFile.getLines) {
      if (isContentLine(line)) {
        val interestingPart = line.slice(1, line.length).trim
        val parts = interestingPart.split(" ", 2)
        val atc = Utils.normalize(parts(0).trim)
        var name = parts(1).trim
        var parenthesisIndex = name.indexOf(" (")
        if (parenthesisIndex == -1) {
          parenthesisIndex = name.length
        }
        var bracketIndex = name.indexOf(" [")
        if (bracketIndex == -1) {
          bracketIndex = name.length
        }
        name = Utils.normalize(name.slice(0, parenthesisIndex min bracketIndex))
        val record = NameAtcRecord(id, name, atc)
        if (verbose) {
          println(record)
        }
        nameAtcRecords.insert(record).index()
        id += 1
      }
    }
    bufferedFile.close()
    println(s"${id} Br08303Records inserted!")
  }

  def drugNameEqDrugAtc(drugName: DrugName): mutable.Set[DrugAtc] = {
    mutable.Set.from(
      nameAtcRecords.query()
        .filter(exact(nameAtcRecords.name(drugName.value)))
        .search()
        .entries
        .map(nameAtcRecord => DrugAtc(nameAtcRecord.atc)))
  }
  def drugAtcEqDrugName(drugAtc: DrugAtc): mutable.Set[DrugName] = {
    mutable.Set.from(
      nameAtcRecords.query()
        .filter(exact(nameAtcRecords.atc(drugAtc.value)))
        .search()
        .entries
        .map(nameAtcRecord => DrugName(nameAtcRecord.name))
    )
  }
}
