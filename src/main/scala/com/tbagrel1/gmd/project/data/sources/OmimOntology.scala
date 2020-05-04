package com.tbagrel1.gmd.project.data.sources

import com.tbagrel1.gmd.project.data.{SymptomCui, SymptomName, SymptomOmim}

class OmimOntology {
  def symptomNameEqSymptomOmim(symptomName: SymptomName): Set[SymptomOmim] = { Set.empty }
  def symptomOmimEqSymptomName(symptomOmim: SymptomOmim): Set[SymptomName] = { Set.empty }
  def symptomNameEqSymptomCui(symptomName: SymptomName): Set[SymptomCui] = { Set.empty }
  def symptomCuiEqSymptomName(symptomCui: SymptomCui): Set[SymptomName] = { Set.empty }
  def symptomOmimEqSymptomCui(symptomOmim: SymptomOmim): Set[SymptomCui] = { Set.empty }
  def symptomCuiEqSymptomOmim(symptomCui: SymptomCui): Set[SymptomOmim] = { Set.empty }
  def symptomNameSynonymSymptomName(symptomName: SymptomName): Set[SymptomName] = { Set.empty }
  def symptomOmimSynonymSymptomName(symptomOmim: SymptomOmim): Set[SymptomName] = { Set.empty }
  def symptomCuiSynonymSymptomName(symptomCui: SymptomCui): Set[SymptomName] = { Set.empty }
}
