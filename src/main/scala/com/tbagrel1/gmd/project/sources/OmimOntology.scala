package com.tbagrel1.gmd.project.sources

import java.io.File
import java.nio.file.Paths

import com.github.tototoshi.csv.{CSVReader, DefaultCSVFormat}
import com.outr.lucene4s.{DirectLucene, exact}
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.mapper.Searchable
import com.outr.lucene4s.query.SearchTerm
import com.tbagrel1.gmd.project.{Parameters, SymptomCui, SymptomName, SymptomOmim, Utils}

import scala.collection.mutable

case class OmimOntologyEqSymptomNameSymptomOmimRecord(id: Int, symptomName: String, symptomOmim: String)
case class OmimOntologyEqSymptomNameSymptomCuiRecord(id: Int, symptomName: String, symptomCui: String)
case class OmimOntologySynonymSymptomNameSymptomNameRecord(id: Int, symptomName1: String, symptomName2: String)

object OmimOntologyLucene extends DirectLucene(appendIfExists = true, uniqueFields = List("eqSymptomNameSymptomOmimRecordId", "eqSymptomNameSymptomCuiRecordId", "synonymSymptomNameSymptomNameId"), directory = Option(Paths.get("indexes/omim_ontology"))) {
  val eqSymptomNameSymptomOmimRecords: SearchableOmimOntologyEqSymptomNameSymptomOmimRecord = create.searchable[SearchableOmimOntologyEqSymptomNameSymptomOmimRecord]
  val eqSymptomNameSymptomCuiRecords: SearchableOmimOntologyEqSymptomNameSymptomCuiRecord = create.searchable[SearchableOmimOntologyEqSymptomNameSymptomCuiRecord]
  val synonymSymptomNameSymptomNameRecords: SearchableOmimOntologySynonymSymptomNameSymptomName = create.searchable[SearchableOmimOntologySynonymSymptomNameSymptomName]
}

trait SearchableOmimOntologyEqSymptomNameSymptomOmimRecord extends Searchable[OmimOntologyEqSymptomNameSymptomOmimRecord] {
  override def idSearchTerms(eqSymptomNameSymptomOmimRecord: OmimOntologyEqSymptomNameSymptomOmimRecord): List[SearchTerm] = List(exact(id(eqSymptomNameSymptomOmimRecord.id)))

  val id: Field[Int] = OmimOntologyLucene.create.field("eqSymptomNameSymptomOmimRecordId", FieldType.Numeric)
  val symptomName: Field[String] = OmimOntologyLucene.create.field("eqSymptomNameSymptomOmimRecordSymptomName", Parameters.NAME_FIELD_TYPE, false)
  val symptomOmim: Field[String]  = OmimOntologyLucene.create.field("eqSymptomNameSymptomOmimRecordSymptomOmim", FieldType.Untokenized, false) // exact matching
}

trait SearchableOmimOntologyEqSymptomNameSymptomCuiRecord extends Searchable[OmimOntologyEqSymptomNameSymptomCuiRecord] {
  override def idSearchTerms(eqSymptomNameSymptomCuiRecord: OmimOntologyEqSymptomNameSymptomCuiRecord): List[SearchTerm] = List(exact(id(eqSymptomNameSymptomCuiRecord.id)))

  val id: Field[Int] = OmimOntologyLucene.create.field("eqSymptomNameSymptomCuiRecordId", FieldType.Numeric)
  val symptomName: Field[String] = OmimOntologyLucene.create.field("eqSymptomNameSymptomCuiRecordSymptomName", Parameters.NAME_FIELD_TYPE, false)
  val symptomCui: Field[String]  = OmimOntologyLucene.create.field("eqSymptomNameSymptomCuiRecordSymptomCui", FieldType.Untokenized, false) // exact matching
}

