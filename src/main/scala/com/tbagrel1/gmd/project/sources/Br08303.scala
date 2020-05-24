package com.tbagrel1.gmd.project.sources

import java.nio.file.Paths

import com.outr.lucene4s._
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.mapper.Searchable
import com.outr.lucene4s.query.SearchTerm
import com.tbagrel1.gmd.project.sources.Br08303.isContentLine
import com.tbagrel1.gmd.project.{DrugAtc, DrugName, Parameters, Utils}

import scala.collection.mutable
import scala.io.Source

case class Br08303EqDrugNameDrugAtcRecord(id: Int, drugName: String, drugAtc: String)

object Br08303Lucene extends DirectLucene(uniqueFields = List("eqDrugNameDrugAtcRecordId"), Option(Paths.get("indexes/br08303"))) {
  val eqDrugNameDrugAtcRecord: SearchableBr08303EqDrugNameDrugAtcRecord = create.searchable[SearchableBr08303EqDrugNameDrugAtcRecord]
}

trait SearchableBr08303EqDrugNameDrugAtcRecord extends Searchable[Br08303EqDrugNameDrugAtcRecord] {
  override def idSearchTerms(eqDrugNameDrugAtcRecord: Br08303EqDrugNameDrugAtcRecord): List[SearchTerm] = List(exact(id(eqDrugNameDrugAtcRecord.id)))

  val id: Field[Int] = Br08303Lucene.create.field("eqDrugNameDrugAtcRecordId", FieldType.Numeric)
  val drugName: Field[String] = Br08303Lucene.create.field("eqDrugNameDrugAtcRecordDrugName", Parameters.NAME_FIELD_TYPE, false)
  val drugAtc: Field[String]  = Br08303Lucene.create.field("eqDrugNameDrugAtcRecordDrugAtc", FieldType.Untokenized, false) // exact matching
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
        val drugAtc = Utils.normalize(parts(0).trim)
        var drugName = parts(1).trim
        var parenthesisIndex = drugName.indexOf(" (")
        if (parenthesisIndex == -1) {
          parenthesisIndex = drugName.length
        }
        var bracketIndex = drugName.indexOf(" [")
        if (bracketIndex == -1) {
          bracketIndex = drugName.length
        }
        drugName = Utils.normalize(drugName.slice(0, parenthesisIndex min bracketIndex))
        val record = Br08303EqDrugNameDrugAtcRecord(id, drugName, drugAtc)
        if (verbose) {
          println(record)
        }
        eqDrugNameDrugAtcRecord.insert(record).index()
        id += 1
      }
    }
    bufferedFile.close()
    println(s"${id} Br08303EqDrugNameDrugAtcRecords inserted!")
  }

  def drugNameEqDrugAtc(drugName: DrugName): mutable.Set[DrugAtc] = {
    mutable.Set.from(
      eqDrugNameDrugAtcRecord.query()
        .filter(exact(eqDrugNameDrugAtcRecord.drugName(drugName.value)))
        .search()
        .entries
        .map(nameAtcRecord => DrugAtc(nameAtcRecord.drugAtc)))
  }
  def drugAtcEqDrugName(drugAtc: DrugAtc): mutable.Set[DrugName] = {
    mutable.Set.from(
      eqDrugNameDrugAtcRecord.query()
        .filter(exact(eqDrugNameDrugAtcRecord.drugAtc(drugAtc.value)))
        .search()
        .entries
        .map(nameAtcRecord => DrugName(nameAtcRecord.drugName))
      )
  }
}
