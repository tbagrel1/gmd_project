package com.tbagrel1.gmd.project.data.sources

import com.tbagrel1.gmd.project.data.{DrugAtc, DrugCompound}

class ChemicalSources {
  def drugAtcEqDrugCompound(drugAtc: DrugAtc): Set[DrugCompound] = { Set.empty }
  def drugCompoundEqDrugAtc(drugCompound: DrugCompound): Set[DrugAtc] = { Set.empty }
}
