package com.tbagrel1.gmd.project.sources

import java.sql.{Connection, DriverManager}

import com.tbagrel1.gmd.project.{Attribute, Parameters, SymptomHp, SymptomName, SymptomOmim, Utils}

import scala.collection.mutable

object HpAnnotations {
  def main(args: Array[String]): Unit = {
    println(getNames(SymptomName(Utils.normalize("ACROMICRIC DYSPLASIA"))))
    println(
      getNames(
        SymptomName(
          Utils.normalize(
            "#102500 HAJDU-CHENEY SYNDROME; HJCYS;;ACROOSTEOLYSIS WITH OSTEOPOROSIS AND CHANGES IN SKULL AND" +
              " MANDIBLE;;CHENEY SYNDROME;;ARTHRODENTOOSTEODYSPLASIA;;SERPENTINE FIBULA-POLYCYSTIC KIDNEY " +
              "SYNDROME; SFPKS"
            )
          )
        )
      )
    val hpAnnotations = new HpAnnotations
    println(hpAnnotations.symptomHpCausedBySymptomName(SymptomHp(Utils.normalize("0000263"))))
  }

  def getNames(symptomName: SymptomName): mutable.Set[SymptomName] = {
    val pattern = ".[0-9]{3,}".r
    (if (pattern.findPrefixOf(symptomName.value).isDefined) {
      val strippedNames = symptomName.value.strip
      mutable.Set
        .from(
          strippedNames.splitAt(strippedNames.indexOf(' '))
            ._2
            .replace('\n', ' ')
            .split(';')
            .filterNot(_.isEmpty)
            .map(Utils.normalize)
          )
    } else {
      mutable.Set(symptomName.value)
    }).map(SymptomName.apply)
  }
}
class HpAnnotations {
  val databasePath = "jdbc:sqlite:./data_sources/"
  val database = "hpo_annotations.sqlite"
  var connection: Connection = DriverManager.getConnection(databasePath + database)

  def getSymptomNames: mutable.Set[String] = {
    val query = s"SELECT DISTINCT disease_label FROM phenotype_annotation"
    val statement = connection.prepareStatement(query)
    val results = statement.executeQuery()
    val resultSet = mutable.HashSet.empty[String]
    while (results.next()) {
      val resultString = results.getString("disease_label")
      resultSet.addOne (Utils.normalize (resultString) )
    }
    resultSet
  }

  def getSymtpomHp: mutable.Set[String] = {
    val query = s"SELECT DISTINCT sign_id FROM phenotype_annotation"
    val statement = connection.prepareStatement(query)
    val results = statement.executeQuery()
    val resultSet = mutable.HashSet.empty[String]
    while (results.next()) {
      val resultString = results.getString("sign_id")
      resultSet.addOne (Utils.codeNormalize(resultString))
    }
    resultSet
  }

  def getSymptomOmim: mutable.Set[String] = {
    val query = s"SELECT DISTINCT disease_db_and_id FROM phenotype_annotation WHERE disease_db_and_id LIKE 'OMIM:%'"
    val statement = connection.prepareStatement(query)
    val results = statement.executeQuery()
    val resultSet = mutable.HashSet.empty[String]
    while (results.next()) {
      val resultString = results.getString("disease_db_and_id")
      resultSet.addOne(Utils.codeNormalize(resultString))
    }
    resultSet
  }

  def genericQuery[A <: Attribute, B <: Attribute](inputColumnName: String, inputColumnValue: A, outputColumnName: String, tableName: String, wrapper: String => B, exact: Boolean): mutable.Set[B] = {
    val query = if (exact) {
      s"SELECT DISTINCT ${outputColumnName} FROM ${tableName} WHERE UPPER(${inputColumnName}) = ?"
    } else {
      s"SELECT DISTINCT ${outputColumnName} FROM ${tableName} WHERE UPPER(${inputColumnName}) LIKE ?"
    }
    val statement = connection.prepareStatement(query)
    statement.setString(1, inputColumnValue.value)

    val results = statement.executeQuery()
    val resultSet = mutable.HashSet.empty[B]
    while (results.next()) {
      val resultString = results.getString(outputColumnName)
      resultSet.addOne(wrapper(resultString))
    }

    resultSet.filter(!_.value.isEmpty)
  }

  def genericQueryOmimOutput[A <: Attribute, B <: Attribute](inputColumnName: String, inputColumnValue: A, outputColumnName: String, tableName: String, wrapper: String => B, exact: Boolean): mutable.Set[B] = {
    val query = if (exact) {
      s"SELECT DISTINCT ${outputColumnName} FROM ${tableName} WHERE UPPER(${inputColumnName}) = ? AND ${outputColumnName} LIKE 'OMIM:%'"
    } else {
      // val blabla = s"%${inputColumnValue.value}%"
      s"SELECT DISTINCT ${outputColumnName} FROM ${tableName} WHERE UPPER(${inputColumnName}) LIKE ? AND ${outputColumnName} LIKE 'OMIM:%'"
    }
    val statement = connection.prepareStatement(query)
    statement.setString(1, inputColumnValue.value)

    val results = statement.executeQuery()
    val resultSet = mutable.HashSet.empty[B]
    while (results.next()) {
      val resultString = results.getString(outputColumnName)
      resultSet.addOne(wrapper(Utils.normalize(Utils.stripPrefix(resultString, "OMIM:"))))
    }

    resultSet.filter(!_.value.isEmpty)
  }

  def symptomNameEqSymptomOmim(symptomName: SymptomName): mutable.Set[SymptomOmim] = {
    genericQueryOmimOutput("disease_label", symptomName, "disease_db_and_id", "phenotype_annotation", SymptomOmim.apply, Parameters.EXACT_SQL)
  }

  def symptomOmimEqSymptomName(symptomOmim: SymptomOmim): mutable.Set[SymptomName] = {
    val symptomOmimWithPrefix = SymptomOmim("OMIM:" + symptomOmim.value)
    genericQuery("disease_db_and_id", symptomOmimWithPrefix, "disease_label", "phenotype_annotation", SymptomName.apply, Parameters.EXACT_SQL).flatMap(HpAnnotations.getNames)
  }

  def symptomHpCausedBySymptomName(symptomHp: SymptomHp): mutable.Set[SymptomName] = {
    val symptomHpWithPrefix = SymptomHp("HP:" + symptomHp.value)
    genericQuery("sign_id", symptomHpWithPrefix, "disease_label", "phenotype_annotation", SymptomName.apply, Parameters.EXACT_SQL).flatMap(HpAnnotations.getNames)
  }

  def symptomHpCausedBySymptomOmim(symptomHp: SymptomHp): mutable.Set[SymptomOmim] = {
    val symptomHpWithPrefix = SymptomHp("HP:" + symptomHp.value)
    genericQueryOmimOutput("sign_id", symptomHpWithPrefix, "disease_db_and_id", "phenotype_annotation", SymptomOmim.apply, Parameters.EXACT_SQL)
  }
}
