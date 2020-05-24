package com.tbagrel1.gmd.project.sources

import java.nio.file.Paths

import com.outr.lucene4s.{DirectLucene, exact}
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.mapper.Searchable
import com.outr.lucene4s.query.SearchTerm
import com.tbagrel1.gmd.project.{Parameters, SymptomHp, SymptomName, Utils}

import scala.collection.mutable
import scala.io.Source
import scala.util.parsing.combinator.RegexParsers

case class HpOntologyEqSymptomNameSymptomHpRecord(id: Int, symptomName: String, symptomHp: String)
case class HpOntologySynonymSymptomNameSymptomNameRecord(id: Int, symptomName1: String, symptomName2: String)

class HpOntologyParser extends RegexParsers {
  override def skipWhitespace: Boolean = false

  def propName: Parser[String] = {
    """[a-zA-Z0-9_]+""".r
  }

  def propValue: Parser[String] = {
    """[^\[\n][^\n]+""".r
  }

  def newLine: Parser[Unit] = {
    "\n" ^^^ { }
  }

  def prop: Parser[(String, String)] = {
    ((propName <~ ": ") ~ propValue) <~ newLine ^^ { case thePropName ~ thePropValue => (thePropName, thePropValue) }
  }

  def entity: Parser[mutable.HashMap[String, mutable.ArrayBuffer[String]]] = {
    "[Term]\n" ~> rep1(prop) <~ newLine ^^ { props => {
      val theEntity = mutable.HashMap.empty[String, mutable.ArrayBuffer[String]]
      for ((propName, propValue) <- props) {
        if (!theEntity.contains(propName)) {
          theEntity.put(propName, mutable.ArrayBuffer.empty[String])
        }
        theEntity(propName).addOne(propValue)
      }
      theEntity
    } }
  }

  def allEntities: Parser[List[mutable.HashMap[String, mutable.ArrayBuffer[String]]]] = rep(entity)
}

object HpOntologyLucene extends DirectLucene(uniqueFields = List("eqSymptomNameSymptomHpRecordId", "synonymSymptomNameSymptomNameRecordId"), Option(Paths.get("indexes/hp_ontology"))) {
  val eqSymptomNameSymptomHpRecords: SearchableHpOntologyEqSymptomNameSymptomHpRecord = create.searchable[SearchableHpOntologyEqSymptomNameSymptomHpRecord]
  val synonymSymptomNameSymptomNameRecords: SearchableHpOntologySynonymSymptomNameSymptomNameRecord = create.searchable[SearchableHpOntologySynonymSymptomNameSymptomNameRecord]
}

trait SearchableHpOntologyEqSymptomNameSymptomHpRecord extends Searchable[HpOntologyEqSymptomNameSymptomHpRecord] {
  override def idSearchTerms(eqSymptomNameSymptomHpRecord: HpOntologyEqSymptomNameSymptomHpRecord): List[SearchTerm] = List(exact(id(eqSymptomNameSymptomHpRecord.id)))

  val id: Field[Int] = HpOntologyLucene.create.field("eqSymptomNameSymptomHpRecordId", FieldType.Numeric)
  val symptomName: Field[String] = HpOntologyLucene.create.field("eqSymptomNameSymptomHpRecordSymptomName", Parameters.NAME_FIELD_TYPE, false)
  val symptomHp: Field[String]  = HpOntologyLucene.create.field("eqSymptomNameSymptomHpRecordSymptomHp", FieldType.Untokenized, false) // exact matching
}

trait SearchableHpOntologySynonymSymptomNameSymptomNameRecord extends Searchable[HpOntologySynonymSymptomNameSymptomNameRecord] {
  override def idSearchTerms(synonymSymptomNameSymptomNameRecord: HpOntologySynonymSymptomNameSymptomNameRecord): List[SearchTerm] = List(exact(id(synonymSymptomNameSymptomNameRecord.id)))

  val id: Field[Int] = HpOntologyLucene.create.field("synonymSymptomNameSymptomNameRecordId", FieldType.Numeric)
  val symptomName1: Field[String] = HpOntologyLucene.create.field("synonymSymptomNameSymptomNameRecordSymptomName1", Parameters.NAME_FIELD_TYPE, false)
  val symptomName2: Field[String]  = HpOntologyLucene.create.field("synonymSymptomNameSymptomNameRecordSymptomName2", Parameters.NAME_FIELD_TYPE, false)
}

