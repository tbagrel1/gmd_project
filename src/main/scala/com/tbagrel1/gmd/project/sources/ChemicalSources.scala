package com.tbagrel1.gmd.project.sources

import java.io.File
import java.nio.file.Paths

import com.github.tototoshi.csv.{CSVReader, DefaultCSVFormat}
import com.outr.lucene4s.{DirectLucene, exact}
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.mapper.Searchable
import com.outr.lucene4s.query.SearchTerm
import com.tbagrel1.gmd.project.{DrugAtc, DrugCompound, DrugName, Utils}

import scala.collection.mutable

case class AtcCompoundRecord(id: Int, atc: String, compound: String)

object ChemicalSourcesLucene extends DirectLucene(uniqueFields = List("id"), Option(Paths.get("indexes/chemical_sources"))) {
  val atcCompoundRecords: SearchableAtcCompoundRecord = create.searchable[SearchableAtcCompoundRecord]
}

trait SearchableAtcCompoundRecord extends Searchable[AtcCompoundRecord] {
  override def idSearchTerms(atcCompoundRecord: AtcCompoundRecord): List[SearchTerm] = List(exact(id(atcCompoundRecord.id)))

  def id: Field[Int]
  val atc: Field[String] = Br08303Lucene.create.field("atc", FieldType.Untokenized, false) // exact matching
  val compound: Field[String]  = Br08303Lucene.create.field("compound", FieldType.Untokenized, false) // exact matching
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

      val record1 = AtcCompoundRecord(id, atc, compound1)
      if (verbose) {
        println(record1)
      }
      atcCompoundRecords.insert(record1).index()
      id += 1

      if (compound1 != compound2) {
        val record2 = AtcCompoundRecord(id, atc, compound2)
        if (verbose) {
          println(record2)
        }
        atcCompoundRecords.insert(record2).index()
        id += 1
      }
    }
    println(s"${id} ChemicalSourceRecords inserted!")
  }

  def drugAtcEqDrugCompound(drugAtc: DrugAtc): mutable.Set[DrugCompound] = {
    mutable.Set.from(
      atcCompoundRecords.query()
        .filter(exact(atcCompoundRecords.atc(drugAtc.value)))
        .search()
        .entries
        .map(atcCompoundRecord => DrugCompound(atcCompoundRecord.compound)))
  }
  def drugCompoundEqDrugAtc(drugCompound: DrugCompound): mutable.Set[DrugAtc] = {
    mutable.Set.from(
      atcCompoundRecords.query()
        .filter(exact(atcCompoundRecords.compound(drugCompound.value)))
        .search()
        .entries
        .map(atcCompoundRecord => DrugAtc(atcCompoundRecord.atc)))
  }
}
