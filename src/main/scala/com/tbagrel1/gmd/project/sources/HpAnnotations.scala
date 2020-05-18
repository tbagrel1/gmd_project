package com.tbagrel1.gmd.project.sources

import java.sql.{Connection, DriverManager}

import com.tbagrel1.gmd.project.{SymptomHp, SymptomName, SymptomOmim}

import scala.collection.mutable

object HpAnnotations {
  // TODO: remove -- test code only
  def main(args: Array[String]): Unit = {
    val test = new HpAnnotations
  }
}

class HpAnnotations {
  def symptomNameEqSymptomOmim(symptomName: SymptomName): mutable.Set[SymptomOmim] = { mutable.Set.empty }
  def symptomOmimEqSymptomName(symptomOmim: SymptomOmim): mutable.Set[SymptomName] = { mutable.Set.empty }
  def symptomHpCausedBySymptomName(symptomHp: SymptomHp): mutable.Set[SymptomName] = { mutable.Set.empty }
  def symptomHpCausedBySymptomOmim(symptomHp: SymptomHp): mutable.Set[SymptomOmim] = { mutable.Set.empty }

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
  connection.close()
}
