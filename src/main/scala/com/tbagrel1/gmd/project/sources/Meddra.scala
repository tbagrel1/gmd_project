package com.tbagrel1.gmd.project.sources

import java.sql.{Connection, DriverManager}

import com.tbagrel1.gmd.project.{Attribute, DrugCompound, SymptomCui, SymptomName, Utils}

import scala.collection.mutable

object Meddra {
  def main(args: Array[String]): Unit = {
    val meddra = new Meddra
    println(meddra.symptomCuiEqSymptomName(SymptomCui("C0000729")))
    println(meddra.symptomNameEqSymptomCui(SymptomName("MASS IN ABDOMEN")))
    println(meddra.symptomNameCuredByDrugCompound(SymptomName("ABDOMINAL PAIN")))
    println(meddra.symptomCuiCuredByDrugCompound(SymptomCui("C0000729")))
    println(meddra.symptomNameIsSideEffectDrugCompound(SymptomName("ABDOMINAL PAIN")))
    println(meddra.symptomCuiIsSideEffectDrugCompound(SymptomCui("C0000729")))
  }
}

class Meddra {
  val databaseURL = "jdbc:mysql://neptune.telecomnancy.univ-lorraine.fr/"
  val database = "gmd"
  val username = "gmd-read"
  val password = "esial"

  Class.forName("com.mysql.jdbc.Driver")

  val connection: Connection = DriverManager.getConnection(databaseURL + database, username, password)

  def getSymptomNames: mutable.Set[String] = {
    def genericGet(columnName: String, tableName: String): mutable.Set[String] = {
      val query = s"SELECT DISTINCT ${columnName} FROM ${tableName}"
      val statement = connection.prepareStatement(query)

      val results = statement.executeQuery()
      val resultSet = mutable.HashSet.empty[String]
      while (results.next()) {
        val resultString = results.getString(columnName)
        resultSet.addOne(Utils.normalize(resultString))
      }
      resultSet
    }
    genericGet("concept_name", "meddra_all_indications") union
    genericGet("label", "meddra") union
    genericGet("side_effect_name", "meddra_freq") union
    genericGet("side_effect_name", "meddra_all_se")
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

    resultSet.filter(!_.value.isEmpty)
  }

  def symptomNameEqSymptomCui(symptomName: SymptomName): mutable.Set[SymptomCui] = {
    val resultSet1 = genericQuery("label", symptomName, "cui", "meddra", SymptomCui.apply)
    // val resultSet2 = genericQuery("concept_name", symptomName, "cui", "meddra_all_indications", SymptomCui.apply)
    val resultSet3 = genericQuery("concept_name", symptomName, "cui_of_meddra_term", "meddra_all_indications", SymptomCui.apply)
    // val resultSet4 = genericQuery("side_effect_name", symptomName, "cui", "meddra_freq", SymptomCui.apply)
    val resultSet5 = genericQuery("side_effect_name", symptomName, "meddra_concept_id", "meddra_freq", SymptomCui.apply)
    // val resultSet6 = genericQuery("side_effect_name", symptomName, "cui", "meddra_all_se", SymptomCui.apply)
    val resultSet7 = genericQuery("side_effect_name", symptomName, "cui_of_meddra_term", "meddra_all_se", SymptomCui.apply)

    // DO: est-ce qu'il ne faut pas regarder aussi dans all_se et freq ?
    // resultSet1 union resultSet2 union resultSet3 union resultSet4 union resultSet5 union resultSet6 union resultSet7
    resultSet1 union resultSet3 union resultSet5 union resultSet7
  }

