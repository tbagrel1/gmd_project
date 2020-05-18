package com.tbagrel1.gmd.project.sources

import com.tbagrel1.gmd.project.SymptomName

import scala.collection.mutable

class Orphadata {
  def symptomNameCausedBySymptomName(symptomName: SymptomName): mutable.Set[SymptomName] = { mutable.Set.empty }
}
