package com.tbagrel1.gmd.project.data.sources

import com.tbagrel1.gmd.project.data.{SymptomHp, SymptomName}

class HpOntology {
  def symptomNameEqSymptomHp(symptomName: SymptomName): Set[SymptomHp] = { Set.empty }
  def symptomHpEqSymptomName(symptomHp: SymptomHp): Set[SymptomName] = { Set.empty }
  def symptomNameSynonymSymptomName(symptomName: SymptomName): Set[SymptomName] = { Set.empty }
  def symptomHpSynonymSymptomName(symptomHp: SymptomHp): Set[SymptomName] = { Set.empty }
}