  def symptomCuiEqSymptomName(symptomCui: SymptomCui): mutable.Set[SymptomName] = {
    val resultSet1 = genericQuery("cui", symptomCui, "label", "meddra", SymptomName.apply)
    // val resultSet2 = genericQuery("cui", symptomCui, "concept_name", "meddra_all_indications", SymptomName.apply)
    val resultSet3 = genericQuery("cui_of_meddra_term ", symptomCui, "concept_name", "meddra_all_indications", SymptomName.apply)
    // val resultSet4 = genericQuery("cui", symptomCui, "side_effect_name", "meddra_all_se", SymptomName.apply)
    val resultSet5 = genericQuery("cui_of_meddra_term ", symptomCui, "side_effect_name", "meddra_all_se", SymptomName.apply)
    // val resultSet6 = genericQuery("cui", symptomCui, "side_effect_name", "meddra_freq", SymptomName.apply)
    val resultSet7 = genericQuery("meddra_concept_id ", symptomCui, "side_effect_name", "meddra_freq", SymptomName.apply)

    // DO: est-ce qu'il ne faut pas regarder aussi dans all_se et freq ?
    // resultSet1 union resultSet2 union resultSet3 union resultSet4 union resultSet5 union resultSet6 union resultSet7
    resultSet1 union resultSet3 union resultSet5 union resultSet7
  }

  def symptomNameCuredByDrugCompound(symptomName: SymptomName): mutable.Set[DrugCompound] = {
    val resultSet1 = genericQuery("meddra_concept_name", symptomName, "stitch_compound_id", "meddra_all_indications", DrugCompound.apply)
    resultSet1
  }

  def symptomCuiCuredByDrugCompound(symptomCui: SymptomCui): mutable.Set[DrugCompound] = {
    // val resultSet1 = genericQuery("cui", symptomCui, "stitch_compound_id", "meddra_all_indications", DrugCompound.apply)
    val resultSet2 = genericQuery("cui_of_meddra_term", symptomCui, "stitch_compound_id", "meddra_all_indications", DrugCompound.apply)
    // resultSet1 union resultSet2
    resultSet2
  }

  def symptomNameIsSideEffectDrugCompound(symptomName: SymptomName): mutable.Set[DrugCompound] = {
    val resultSet1 = genericQuery("side_effect_name", symptomName, "stitch_compound_id1", "meddra_all_se", DrugCompound.apply)
    val resultSet2 = genericQuery("side_effect_name", symptomName, "stitch_compound_id2", "meddra_all_se", DrugCompound.apply)
    val resultSet3 = genericQuery("side_effect_name", symptomName, "stitch_compound_id1", "meddra_freq", DrugCompound.apply)
    val resultSet4 = genericQuery("side_effect_name", symptomName, "stitch_compound_id2", "meddra_freq", DrugCompound.apply)
    resultSet1 union resultSet2 union resultSet3 union resultSet4
  }

  def symptomCuiIsSideEffectDrugCompound(symptomCui: SymptomCui): mutable.Set[DrugCompound] = {
    // val resultSet1 = genericQuery("cui", symptomCui, "stitch_compound_id1", "meddra_all_se", DrugCompound.apply)
    // val resultSet2 = genericQuery("cui", symptomCui, "stitch_compound_id2", "meddra_all_se", DrugCompound.apply)
    // val resultSet3 = genericQuery("cui", symptomCui, "stitch_compound_id1", "meddra_freq", DrugCompound.apply)
    // val resultSet4 = genericQuery("cui", symptomCui, "stitch_compound_id2", "meddra_freq", DrugCompound.apply)
    val resultSet5 = genericQuery("cui_of_meddra_term", symptomCui, "stitch_compound_id1", "meddra_all_se", DrugCompound.apply)
    val resultSet6 = genericQuery("cui_of_meddra_term", symptomCui, "stitch_compound_id2", "meddra_all_se", DrugCompound.apply)
    val resultSet7 = genericQuery("meddra_concept_id", symptomCui, "stitch_compound_id1", "meddra_freq", DrugCompound.apply)
    val resultSet8 = genericQuery("meddra_concept_id", symptomCui, "stitch_compound_id2", "meddra_freq", DrugCompound.apply)
    // resultSet1 union resultSet2 union resultSet3 union resultSet4 union resultSet5 union resultSet6 union resultSet7 union resultSet8
    resultSet5 union resultSet6 union resultSet7 union resultSet8
  }
}
