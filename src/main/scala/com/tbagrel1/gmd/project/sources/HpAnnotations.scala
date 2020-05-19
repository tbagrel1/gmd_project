package com.tbagrel1.gmd.project.sources

import java.sql.{Connection, DriverManager}

import com.tbagrel1.gmd.project.{Attribute, SymptomCui, SymptomHp, SymptomName, SymptomOmim, Utils}

import scala.collection.mutable

object HpAnnotations {
  // TODO: remove -- test code only
  def main(args: Array[String]): Unit = {
    val test = new HpAnnotations
  }
}

class HpAnnotations {
  val databasePath = "jdbc:sqlite:/home/tim/floobits/share/tbagrel1/gmd_project/data_sources/"
  val database = "hpo_annotations.sqlite"
  var connection: Connection = _
  val query: String = "SELECT name " +
    "FROM sqlite_master " +
    "WHERE type = 'table' " +
    "AND name NOT LIKE 'sqlite_%'"
  var x = ""
  try {
    //Class.forName("com.sqlite.jdbc.Driver")
    connection = DriverManager.getConnection(databasePath + database)
    val statement = connection.createStatement()
    val results = statement.executeQuery(query)
    while(results.next()) {
      x = results.getString("name")
      println("%s".format(x))
    }
  } catch {
    case e: Exception => e.printStackTrace()
  }
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

  def genericQueryOmim[A <: Attribute, B <: Attribute](inputColumnName: String, inputColumnValue: A, outputColumnName: String, tableName: String, wrapper: String => B): mutable.Set[B] = {
    val query = s"SELECT DISTINCT ${outputColumnName} FROM ${tableName} WHERE UPPER(${inputColumnName}) = ? AND UPPER(${outputColumnName}) LIKE 'OMIM'"
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

  def symptomNameEqSymptomOmim(symptomName: SymptomName): mutable.Set[SymptomOmim] = {
    genericQuery("disease_label", symptomName, "sign_id", "meddra", SymptomOmim.apply)
  }

  def symptomOmimEqSymptomName(symptomOmim: SymptomOmim): mutable.Set[SymptomName] = { mutable.Set.empty }
  def symptomHpCausedBySymptomName(symptomHp: SymptomHp): mutable.Set[SymptomName] = { mutable.Set.empty }
  def symptomHpCausedBySymptomOmim(symptomHp: SymptomHp): mutable.Set[SymptomOmim] = { mutable.Set.empty }
}
