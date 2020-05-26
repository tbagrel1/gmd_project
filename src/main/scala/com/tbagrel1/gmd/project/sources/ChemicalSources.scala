package com.tbagrel1.gmd.project.sources

import java.io.File
import java.nio.file.Paths

import com.github.tototoshi.csv.{CSVReader, DefaultCSVFormat}
import com.outr.lucene4s.{DirectLucene, exact}
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.mapper.Searchable
import com.outr.lucene4s.query.SearchTerm
import com.tbagrel1.gmd.project.{DrugAtc, DrugCompound, Parameters, Utils}

import scala.collection.mutable

case class ChemicalSourcesEqDrugAtcDrugCompoundRecord(id: Int, drugAtc: String, drugCompound: String)

object ChemicalSourcesLucene extends DirectLucene(appendIfExists = true, uniqueFields = List("eqDrugAtcDrugCompoundRecordId"), directory = Option(Paths.get("indexes/chemical_sources"))) {
  val eqDrugAtcDrugCompoundRecords: SearchableChemicalSourcesEqDrugAtcDrugCompoundRecord = create.searchable[SearchableChemicalSourcesEqDrugAtcDrugCompoundRecord]
}

trait SearchableChemicalSourcesEqDrugAtcDrugCompoundRecord extends Searchable[ChemicalSourcesEqDrugAtcDrugCompoundRecord] {
  override def idSearchTerms(eqDrugAtcDrugCompoundRecord: ChemicalSourcesEqDrugAtcDrugCompoundRecord): List[SearchTerm] = List(exact(id(eqDrugAtcDrugCompoundRecord.id)))

  val id: Field[Int] = ChemicalSourcesLucene.create.field("eqDrugAtcDrugCompoundRecordId", FieldType.Numeric)
  val drugAtc: Field[String] = ChemicalSourcesLucene.create.field("eqDrugAtcDrugCompoundRecordDrugAtc", FieldType.Untokenized, false) // exact matching
  val drugCompound: Field[String]  = ChemicalSourcesLucene.create.field("eqDrugAtcDrugCompoundRecordDrugCompound", FieldType.Untokenized, false) // exact matching
}

object ChemicalSources {
  def main(args: Array[String]): Unit = {
    val chemicalSources = new ChemicalSources
    chemicalSources.createIndex(true)
    println(chemicalSources.drugCompoundEqDrugAtc(DrugCompound(Utils.normalize("CID100028871"))))
  }
}

class ChemicalSources {
  import ChemicalSourcesLucene._

  implicit object ChemicalSourcesCsvFormat extends DefaultCSVFormat {
    override val delimiter: Char = '\t'
  }

  def createIndex(verbose: Boolean = false): Unit = {
    val reader = CSVReader.open(new File("data_sources/chemical.sources.v5.0.tsv"))
    var id = 0
    for ((fields, i) <- reader.iterator.zipWithIndex if i >= 9 && fields.length == 4 && fields(2) == "ATC") {
      val compound1 = Utils.normalize(fields(0).map(c => if (c == 's' || c == 'm') { '1' } else { c }))
      val compound2 = Utils.normalize(fields(1).map(c => if (c == 's' || c == 'm') { '1' } else { c }))
      val atc = Utils.normalize(fields(3))

      if (!atc.isEmpty && !compound1.isEmpty) {
        val record1 = ChemicalSourcesEqDrugAtcDrugCompoundRecord(id, atc, compound1)
        if (verbose) {
          println(record1)
        }
        eqDrugAtcDrugCompoundRecords.insert(record1).index()
        id += 1
      }

      if (!atc.isEmpty && !compound2.isEmpty && compound1 != compound2) {
        val record2 = ChemicalSourcesEqDrugAtcDrugCompoundRecord(id, atc, compound2)
        if (verbose) {
          println(record2)
        }
        eqDrugAtcDrugCompoundRecords.insert(record2).index()
        id += 1
      }
    }
    commit()
  }

  def getDrugAtc: mutable.Set[String] = {
    mutable.Set.from(
      eqDrugAtcDrugCompoundRecords.query().limit(Parameters.NO_LIMIT)
        .search()
        .entries
        .map(atcCompoundRecord => atcCompoundRecord.drugAtc))
  }

  def getDrugCompound: mutable.Set[String] = {
    mutable.Set.from(
      eqDrugAtcDrugCompoundRecords.query().limit(Parameters.NO_LIMIT)
        .search()
        .entries
        .map(atcCompoundRecord => atcCompoundRecord.drugCompound))
  }

  def drugAtcEqDrugCompound(drugAtc: DrugAtc): mutable.Set[DrugCompound] = {
    mutable.Set.from(
      eqDrugAtcDrugCompoundRecords.query().limit(Parameters.NO_LIMIT)
        .filter(exact(eqDrugAtcDrugCompoundRecords.drugAtc(drugAtc.value)))
        .search()
        .entries
        .map(atcCompoundRecord => DrugCompound(atcCompoundRecord.drugCompound)))
  }
  def drugCompoundEqDrugAtc(drugCompound: DrugCompound): mutable.Set[DrugAtc] = {
    mutable.Set.from(
      eqDrugAtcDrugCompoundRecords.query().limit(Parameters.NO_LIMIT)
        .filter(exact(eqDrugAtcDrugCompoundRecords.drugCompound(drugCompound.value)))
        .search()
        .entries
        .map(atcCompoundRecord => DrugAtc(atcCompoundRecord.drugAtc)))
  }
}
