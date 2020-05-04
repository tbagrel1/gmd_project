package com.tbagrel1.gmd.project.data.sources

import com.tbagrel1.gmd.project.data.{DrugAtc, DrugName}

class Br08303 {
  def drugNameEqDrugAtc(drugName: DrugName): Set[DrugAtc] = { Set.empty }
  def drugAtcEqDrugName(drugAtc: DrugAtc): Set[DrugName] = { Set.empty }
}