trait SearchableOmimOntologySynonymSymptomNameSymptomName extends Searchable[OmimOntologySynonymSymptomNameSymptomNameRecord] {
  override def idSearchTerms(synonymSymptomNameSymptomNameRecord: OmimOntologySynonymSymptomNameSymptomNameRecord): List[SearchTerm] = List(exact(id(synonymSymptomNameSymptomNameRecord.id)))

  val id: Field[Int] = OmimOntologyLucene.create.field("synonymSymptomNameSymptomNameRecordId", FieldType.Numeric)
  val symptomName1: Field[String] = OmimOntologyLucene.create.field("synonymSymptomNameSymptomNameRecordSymptomName1", Parameters.NAME_FIELD_TYPE, false)
  val symptomName2: Field[String]  = OmimOntologyLucene.create.field("synonymSymptomNameSymptomNameRecordSymptomName2", Parameters.NAME_FIELD_TYPE, false)
}

object OmimOntology {
  def main(args: Array[String]): Unit = {
    val omimOntology = new OmimOntology
    omimOntology.createIndex(true)
    println(omimOntology.symptomNameSynonymSymptomName(SymptomName(Utils.normalize("CAML1"))))
  }
}

class OmimOntology {
  import OmimOntologyLucene._

  implicit object OmimOntologyCsvFormat extends DefaultCSVFormat {
    override val delimiter: Char = ','
  }

