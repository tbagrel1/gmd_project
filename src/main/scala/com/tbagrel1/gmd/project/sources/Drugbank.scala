package com.tbagrel1.gmd.project.sources

import com.tbagrel1.gmd.project.{DrugAtc, DrugName, SymptomName}

import scala.collection.mutable

class Drugbank {
  def drugNameEqDrugAtc(drugName: DrugName): mutable.Set[DrugAtc] = { mutable.Set.empty }
  def drugAtcEqDrugName(drugAtc: DrugAtc): mutable.Set[DrugName] = { mutable.Set.empty }
  def drugAtcSynonymDrugName(drugAtc: DrugAtc): mutable.Set[DrugName] = { mutable.Set.empty }
  def drugNameSynonymDrugName(drugName: DrugName): mutable.Set[DrugName] = { mutable.Set.empty }
  def symptomNameCuredByDrugName(symptomName: SymptomName): mutable.Set[DrugName] = { mutable.Set.empty }
  def symptomNameCuredByDrugAtc(symptomName: SymptomName): mutable.Set[DrugAtc] = { mutable.Set.empty }
  def symptomNameIsSideEffectDrugName(symptomName: SymptomName): mutable.Set[DrugName] = { mutable.Set.empty }
  def symptomNameIsSideEffectDrugAtc(symptomName: SymptomName): mutable.Set[DrugName] = { mutable.Set.empty }


}
