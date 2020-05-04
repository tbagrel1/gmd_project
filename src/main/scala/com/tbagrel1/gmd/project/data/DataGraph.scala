package com.tbagrel1.gmd.project.data

import com.tbagrel1.gmd.project.data.sources.{Br08303, ChemicalSources, Drugbank, HpAnnotations, HpOntology, Meddra, Omim, OmimOntology, Orphadata}

import scala.collection.mutable

class DataGraph {
  val br08303: Br08303 = null
  val chemicalSources: ChemicalSources = null
  val drugbank: Drugbank = null
  val hpAnnotations: HpAnnotations = null
  val hpOntology: HpOntology = null
  val meddra: Meddra = null
  val omim: Omim = null
  val omimOntology: OmimOntology = null
  val orphadata: Orphadata = null

  val drugNameNodes: mutable.HashMap[String, DrugActivation] = null
  val drugAtcNodes: mutable.HashMap[String, DrugActivation] = null
  val drugCompoundNodes: mutable.HashMap[String, DrugActivation] = null
  val symptomNameNodes: mutable.HashMap[String, SymptomActivation] = null
  val symptomOrphaNodes: mutable.HashMap[String, SymptomActivation] = null
  val symptomHpNodes: mutable.HashMap[String, SymptomActivation] = null
  val symptomCuiNodes: mutable.HashMap[String, SymptomActivation] = null
  val symptomOmimNodes: mutable.HashMap[String, SymptomActivation] = null

  val drugAttributesQueue: mutable.Queue[DrugAttribute] = mutable.Queue.empty
  val symptomAttributesQueue: mutable.Queue[SymptomAttribute] = mutable.Queue.empty

  def dispatchSymptomEqSynonymAt(attribute: SymptomAttribute): Unit = {

  }
}
