package com.tbagrel1.gmd.project.sources

import java.nio.file.Paths
import java.util.regex.Pattern

import com.outr.lucene4s.{DirectLucene, exact}
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.mapper.Searchable
import com.outr.lucene4s.query.SearchTerm
import com.tbagrel1.gmd.project.{Parameters, SymptomName, Utils}

import scala.collection.mutable
import scala.io.Source

case class OmimCauseSymptomNameRecord(id: Int, symptomName: String, symptoms: String)

object OmimLucene extends DirectLucene(uniqueFields = List("causeSymptomNameRecordId"), Option(Paths.get("indexes/omim"))) {
  val causeSymptomNameRecords: SearchableOmimCauseSymptomNameRecord = create.searchable[SearchableOmimCauseSymptomNameRecord]
}

trait SearchableOmimCauseSymptomNameRecord extends Searchable[OmimCauseSymptomNameRecord] {
  override def idSearchTerms(causeSymptomNameRecord: OmimCauseSymptomNameRecord): List[SearchTerm] = List(exact(id(causeSymptomNameRecord.id)))

  val id: Field[Int] = OmimLucene.create.field("causeSymptomNameRecordId", FieldType.Numeric)
  val symptomName: Field[String] = OmimLucene.create.field("causeSymptomNameRecordSymptomName", Parameters.NAME_FIELD_TYPE, false)
  val symptoms: Field[String]  = OmimLucene.create.field("causeSymptomNameRecordSymptoms", FieldType.Stored, false) // "in" matching
}

object Omim {
  def main(args: Array[String]): Unit = {
    val omim = new Omim
    omim.createIndex(true)
    println(omim.symptomNameCausedBySymptomName(SymptomName(Utils.normalize("Generalized dilating diathesis"))))
  }
}

class Omim {
  import OmimLucene._

  def createIndex(verbose: Boolean = false): Unit = {
    val bufferedFile = Source.fromFile("data_sources/omim.txt")
    val content = bufferedFile.getLines().mkString("\n")
    bufferedFile.close()
    val records = content.split(Pattern.quote("*RECORD*\n")).tail.map(rawFields => rawFields.split(Pattern.quote("*FIELD* ")).tail.map(fieldWithTitle => {
      fieldWithTitle.splitAt(fieldWithTitle.indexOf('\n'))
    }))
    var id = 0
    for (record <- records) {
      var rawTi: String = null
      var rawCs: String = null
      for ((fieldName, fieldValue) <- record) {
        if (fieldName == "TI") {
          rawTi = fieldValue
        } else if (fieldName == "CS") {
          rawCs = fieldValue
        }
      }
      if (rawTi != null && rawCs != null) {
        val ti = rawTi.splitAt(rawTi.indexOf(' '))._2.replace('\n', ' ').split(';').filterNot(_.isEmpty).map(Utils.normalize)
        val cs = Utils.normalize(rawCs)
        for (name <- ti) {
          val record = OmimCauseSymptomNameRecord(id, name, cs)
          if (verbose) {
            println(record)
          }
          causeSymptomNameRecords.insert(record).index()
          id += 1
        }
      }
    }
  }

  def symptomNameCausedBySymptomName(symptomName: SymptomName): mutable.Set[SymptomName] = {
    mutable.Set.from(
      causeSymptomNameRecords.query()
        .filter(exact(causeSymptomNameRecords.symptoms(symptomName.value)))
        .search()
        .entries
        .map(causeSymptomNameRecord => SymptomName(causeSymptomNameRecord.symptomName))
    )
  }
}
