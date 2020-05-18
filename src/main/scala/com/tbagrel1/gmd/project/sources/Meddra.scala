package com.tbagrel1.gmd.project.sources

import java.sql.{Connection, DriverManager, PreparedStatement}

import com.tbagrel1.gmd.project.{Attribute, DrugCompound, SymptomCui, SymptomName, Utils}

import scala.collection.mutable

object Meddra {
  // TODO: remove -- test code only
  def main(args: Array[String]): Unit = {
    val meddra = new Meddra
    println(meddra.symptomCuiEqSymptomName(SymptomCui("C0015544")))
    println(meddra.symptomNameEqSymptomCui(SymptomName("MASS IN ABDOMEN")))
  }
}

class Meddra {
  val databaseURL = "jdbc:mysql://neptune.telecomnancy.univ-lorraine.fr/"
  val database = "gmd"
  val username = "gmd-read"
  val password = "esial"

  Class.forName("com.mysql.jdbc.Driver")
  val connection: Connection = DriverManager.getConnection(databaseURL + database, username, password)

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

  def symptomNameEqSymptomCui(symptomName: SymptomName): mutable.Set[SymptomCui] = {
    val resultSet1 = genericQuery("label", symptomName, "cui", "meddra", SymptomCui.apply)
    val resultSet2 = genericQuery("concept_name", symptomName, "cui", "meddra_all_indications", SymptomCui.apply)
    val resultSet3 = genericQuery("concept_name", symptomName, "cui_of_meddra_term", "meddra_all_indications", SymptomCui.apply)
    resultSet1 union resultSet2 union resultSet3
  }

  def symptomCuiEqSymptomName(symptomCui: SymptomCui): mutable.Set[SymptomName] = {
    val resultSet1 = genericQuery("cui", symptomCui, "label", "meddra", SymptomName.apply)
    val resultSet2 = genericQuery("cui", symptomCui, "concept_name", "meddra_all_indications", SymptomName.apply)
    val resultSet3 = genericQuery("cui_of_meddra_term ", symptomCui, "concept_name", "meddra_all_indications", SymptomName.apply)
    resultSet1 union resultSet2 union resultSet3
  }
  def symptomNameCuredByDrugCompound(symptomName: SymptomName): mutable.Set[DrugCompound] = { mutable.Set.empty }
  def symptomCuiCuredByDrugCompound(symptomCui: SymptomCui): mutable.Set[DrugCompound] = { mutable.Set.empty }
  def symptomNameIsSideEffectDrugCompound(symptomName: SymptomName): mutable.Set[DrugCompound] = { mutable.Set.empty }
  def symptomCuiIsSideEffectDrugCompound(symptomCui: SymptomCui): mutable.Set[DrugCompound] = { mutable.Set.empty }
}
