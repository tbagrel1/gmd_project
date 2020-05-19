package com.tbagrel1.gmd.project.sources

import java.io.File
import java.nio.file.{Files, Paths}

import com.outr.lucene4s.{DirectLucene, exact}
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.mapper.Searchable
import com.outr.lucene4s.query.SearchTerm
import com.tbagrel1.gmd.project.{DrugAtc, DrugName, SymptomName, Utils}
import io.dylemma.spac._
import io.dylemma.spac.xml._

import scala.collection.mutable
import scala.io.Source

// name, atc, synonym, indication, toxicity

case class XmlDrug(name: String, cures: String, sideEffects: String, synonyms: List[XmlSynonym], atcCodes: List[XmlAtcCode])
case class XmlSynonym(synonym: String)
case class XmlAtcCode(atc: String)

case class DrugbankEqDrugNameDrugAtcRecord(id: Int, drugName: String, drugAtc: String)
case class DrugbankSynonymDrugNameDrugNameRecord(id: Int, drugName1: String, drugName2: String)
case class DrugbankCureSideEffectDrugNameRecord(id: Int, drugName: String, cures: String, sideEffects: String)

object DrugbankLucene extends DirectLucene(uniqueFields = List("eqDrugNameDrugAtcRecordId", "synonymDrugNameDrugNameRecordId", "cureSideEffectDrugNameRecordId"), Option(Paths.get("indexes/drugbank"))) {
  val eqDrugNameDrugAtcRecords: SearchableDrugbankEqDrugNameDrugAtcRecord = create.searchable[SearchableDrugbankEqDrugNameDrugAtcRecord]
  val synonymDrugNameDrugNameRecords: SearchableDrugbankSynonymDrugNameDrugNameRecord = create.searchable[SearchableDrugbankSynonymDrugNameDrugNameRecord]
  val cureSideEffectDrugNameRecords: SearchableDrugbankCureSideEffectDrugNameRecord = create.searchable[SearchableDrugbankCureSideEffectDrugNameRecord]
}

trait SearchableDrugbankEqDrugNameDrugAtcRecord extends Searchable[DrugbankEqDrugNameDrugAtcRecord] {
  override def idSearchTerms(eqDrugNameDrugAtcRecord: DrugbankEqDrugNameDrugAtcRecord): List[SearchTerm] = List(exact(id(eqDrugNameDrugAtcRecord.id)))

  val id: Field[Int] = DrugbankLucene.create.field("eqDrugNameDrugAtcRecordId", FieldType.Numeric)
  val drugName: Field[String] = DrugbankLucene.create.field("eqDrugNameDrugAtcRecordDrugName", FieldType.Stored, false) // "in" matching
  val drugAtc: Field[String]  = DrugbankLucene.create.field("eqDrugNameDrugAtcRecordDrugAtc", FieldType.Untokenized, false) // exact matching
}

trait SearchableDrugbankSynonymDrugNameDrugNameRecord extends Searchable[DrugbankSynonymDrugNameDrugNameRecord] {
  override def idSearchTerms(synonymDrugNameDrugNameRecord: DrugbankSynonymDrugNameDrugNameRecord): List[SearchTerm] = List(exact(id(synonymDrugNameDrugNameRecord.id)))

  val id: Field[Int] = DrugbankLucene.create.field("synonymDrugNameDrugNameRecordId", FieldType.Numeric)
  val drugName1: Field[String] = DrugbankLucene.create.field("synonymDrugNameDrugNameRecordDrugName1", FieldType.Stored, false) // "in" matching
  val drugName2: Field[String]  = DrugbankLucene.create.field("synonymDrugNameDrugNameRecordDrugName2", FieldType.Stored, false) // "in" matching
}

trait SearchableDrugbankCureSideEffectDrugNameRecord extends Searchable[DrugbankCureSideEffectDrugNameRecord] {
  override def idSearchTerms(cureSideEffectDrugNameRecord: DrugbankCureSideEffectDrugNameRecord): List[SearchTerm] = List(exact(id(cureSideEffectDrugNameRecord.id)))

  val id: Field[Int] = DrugbankLucene.create.field("cureSideEffectDrugNameRecordId", FieldType.Numeric)
  val drugName: Field[String] = DrugbankLucene.create.field("cureSideEffectDrugNameRecordDrugName", FieldType.Stored, false) // "in" matching
  val cures: Field[String]  = DrugbankLucene.create.field("cureSideEffectDrugNameRecordCures", FieldType.Stored, false) // "in" matching
  val sideEffects: Field[String]  = DrugbankLucene.create.field("cureSideEffectDrugNameRecordSideEffects", FieldType.Stored, false) // "in" matching
}

object Drugbank {
  def main(args: Array[String]): Unit = {
    val drugbank = new Drugbank
    drugbank.createIndex(true)
    println(drugbank.symptomNameCuredByDrugName(SymptomName(Utils.normalize("metastatic colorectal carcinoma"))))
  }
}

class Drugbank {
  import DrugbankLucene._

