package com.tbagrel1.gmd.project.sources

import com.tbagrel1.gmd.project.{SymptomCui, SymptomName, SymptomOmim}

import scala.collection.mutable

class OmimOntology {
  def symptomNameEqSymptomOmim(symptomName: SymptomName): mutable.Set[SymptomOmim] = { mutable.Set.empty }
  def symptomOmimEqSymptomName(symptomOmim: SymptomOmim): mutable.Set[SymptomName] = { mutable.Set.empty }
  def symptomNameEqSymptomCui(symptomName: SymptomName): mutable.Set[SymptomCui] = { mutable.Set.empty }
  def symptomCuiEqSymptomName(symptomCui: SymptomCui): mutable.Set[SymptomName] = { mutable.Set.empty }
  def symptomOmimEqSymptomCui(symptomOmim: SymptomOmim): mutable.Set[SymptomCui] = { mutable.Set.empty }
  def symptomCuiEqSymptomOmim(symptomCui: SymptomCui): mutable.Set[SymptomOmim] = { mutable.Set.empty }
  def symptomNameSynonymSymptomName(symptomName: SymptomName): mutable.Set[SymptomName] = { mutable.Set.empty }
  def symptomOmimSynonymSymptomName(symptomOmim: SymptomOmim): mutable.Set[SymptomName] = { mutable.Set.empty }
  def symptomCuiSynonymSymptomName(symptomCui: SymptomCui): mutable.Set[SymptomName] = { mutable.Set.empty }
}
