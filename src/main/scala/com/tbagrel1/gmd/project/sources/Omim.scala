package com.tbagrel1.gmd.project.sources

import java.nio.file.Paths
import java.util.regex.Pattern

import com.outr.lucene4s.{DirectLucene, exact}
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.mapper.Searchable
import com.outr.lucene4s.query.SearchTerm
import com.tbagrel1.gmd.project.{Parameters, SymptomName, SymptomOmim, Utils}

import scala.collection.mutable
import scala.io.Source

case class OmimCauseSymptomOmimRecord(id: Int, symptomOmim: String, symptoms: String)
case class OmimEqSymptomNameSymptomOmimRecord(id: Int, symptomName: String, symptomOmim: String)

object OmimLucene extends DirectLucene(appendIfExists = true, uniqueFields = List("causeSymptomNameRecordId"), directory = Option(Paths.get("indexes/omim"))) {
  val causeSymptomOmimRecords: SearchableOmimCauseSymptomOmimRecord = create.searchable[SearchableOmimCauseSymptomOmimRecord]
  val eqSymptomNameSymptomOmimRecords: SearchableOmimEqSymptomNameSymptomOmimRecord = create.searchable[SearchableOmimEqSymptomNameSymptomOmimRecord]
}

trait SearchableOmimCauseSymptomOmimRecord extends Searchable[OmimCauseSymptomOmimRecord] {
  override def idSearchTerms(causeSymptomOmimRecord: OmimCauseSymptomOmimRecord): List[SearchTerm] = List(exact(id(causeSymptomOmimRecord.id)))

  val id: Field[Int] = OmimLucene.create.field("causeSymptomOmimRecordId", FieldType.Numeric)
  val symptomOmim: Field[String] = OmimLucene.create.field("causeSymptomOmimRecordSymptomName", FieldType.Untokenized, false)
  val symptoms: Field[String]  = OmimLucene.create.field("causeSymptomOmimRecordSymptoms", FieldType.Stored, false) // "in" matching
}

trait SearchableOmimEqSymptomNameSymptomOmimRecord extends Searchable[OmimEqSymptomNameSymptomOmimRecord] {
  override def idSearchTerms(eqSymptomNameSymptomOmimRecord: OmimEqSymptomNameSymptomOmimRecord): List[SearchTerm] = List(exact(id(eqSymptomNameSymptomOmimRecord.id)))

  val id: Field[Int] = OmimLucene.create.field("eqSymptomNameSymptomOmimRecordId", FieldType.Numeric)
  val symptomName: Field[String] = OmimLucene.create.field("eqSymptomNameSymptomOmimRecordSymptomName", Parameters.NAME_FIELD_TYPE, false)
  val symptomOmim: Field[String]  = OmimLucene.create.field("eqSymptomNameSymptomOmimRecordSymptomOmim", FieldType.Untokenized, false) // exact matching
}

object Omim {
  def main(args: Array[String]): Unit = {
    val omim = new Omim
    omim.createIndex(true)
    println(omim.symptomNameCausedBySymptomOmim(SymptomName(Utils.normalize("Generalized dilating diathesis"))))
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
    var causeId = 0
    var eqId = 0
    for (omimRecord <- records) {
      var rawTi: String = null
      var rawCs: String = null
      var rawNo: String = null
      for ((fieldName, fieldValue) <- omimRecord) {
        if (fieldName == "TI") {
          rawTi = fieldValue
        } else if (fieldName == "CS") {
          rawCs = fieldValue
        } else if (fieldName == "NO") {
          rawNo = fieldValue
        }
      }
      if (rawTi != null && rawCs != null && rawNo != null) {
        val omim = Utils.normalize(rawNo)
        val strippedTi = rawTi.strip
        val ti = strippedTi.splitAt(strippedTi.indexOf(' '))._2.replace('\n', ' ').split(';').filterNot(_.isEmpty).map(Utils.normalize)
        val cs = Utils.normalize(rawCs)
        for (name <- ti) {
          val eqRecord = OmimEqSymptomNameSymptomOmimRecord(eqId, name, omim)
          if (!name.isEmpty && !omim.isEmpty) {
            if (verbose) {
              println(eqRecord)
            }
            eqSymptomNameSymptomOmimRecords.insert(eqRecord).index()
            eqId += 1
          }
        }
        if (!omim.isEmpty && !cs.isEmpty) {
          val causeRecord = OmimCauseSymptomOmimRecord(causeId, omim, cs)
          if (verbose) {
            println(causeRecord)
          }
          causeSymptomOmimRecords.insert(causeRecord).index()
          causeId += 1
        }
      }
    }
    commit()
  }

  def getSymptomNames: mutable.Set[String] = {
    mutable.Set.from(
      eqSymptomNameSymptomOmimRecords.query().limit(Parameters.NO_LIMIT)
        .search()
        .entries
        .map(eqSymptomNameSymptomOmimRecord => eqSymptomNameSymptomOmimRecord.symptomName)
    )
  }

  def getSymptomOmim: mutable.Set[String] = {
    mutable.Set.from(
      eqSymptomNameSymptomOmimRecords.query().limit(Parameters.NO_LIMIT)
        .search()
        .entries
        .map(eqSymptomNameSymptomOmimRecord => eqSymptomNameSymptomOmimRecord.symptomOmim)
    ) union mutable.Set.from(
      causeSymptomOmimRecords.query().limit(Parameters.NO_LIMIT)
        .search()
        .entries
        .map(causeSymptomOmimRecord => causeSymptomOmimRecord.symptomOmim)
    )
  }

  def symptomNameEqSymptomOmim(symptomName: SymptomName): mutable.Set[SymptomOmim] = {
    mutable.Set.from(
      eqSymptomNameSymptomOmimRecords.query().limit(Parameters.NO_LIMIT)
        .filter(exact(eqSymptomNameSymptomOmimRecords.symptomName(symptomName.value)))
        .search()
        .entries
        .map(eqSymptomNameSymptomOmimRecord => SymptomOmim(eqSymptomNameSymptomOmimRecord.symptomOmim))
    )
  }

  def symptomOmimEqSymptomName(symptomOmim: SymptomOmim): mutable.Set[SymptomName] = {
    mutable.Set.from(
      eqSymptomNameSymptomOmimRecords.query().limit(Parameters.NO_LIMIT)
        .filter(exact(eqSymptomNameSymptomOmimRecords.symptomOmim(symptomOmim.value)))
        .search()
        .entries
        .map(eqSymptomNameSymptomOmimRecord => SymptomName(eqSymptomNameSymptomOmimRecord.symptomName))
    )
  }

  def symptomNameCausedBySymptomOmim(symptomName: SymptomName): mutable.Set[SymptomOmim] = {
    mutable.Set.from(
      causeSymptomOmimRecords.query().limit(Parameters.NO_LIMIT)
        .filter(exact(causeSymptomOmimRecords.symptoms(symptomName.value)))
        .search()
        .entries
        .map(causeSymptomOmimRecord => SymptomOmim(causeSymptomOmimRecord.symptomOmim))
      )
  }
}