  implicit val xmlAtcCodeParser: XMLParser[XmlAtcCode] = (
    XMLParser.forMandatoryAttribute("code")
  ).map(XmlAtcCode.apply)

  implicit val xmlSynonymParser: XMLParser[XmlSynonym] = (
    XMLParser.forText
  ).map(XmlSynonym.apply)

  implicit val xmlDrugParser: XMLParser[XmlDrug] = (
    XMLSplitter(* \ "name").first.asText and
    XMLSplitter(* \ "indication").first.asText and
    XMLSplitter(* \ "toxicity").first.asText and
    XMLSplitter(* \ "synonyms" \ "synonym").asListOf[XmlSynonym] and
    XMLSplitter(* \ "atc-codes" \ "atc-code").asListOf[XmlAtcCode]
  ).as(XmlDrug)

  val drugTransformer: XMLTransformer[XmlDrug] = XMLSplitter("drugbank" \ "drug").as[XmlDrug]

  def createIndex(verbose: Boolean = false): Unit = {
    var eqId = 0
    var synonymId = 0
    var cureSideEffectId = 0
    val file = new File("data_sources/drugbank.xml")
      drugTransformer.parseForeach(xmlDrug => {
        val cureSideEffectRecord = DrugbankCureSideEffectDrugNameRecord(cureSideEffectId, Utils.normalize(xmlDrug.name), Utils.normalize(xmlDrug.cures), Utils.normalize(xmlDrug.sideEffects))
        if (verbose) {
          println(cureSideEffectRecord)
        }
        cureSideEffectDrugNameRecords.insert(cureSideEffectRecord).index()
        cureSideEffectId += 1
        for (atcCode <- xmlDrug.atcCodes) {
          val eqRecord = DrugbankEqDrugNameDrugAtcRecord(eqId, Utils.normalize(xmlDrug.name), Utils.normalize(atcCode.atc))
          if (verbose) {
            println(eqRecord)
          }
          eqDrugNameDrugAtcRecords.insert(eqRecord).index()
          eqId += 1
        }
        for (synonym <- xmlDrug.synonyms) {
          val synonymRecord = DrugbankSynonymDrugNameDrugNameRecord(synonymId, Utils.normalize(xmlDrug.name), Utils.normalize(synonym.synonym))
          if (verbose) {
            println(synonymRecord)
          }
          synonymDrugNameDrugNameRecords.insert(synonymRecord).index()
          synonymId += 1
        }
      }) parse file
  }

  def drugNameEqDrugAtc(drugName: DrugName): mutable.Set[DrugAtc] = {
    mutable.Set.from(
      eqDrugNameDrugAtcRecords.query()
        .filter(exact(eqDrugNameDrugAtcRecords.drugName(drugName.value)))
        .search()
        .entries
        .map(eqDrugNameDrugAtcRecord => DrugAtc(eqDrugNameDrugAtcRecord.drugAtc)))
  }
  def drugAtcEqDrugName(drugAtc: DrugAtc): mutable.Set[DrugName] = {
    mutable.Set.from(
      eqDrugNameDrugAtcRecords.query()
        .filter(exact(eqDrugNameDrugAtcRecords.drugAtc(drugAtc.value)))
        .search()
        .entries
        .map(eqDrugNameDrugAtcRecord => DrugName(eqDrugNameDrugAtcRecord.drugName)))
  }
  def drugNameSynonymDrugName(drugName: DrugName): mutable.Set[DrugName] = {
    mutable.Set.from(
      synonymDrugNameDrugNameRecords.query()
        .filter(exact(synonymDrugNameDrugNameRecords.drugName1(drugName.value)))
        .search()
        .entries
        .map(synonymDrugNameDrugNameRecord => DrugName(synonymDrugNameDrugNameRecord.drugName2))
    ) union mutable.Set.from(
      synonymDrugNameDrugNameRecords.query()
        .filter(exact(synonymDrugNameDrugNameRecords.drugName2(drugName.value)))
        .search()
        .entries
        .map(synonymDrugNameDrugNameRecord => DrugName(synonymDrugNameDrugNameRecord.drugName1))
    )
  }
  def symptomNameCuredByDrugName(symptomName: SymptomName): mutable.Set[DrugName] = {
    mutable.Set.from(
      cureSideEffectDrugNameRecords.query()
        .filter(exact(cureSideEffectDrugNameRecords.cures(symptomName.value)))
        .search()
        .entries
        .map(cureSideEffectDrugNameRecord => DrugName(cureSideEffectDrugNameRecord.drugName))
    )
  }
  def symptomNameIsSideEffectDrugName(symptomName: SymptomName): mutable.Set[DrugName] = {
    mutable.Set.from(
      cureSideEffectDrugNameRecords.query()
        .filter(exact(cureSideEffectDrugNameRecords.sideEffects(symptomName.value)))
        .search()
        .entries
        .map(cureSideEffectDrugNameRecord => DrugName(cureSideEffectDrugNameRecord.drugName))
      )
  }
}
