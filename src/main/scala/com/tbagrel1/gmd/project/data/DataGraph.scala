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
  val symptomHpNodes: mutable.HashMap[String, SymptomActivation] = null
  val symptomCuiNodes: mutable.HashMap[String, SymptomActivation] = null
  val symptomOmimNodes: mutable.HashMap[String, SymptomActivation] = null

  val drugAttributesQueue: mutable.Queue[DrugAttribute] = mutable.Queue.empty
  val symptomAttributesQueue: mutable.Queue[SymptomAttribute] = mutable.Queue.empty

  def getSymptomAttributeMap(attribute: SymptomAttribute): mutable.HashMap[String, SymptomActivation] = {
    attribute match {
      case SymptomName(_) => symptomNameNodes
      case SymptomCui(_) => symptomCuiNodes
      case SymptomHp(_) => symptomHpNodes
      case SymptomOmim(_) => symptomOmimNodes
    }
  }

  def getDrugAttributeMap(attribute: DrugAttribute): mutable.HashMap[String, DrugActivation] = {
    attribute match {
      case DrugName(_) => drugNameNodes
      case DrugAtc(_) => drugAtcNodes
      case DrugCompound(_) => drugCompoundNodes
    }
  }

  def getSymptomAttributeActivation(attribute: SymptomAttribute): Option[SymptomActivation] = {
    getSymptomAttributeMap(attribute).get(attribute.value)
  }

  def getDrugAttributeActivation(attribute: DrugAttribute): Option[DrugActivation] = {
    getDrugAttributeMap(attribute).get(attribute.value)
  }

  def dispatchCausedByAt(nextLevel: Int, attribute: SymptomAttribute): Unit = {
    // TODO -- no queue needed, one pass
  }

  def dispatchIsSideEffectAt(attribute: SymptomAttribute): Unit = {
    // TODO -- no queue needed, one pass
  }

  def dispatchCuredByAt(attribute: SymptomAttribute): Unit = {
    // TODO -- no queue needed, one pass
  }

  def dispatchDrugEqSynonymAll(): Unit = {
    while (drugAttributesQueue.nonEmpty) {
      val attribute = drugAttributesQueue.dequeue()
      dispatchDrugEqSynonymAt(attribute)
    }
  }

  def dispatchSymptomEqSynonymAll(): Unit = {
    while (symptomAttributesQueue.nonEmpty) {
      val attribute = symptomAttributesQueue.dequeue()
      dispatchSymptomEqSynonymAt(attribute)
    }
  }

  def dispatchDrugEqSynonymAt(attribute: DrugAttribute): Unit = {
    val (eqs, synonyms, activation) = attribute match {
      case name@DrugName(_) => (
        List((drugbank.drugNameEqDrugAtc(name), "Drugbank"),
             (br08303.drugNameEqDrugAtc(name), "Br08303"))
        ,
        List((drugbank.drugNameSynonymDrugName(name), "Drugbank"))
        ,
        getDrugAttributeActivation(attribute).get
      )
      case atc@DrugAtc(_) => (
        List((drugbank.drugAtcEqDrugName(atc), "Drugbank"),
             (br08303.drugAtcEqDrugName(atc), "Br08303"),
             (chemicalSources.drugAtcEqDrugCompound(atc), "ChemicalSources"))
        ,
        List((drugbank.drugAtcSynonymDrugName(atc), "Drugbank"))
        ,
        getDrugAttributeActivation(attribute).get
      )
      case compound@DrugCompound(_) => (
        List((chemicalSources.drugCompoundEqDrugAtc(compound), "ChemicalSources"))
        ,
        List()
        ,
        getDrugAttributeActivation(attribute).get
      )
    }
    for ((eqsSynonyms, transmissionCoeff, cureOriginApp, sideEffectOriginApp) <- List((eqs, Activation.EQUAL_TRANSMISSION_COEFF, CureActivationOrigin.Equals, SideEffectActivationOrigin.Equals), (synonyms, Activation.SYNONYM_TRANSMISSION_COEFF, CureActivationOrigin.IsSynonym, SideEffectActivationOrigin.IsSynonym))) {
      for ((eqSynonymSet, source) <- eqsSynonyms) {
        for (eqSynonymAttribute <- eqSynonymSet) {
          getDrugAttributeActivation(eqSynonymAttribute) match {
            case None => {
              val map = getDrugAttributeMap(eqSynonymAttribute)
              map.put(
                eqSynonymAttribute.value, DrugActivation(
                  activation.cureActivation * transmissionCoeff,
                  cureOriginApp(attribute, source),
                  activation.sideEffectActivation * transmissionCoeff,
                  sideEffectOriginApp(attribute, source)
                )
              )
              drugAttributesQueue.enqueue(eqSynonymAttribute)
            }
            case Some(eqActivation) => {
              var updated = false
              if (activation.cureActivation * transmissionCoeff > eqActivation.cureActivation) {
                updated = true
                eqActivation.cureActivation = activation.cureActivation * transmissionCoeff
                eqActivation.cureOrigin = cureOriginApp(attribute, source)
              }
              if (activation.sideEffectActivation * transmissionCoeff > eqActivation.sideEffectActivation) {
                updated = true
                eqActivation.sideEffectActivation = activation.sideEffectActivation * transmissionCoeff
                eqActivation.sideEffectOrigin = sideEffectOriginApp(attribute, source)
              }
              if (updated) {
                drugAttributesQueue.enqueue(eqSynonymAttribute)
              }
            }
          }
        }
      }
    }
  }

  def dispatchSymptomEqSynonymAt(attribute: SymptomAttribute): Unit = {
    val (eqs, synonyms, activation) = attribute match {
      case name@SymptomName(_) => (
        List((meddra.symptomNameEqSymptomCui(name).asInstanceOf[Set[SymptomAttribute]], "Meddra"),
          (omimOntology.symptomNameEqSymptomCui(name).asInstanceOf[Set[SymptomAttribute]], "OmimOntology"),
          (omimOntology.symptomNameEqSymptomOmim(name).asInstanceOf[Set[SymptomAttribute]], "OmimOntology"),
          (hpOntology.symptomNameEqSymptomHp(name).asInstanceOf[Set[SymptomAttribute]], "HpOntology"),
          (hpAnnotations.symptomNameEqSymptomOmim(name).asInstanceOf[Set[SymptomAttribute]], "HpAnnotations")
          )
        ,
        List(
          (omimOntology.symptomNameSynonymSymptomName(name).asInstanceOf[Set[SymptomAttribute]], "OmimOntology"),
          (hpOntology.symptomNameSynonymSymptomName(name).asInstanceOf[Set[SymptomAttribute]], "HpOntology")
          )
        ,
        getSymptomAttributeActivation(attribute).get
      )
      case cui@SymptomCui(_) => (
        List(
          (meddra.symptomCuiEqSymptomName(cui).asInstanceOf[Set[SymptomAttribute]], "Meddra"),
          (omimOntology.symptomCuiEqSymptomName(cui).asInstanceOf[Set[SymptomAttribute]], "OmimOntology"),
          (omimOntology.symptomCuiEqSymptomOmim(cui).asInstanceOf[Set[SymptomAttribute]], "OmimOntology")
          )
        ,
        List((omimOntology.symptomCuiSynonymSymptomName(cui).asInstanceOf[Set[SymptomAttribute]], "OmimOntology"))
        ,
        getSymptomAttributeActivation(attribute).get
      )
      case hp@SymptomHp(_) => (
        List((hpOntology.symptomHpEqSymptomName(hp).asInstanceOf[Set[SymptomAttribute]], "HpOntology"))
        ,
        List((hpOntology.symptomHpSynonymSymptomName(hp).asInstanceOf[Set[SymptomAttribute]], "HpOntology"))
        ,
        getSymptomAttributeActivation(attribute).get
      )
      case omim@SymptomOmim(_) => (
        List(
          (omimOntology.symptomOmimEqSymptomName(omim).asInstanceOf[Set[SymptomAttribute]], "OmimOntology"),
          (omimOntology.symptomOmimEqSymptomCui(omim).asInstanceOf[Set[SymptomAttribute]], "OmimOntology"),
          (hpAnnotations.symptomOmimEqSymptomName(omim).asInstanceOf[Set[SymptomAttribute]], "HpAnnotations")
          )
        ,
        List((omimOntology.symptomOmimSynonymSymptomName(omim).asInstanceOf[Set[SymptomAttribute]], "OmimOntology"))
        ,
        getSymptomAttributeActivation(attribute).get
      )
    }
    for ((eqsSynonyms, transmissionCoeff, originApp) <- List((eqs, Activation.EQUAL_TRANSMISSION_COEFF, SymptomActivationOrigin.Equals), (synonyms, Activation.SYNONYM_TRANSMISSION_COEFF, SymptomActivationOrigin.IsSynonym))) {
      for ((eqSynonymSet, source) <- eqsSynonyms) {
        for (eqSynonymAttribute <- eqSynonymSet) {
          getSymptomAttributeActivation(eqSynonymAttribute) match {
            case None => {
              val map = getSymptomAttributeMap(eqSynonymAttribute)
              map.put(
                eqSynonymAttribute.value, SymptomActivation(
                  for (act <- activation.levelActivation) yield { act * transmissionCoeff },
                  for (_ <- activation.levelActivation) yield { originApp(attribute, source) }
                  )
                )
              symptomAttributesQueue.enqueue(eqSynonymAttribute)
            }
            case Some(eqActivation) => {
              var updated = false
              for (i <- 0 until activation.levelActivation.length) {
                if (activation.levelActivation(i) * transmissionCoeff > eqActivation.levelActivation(i)) {
                  updated = true
                  eqActivation.levelActivation(i) = activation.levelActivation(i) * transmissionCoeff
                  eqActivation.levelOrigin(i) = originApp(attribute, source)
                }
              }
              if (updated) {
                symptomAttributesQueue.enqueue(eqSynonymAttribute)
              }
            }
          }
        }
      }
    }
  }
}
