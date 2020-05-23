package com.tbagrel1.gmd.project.sources

import java.sql.{Connection, DriverManager}

import com.tbagrel1.gmd.project.{Attribute, SymptomCui, SymptomHp, SymptomName, SymptomOmim, Utils}

import scala.collection.mutable

object HpAnnotations {
  def main(args: Array[String]): Unit = {
    val hpAnnotations = new HpAnnotations
    println(hpAnnotations.symptomHpCausedBySymptomName(SymptomHp(Utils.normalize("0000263"))))
  }
}

class HpAnnotations {
  val databasePath = "jdbc:sqlite:/home/tim/floobits/share/tbagrel1/gmd_project/data_sources/"
  val database = "hpo_annotations.sqlite"
  var connection: Connection = DriverManager.getConnection(databasePath + database)

  // TODO: est-ce que pour les recherches SQL sur un name, on fait une recherche approchée (avec LIKE au lieu de =) ?

  def genericQuery[A <: Attribute, B <: Attribute](inputColumnName: String, inputColumnValue: A, outputColumnName: String, tableName: String, wrapper: String => B): mutable.Set[B] = {
    val query = s"SELECT DISTINCT ${outputColumnName} FROM ${tableName} WHERE UPPER(${inputColumnName}) = ?"
    val statement = connection.prepareStatement(query)
    statement.setString(1, inputColumnValue.value)

    val results = statement.executeQuery()
    val resultSet = mutable.HashSet.empty[B]
    while (results.next()) {
      val resultString = results.getString(outputColumnName)
      resultSet.addOne(wrapper(Utils.normalize(resultString)))
    }

    resultSet
  }

  def genericQueryOmimOutput[A <: Attribute, B <: Attribute](inputColumnName: String, inputColumnValue: A, outputColumnName: String, tableName: String, wrapper: String => B): mutable.Set[B] = {
    val query = s"SELECT DISTINCT ${outputColumnName} FROM ${tableName} WHERE UPPER(${inputColumnName}) = ? AND ${outputColumnName} LIKE 'OMIM:%'"
    val statement = connection.prepareStatement(query)
    statement.setString(1, inputColumnValue.value)

    val results = statement.executeQuery()
    val resultSet = mutable.HashSet.empty[B]
    while (results.next()) {
      val resultString = results.getString(outputColumnName)
      resultSet.addOne(wrapper(Utils.normalize(Utils.stripPrefix(resultString, "OMIM:"))))
    }

    resultSet
  }

  def symptomNameEqSymptomOmim(symptomName: SymptomName): mutable.Set[SymptomOmim] = {
    genericQueryOmimOutput("disease_label", symptomName, "disease_db_and_id", "hpo_annotations", SymptomOmim.apply)
  }

  def symptomOmimEqSymptomName(symptomOmim: SymptomOmim): mutable.Set[SymptomName] = {
    val symptomOmimWithPrefix = SymptomOmim("OMIM:" + symptomOmim.value)
    genericQuery("disease_db_and_id", symptomOmimWithPrefix, "disease_label", "hpo_annotation", SymptomName.apply)
  }
  def symptomHpCausedBySymptomName(symptomHp: SymptomHp): mutable.Set[SymptomName] = {
    val symptomHpWithPrefix = SymptomHp("HP:" + symptomHp.value)
    genericQuery("sign_id", symptomHpWithPrefix, "disease_label", "hpo_annotation", SymptomName.apply)
  }
  def symptomHpCausedBySymptomOmim(symptomHp: SymptomHp): mutable.Set[SymptomOmim] = {
    val symptomHpWithPrefix = SymptomHp("HP:" + symptomHp.value)
    genericQueryOmimOutput("sign_id", symptomHpWithPrefix, "disease_db_and_id", "hpo_annotations", SymptomOmim.apply)
  }
}