object HpOntology {
  def main(args: Array[String]): Unit = {
    val hpOntology = new HpOntology
    hpOntology.createIndex(true)
    println(Utils.enrichFlatMapAll(mutable.Set(SymptomName(Utils.normalize("Multicystic renal dysplasia"))), hpOntology.symptomNameSynonymSymptomName, 5))
  }
}

class HpOntology {
  import HpOntologyLucene._

  def createIndex(verbose: Boolean = false): Unit = {
    val bufferedFile = Source.fromFile("data_sources/hp.obo")
    var lines = bufferedFile.getLines().toList
    bufferedFile.close()
    lines = lines.slice(28, lines.length)
    val content = lines.mkString("\n")
    val parser = new HpOntologyParser
    val entities: List[mutable.HashMap[String, mutable.ArrayBuffer[String]]] = parser.parse(parser.allEntities, content) match {
      case parser.Success(result, _) => result.asInstanceOf[List[mutable.HashMap[String, mutable.ArrayBuffer[String]]]]
      case parser.Failure(msg, _) => throw new Exception(s"FAILURE: ${msg}")
      case parser.Error(msg, _) => throw new Exception(s"ERROR: ${msg}")
    }
    var eqId = 0
    var synonymId = 0
    for (entity <- entities) {
      val name = Utils.normalize(entity("name").head)
      for (rawHp <- entity.getOrElse("id", mutable.ArrayBuffer.empty) ++ entity.getOrElse("alt_id", mutable.ArrayBuffer.empty)) {
        val hp = Utils.normalize(Utils.stripPrefix(rawHp, "HP:"))
        val eqRecord = HpOntologyEqSymptomNameSymptomHpRecord(eqId, name, hp)
        if (verbose) {
          println(eqRecord)
        }
        eqSymptomNameSymptomHpRecords.insert(eqRecord).index()
        eqId += 1
      }
      for (rawSynonym <- entity.getOrElse("synonym", mutable.ArrayBuffer.empty)) {
        val end = rawSynonym.slice(1, rawSynonym.length).indexOf('"') + 1
        val synonym = Utils.normalize(rawSynonym.slice(1, end))
        val synonymRecord = HpOntologySynonymSymptomNameSymptomNameRecord(synonymId, name, synonym)
        if (verbose) {
          println(synonymRecord)
        }
        synonymSymptomNameSymptomNameRecords.insert(synonymRecord).index()
        synonymId += 1
      }
    }
  }

  def symptomNameEqSymptomHp(symptomName: SymptomName): mutable.Set[SymptomHp] = {
    mutable.Set.from(
      eqSymptomNameSymptomHpRecords.query()
        .filter(exact(eqSymptomNameSymptomHpRecords.symptomName(symptomName.value)))
        .search()
        .entries
        .map(eqSymptomNameSymptomHpRecord => SymptomHp(eqSymptomNameSymptomHpRecord.symptomHp))
    )
  }

  def symptomHpEqSymptomName(symptomHp: SymptomHp): mutable.Set[SymptomName] = {
    mutable.Set.from(
      eqSymptomNameSymptomHpRecords.query()
        .filter(exact(eqSymptomNameSymptomHpRecords.symptomHp(symptomHp.value)))
        .search()
        .entries
        .map(eqSymptomNameSymptomHpRecord => SymptomName(eqSymptomNameSymptomHpRecord.symptomName))
      )
  }

  def symptomNameSynonymSymptomName(symptomName: SymptomName): mutable.Set[SymptomName] = {
    mutable.Set.from(
      synonymSymptomNameSymptomNameRecords.query()
        .filter(exact(synonymSymptomNameSymptomNameRecords.symptomName1(symptomName.value)))
        .search()
        .entries
        .map(synonymSymptomNameSymptomNameRecord => SymptomName(synonymSymptomNameSymptomNameRecord.symptomName2))
      ) union mutable.Set.from(
      synonymSymptomNameSymptomNameRecords.query()
        .filter(exact(synonymSymptomNameSymptomNameRecords.symptomName2(symptomName.value)))
        .search()
        .entries
        .map(synonymSymptomNameSymptomNameRecord => SymptomName(synonymSymptomNameSymptomNameRecord.symptomName1))
      )
  }
}