  def createIndex(verbose: Boolean = false): Unit = {
    val reader = CSVReader.open(new File("data_sources/omim_onto.csv"))
    var eqNameOmimId = 0
    var eqNameCuiId = 0
    var synonymId = 0

    val it = reader.iterator
    it.next()
    for (fields <- it if fields.length >= 6) {
      val name = Utils.normalize(fields(1))
      val synonyms = fields(2).split('|').map(Utils.normalize)
      val cuis = fields(5).split('|').map(Utils.normalize)

      if (fields(0).startsWith("http://purl.bioontology.org/ontology/OMIM/")) {
        val omim = Utils.normalize(Utils.stripPrefix(fields(0), "http://purl.bioontology.org/ontology/OMIM/"))
        if (!name.isEmpty && !omim.isEmpty) {
          val eqNameOmimRecord = OmimOntologyEqSymptomNameSymptomOmimRecord(eqNameOmimId, name, omim)
          if (verbose) {
            println(eqNameOmimRecord)
          }
          eqSymptomNameSymptomOmimRecords.insert(eqNameOmimRecord).index()
          eqNameOmimId += 1
        }
      }

      for (cui <- cuis) {
        if (!name.isEmpty && !cui.isEmpty) {
          val eqNameCuiRecord = OmimOntologyEqSymptomNameSymptomCuiRecord(eqNameCuiId, name, cui)
          if (verbose) {
            println(eqNameCuiRecord)
          }
          eqSymptomNameSymptomCuiRecords.insert(eqNameCuiRecord).index()
          eqNameCuiId += 1
        }
      }

      for (synonym <- synonyms) {
        if (!name.isEmpty && !synonym.isEmpty) {
          val synonymRecord = OmimOntologySynonymSymptomNameSymptomNameRecord(synonymId, name, synonym)
          if (verbose) {
            println(synonymRecord)
          }
          synonymSymptomNameSymptomNameRecords.insert(synonymRecord).index()
          synonymId += 1
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
    ) union mutable.Set.from(
      eqSymptomNameSymptomCuiRecords.query().limit(Parameters.NO_LIMIT)
        .search()
        .entries
        .map(eqSymptomNameSymptomCuiRecord => eqSymptomNameSymptomCuiRecord.symptomName)
    ) union mutable.Set.from(
      synonymSymptomNameSymptomNameRecords.query().limit(Parameters.NO_LIMIT)
        .search()
        .entries
        .map(synonymSymptomNameSymptomNameRecord => synonymSymptomNameSymptomNameRecord.symptomName2)
    ) union  mutable.Set.from(
      synonymSymptomNameSymptomNameRecords.query().limit(Parameters.NO_LIMIT)
        .search()
        .entries
        .map(synonymSymptomNameSymptomNameRecord => synonymSymptomNameSymptomNameRecord.symptomName1)
    )
  }


  def getSymptomCui: mutable.Set[String] = {
    mutable.Set.from(
      eqSymptomNameSymptomCuiRecords.query().limit(Parameters.NO_LIMIT)
        .search()
        .entries
        .map(eqSymptomNameSymptomCuiRecord => eqSymptomNameSymptomCuiRecord.symptomCui))
  }

  def getSymptomOmim: mutable.Set[String] = {
    mutable.Set.from(
      eqSymptomNameSymptomOmimRecords.query().limit(Parameters.NO_LIMIT)
        .search()
        .entries
        .map(eqSymptomNameSymptomOmimRecord => eqSymptomNameSymptomOmimRecord.symptomOmim))
  }

  def symptomNameEqSymptomOmim(symptomName: SymptomName): mutable.Set[SymptomOmim] = {
    mutable.Set.from(
      eqSymptomNameSymptomOmimRecords.query().limit(Parameters.NO_LIMIT)
        .filter(exact(eqSymptomNameSymptomOmimRecords.symptomName(symptomName.value)))
        .search()
        .entries
        .map(eqSymptomNameSymptomOmimRecord => SymptomOmim(eqSymptomNameSymptomOmimRecord.symptomOmim)))
  }

  def symptomOmimEqSymptomName(symptomOmim: SymptomOmim): mutable.Set[SymptomName] = {
    mutable.Set.from(
      eqSymptomNameSymptomOmimRecords.query().limit(Parameters.NO_LIMIT)
        .filter(exact(eqSymptomNameSymptomOmimRecords.symptomOmim(symptomOmim.value)))
        .search()
        .entries
        .map(eqSymptomNameSymptomOmimRecord => SymptomName(eqSymptomNameSymptomOmimRecord.symptomName)))
  }

  def symptomNameEqSymptomCui(symptomName: SymptomName): mutable.Set[SymptomCui] = {
    mutable.Set.from(
      eqSymptomNameSymptomCuiRecords.query().limit(Parameters.NO_LIMIT)
        .filter(exact(eqSymptomNameSymptomCuiRecords.symptomName(symptomName.value)))
        .search()
        .entries
        .map(eqSymptomNameSymptomCuiRecord => SymptomCui(eqSymptomNameSymptomCuiRecord.symptomCui)))
  }

  def symptomCuiEqSymptomName(symptomCui: SymptomCui): mutable.Set[SymptomName] = {
    mutable.Set.from(
      eqSymptomNameSymptomCuiRecords.query().limit(Parameters.NO_LIMIT)
        .filter(exact(eqSymptomNameSymptomCuiRecords.symptomCui(symptomCui.value)))
        .search()
        .entries
        .map(eqSymptomNameSymptomCuiRecord => SymptomName(eqSymptomNameSymptomCuiRecord.symptomName)))
  }

  def symptomNameSynonymSymptomName(symptomName: SymptomName): mutable.Set[SymptomName] = {
    mutable.Set.from(
      synonymSymptomNameSymptomNameRecords.query().limit(Parameters.NO_LIMIT)
        .filter(exact(synonymSymptomNameSymptomNameRecords.symptomName1(symptomName.value)))
        .search()
        .entries
        .map(synonymSymptomNameSymptomNameRecord => SymptomName(synonymSymptomNameSymptomNameRecord.symptomName2))
    ) union  mutable.Set.from(
      synonymSymptomNameSymptomNameRecords.query().limit(Parameters.NO_LIMIT)
        .filter(exact(synonymSymptomNameSymptomNameRecords.symptomName2(symptomName.value)))
        .search()
        .entries
        .map(synonymSymptomNameSymptomNameRecord => SymptomName(synonymSymptomNameSymptomNameRecord.symptomName1))
      )
  }
}
