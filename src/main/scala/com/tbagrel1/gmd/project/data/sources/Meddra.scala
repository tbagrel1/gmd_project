package com.tbagrel1.gmd.project.data.sources

import java.sql.{Connection, DriverManager}

import com.tbagrel1.gmd.project.data.{DrugCompound, SymptomCui, SymptomName}

object Meddra {
  // TODO: remove -- test code only
  def main(args: Array[String]): Unit = {
    val test = new Meddra
  }
}

class Meddra {
  def symptomNameEqSymptomCui(symptomName: SymptomName): Set[SymptomCui] = { Set.empty }
  def symptomCuiEqSymptomName(symptomCui: SymptomCui): Set[SymptomName] = { Set.empty }
  def symptomNameCuredByDrugCompound(symptomName: SymptomName): Set[DrugCompound] = { Set.empty }
  def symptomCuiCuredByDrugCompound(symptomCui: SymptomCui): Set[DrugCompound] = { Set.empty }
  def symptomNameIsSideEffectDrugCompound(symptomName: SymptomName): Set[DrugCompound] = { Set.empty }
  def symptomCuiIsSideEffectDrugCompound(symptomCui: SymptomCui): Set[DrugCompound] = { Set.empty }

  val driver = "com.mysql.jdbc.Driver"
  val databaseURL = "jdbc:mysql://neptune.telecomnancy.univ-lorraine.fr"
  val database = "gmd"
  val username = "gmd-read"
  val password = "esial"
  var connection: Connection = _
  val query: String = "SELECT TABLE_NAME " +
    "FROM information_schema.TABLES "
  var x = ""
  try {
    Class.forName(driver).newInstance()
    connection = DriverManager.getConnection(databaseURL+database, username, password)
    val statement = connection.createStatement()
    val results = statement.executeQuery(query)
    while(results.next()) {
      x = results.getString("table_name")
      println("%s".format(x))
    }
  } catch {
    case e: Exception => e.printStackTrace()
  }
  connection.close()
}
