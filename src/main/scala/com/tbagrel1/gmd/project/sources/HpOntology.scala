package com.tbagrel1.gmd.project.sources

import com.tbagrel1.gmd.project.{SymptomHp, SymptomName}

import scala.collection.mutable
import scala.io.Source
import scala.util.parsing.combinator.RegexParsers

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

object HpOntology {
  def main(args: Array[String]): Unit = {
    val hpOntology = new HpOntology
    hpOntology.createIndex(true)
  }
}

class HpOntology {
  def createIndex(verbose: Boolean = false): Unit = {
    val bufferedFile = Source.fromFile("data_sources/hp.obo")
    var lines = bufferedFile.getLines().toList
    lines = lines.slice(28, lines.length)
    val content = lines.mkString("\n")
    println(content.slice(0, 500))
    val parser = new HpOntologyParser
    parser.parse(parser.allEntities, content) match {
      case parser.Success(result, _) => println(result)
      case parser.Failure(msg, _) => println(s"FAILURE: ${msg}")
      case parser.Error(msg, _) => println(s"ERROR: ${msg}")
    }

    bufferedFile.close()
  }

  // name (stored) <-> id, alt_ids (untokenized, with strip prefix)
  // name (stored) <-> synonyms (tokenized, and bidirectional) /!\ ne pas prendre is_a
  // commence à la ligne 28

  def symptomNameEqSymptomHp(symptomName: SymptomName): mutable.Set[SymptomHp] = {
    mutable.Set.empty
  }

  def symptomHpEqSymptomName(symptomHp: SymptomHp): mutable.Set[SymptomName] = {
    mutable.Set.empty
  }

  def symptomNameSynonymSymptomName(symptomName: SymptomName): mutable.Set[SymptomName] = {
    mutable.Set.empty
  }
}
