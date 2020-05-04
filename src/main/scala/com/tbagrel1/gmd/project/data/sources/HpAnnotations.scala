package com.tbagrel1.gmd.project.data.sources

import com.tbagrel1.gmd.project.data.{SymptomHp, SymptomName, SymptomOmim}


class HpAnnotations {
  def symptomNameEqSymptomOmim(symptomName: SymptomName): Set[SymptomOmim] = { Set.empty }
  def symptomOmimEqSymptomName(symptomOmim: SymptomOmim): Set[SymptomName] = { Set.empty }
  def symptomHpCausedBySymptomName(symptomHp: SymptomHp): Set[SymptomName] = { Set.empty }
  def symptomHpCausedBySymptomOmim(symptomHp: SymptomHp): Set[SymptomOmim] = { Set.empty }
}
