package com.tbagrel1.gmd.project.data.sources

import com.tbagrel1.gmd.project.data.{DrugAtc, DrugName, SymptomName}

class Drugbank {
  def drugNameEqDrugAtc(drugName: DrugName): Set[DrugAtc] = { Set.empty }
  def drugAtcEqDrugName(drugAtc: DrugAtc): Set[DrugName] = { Set.empty }
  def drugAtcSynonymDrugName(drugAtc: DrugAtc): Set[DrugName] = { Set.empty }
  def drugNameSynonymDrugName(drugName: DrugName): Set[DrugName] = { Set.empty }
  def symptomNameCuredByDrugName(symptomName: SymptomName): Set[DrugName] = { Set.empty }
  def symptomNameCuredByDrugAtc(symptomName: SymptomName): Set[DrugAtc] = { Set.empty }
  def symptomNameIsSideEffectDrugName(symptomName: SymptomName): Set[DrugName] = { Set.empty }
  def symptomNameIsSideEffectDrugAtc(symptomName: SymptomName): Set[DrugName] = { Set.empty }
}
