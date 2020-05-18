package com.tbagrel1.gmd.project.sources

import com.tbagrel1.gmd.project.{SymptomHp, SymptomName}

import scala.collection.mutable

class HpOntology {
  def symptomNameEqSymptomHp(symptomName: SymptomName): mutable.Set[SymptomHp] = { mutable.Set.empty }
  def symptomHpEqSymptomName(symptomHp: SymptomHp): mutable.Set[SymptomName] = { mutable.Set.empty }
  def symptomNameSynonymSymptomName(symptomName: SymptomName): mutable.Set[SymptomName] = { mutable.Set.empty }
  def symptomHpSynonymSymptomName(symptomHp: SymptomHp): mutable.Set[SymptomName] = { mutable.Set.empty }
}
