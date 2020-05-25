package com.tbagrel1.gmd.project

import com.tbagrel1.gmd.project.sources.SourceCatalog

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class DataGraph(val sources: SourceCatalog, val initialSymptoms: mutable.HashMap[String, (String, Double)]) {
  // TODO: problème omim.txt sur le name
  // TODO: name vide

  val initialActivationAvg: Double = initialSymptoms.map(_._2._2).sum / initialSymptoms.size
  val cutOff: Double = initialActivationAvg * Parameters.CUT_OFF_THRESHOLD

  val symptomNameNodes: mutable.HashMap[String, SymptomActivation] = symptomNodesFromInitial("name")
  val symptomHpNodes: mutable.HashMap[String, SymptomActivation] = symptomNodesFromInitial("hp")
  val symptomCuiNodes: mutable.HashMap[String, SymptomActivation] = symptomNodesFromInitial("cui")
  val symptomOmimNodes: mutable.HashMap[String, SymptomActivation] = symptomNodesFromInitial("omim")

  val drugNameNodes: mutable.HashMap[String, DrugActivation] = mutable.HashMap.empty
  val drugAtcNodes: mutable.HashMap[String, DrugActivation] = mutable.HashMap.empty
  val drugCompoundNodes: mutable.HashMap[String, DrugActivation] = mutable.HashMap.empty

  val drugAttributesQueue: mutable.Queue[DrugAttribute] = mutable.Queue.empty
  val symptomAttributesQueue: mutable.Queue[SymptomAttribute] = mutable.Queue.empty

  def symptomDiameter: Int = {
    symptomNameNodes.size + symptomHpNodes.size + symptomCuiNodes.size + symptomOmimNodes.size
  }

  def drugDiameter: Int = {
    drugNameNodes.size + drugAtcNodes.size + drugCompoundNodes.size
  }

  def reportDiameter(): Unit = {
    val sD = symptomDiameter
    val dD = drugDiameter
    println(s"    % Diameter of symptom space: ${sD} | Diameter of drug space: ${dD} | Total diameter: ${sD + dD}")
  }

  def symptomNodesFromInitial(requestedAttributeType: String): mutable.HashMap[String, SymptomActivation] = {
    for ((value, (attributeType, weight)) <- initialSymptoms if attributeType == requestedAttributeType)
      yield { (value, SymptomActivation(
        for (i <- ArrayBuffer.from(0 until Parameters.CAUSE_LEVELS)) yield { if (i == 0) { weight } else { 0.0 } },
        for (i <- ArrayBuffer.from(0 until Parameters.CAUSE_LEVELS)) yield { if (i == 0) { SymptomActivationOrigin.UserInput } else { SymptomActivationOrigin.NoOrigin } }
      )) }
  }

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

  def symptomAttributesFromMaps: Seq[SymptomAttribute] = {
    val result: mutable.ArrayBuilder[SymptomAttribute] = mutable.ArrayBuilder.make
    result.addAll(symptomNameNodes.keys.map(SymptomName))
    result.addAll(symptomCuiNodes.keys.map(SymptomCui))
    result.addAll(symptomHpNodes.keys.map(SymptomHp))
    result.addAll(symptomOmimNodes.keys.map(SymptomOmim))
    result.result()
  }

  def drugAttributesFromMaps: Seq[DrugAttribute] = {
    val result: mutable.ArrayBuilder[DrugAttribute] = mutable.ArrayBuilder.make
    result.addAll(drugNameNodes.keys.map(DrugName))
    result.addAll(drugAtcNodes.keys.map(DrugAtc))
    result.addAll(drugCompoundNodes.keys.map(DrugCompound))
    result.result()
  }

  def sendLight(): Unit = {
    println("# Step 1: Compute synonyms of the input symptoms")
    dispatchSymptomEqSynonymAt(symptomAttributesFromMaps)
    println(symptomNameNodes.keys) // TODO: remove
    println("# Step 2: Look for causes and dispatch synonyms")
    for (nextLevel <- 1 until Parameters.CAUSE_LEVELS) {
      println(nextLevel) // TODO: remove
      dispatchSymptomEqSynonymAt(dispatchCausedByAt(nextLevel, symptomAttributesFromMaps))
    }
    println("# Step 3: Look for side effect sources")
    val symptoms = symptomAttributesFromMaps
    dispatchIsSideEffectAt(symptoms)
    println("# Step 4: Look for cures")
    dispatchCuredByAt(symptoms)
    println("# Step 5: Look for synonyms of the activated drugs")
    dispatchDrugEqSynonymAt(drugAttributesFromMaps)
    println("# Done!")
  }

  def cures: Seq[(String, Double, CureActivationOrigin)] = {
    val theCures = for ((name, act) <- drugNameNodes if act.cureActivation > 0) yield { (name, act.cureActivation, act.cureOrigin) }
    theCures.toSeq.sortWith { case ((_, act1, _), (_, act2, _)) => act1 > act2 }
  }

  def sideEffectSources: Seq[(String, Double, SideEffectActivationOrigin)] = {
    val theSideEffectSources = for ((name, act) <- drugNameNodes if act.sideEffectActivation > 0) yield { (name, act.sideEffectActivation, act.sideEffectOrigin) }
    theSideEffectSources.toSeq.sortWith { case ((_, act1, _), (_, act2, _)) => act1 > act2 }
  }

  def causes: Seq[(String, Double, SymptomActivationOrigin)] = {
    val theCauses = (for (i <- 1 until Parameters.CAUSE_LEVELS) yield {
      (for ((name, act) <- symptomNameNodes if act.levelActivation(i) > 0) yield {
        (name, act.levelActivation(i), act.levelOrigin(i))
      }).toSeq
    }).flatten
    theCauses.sortWith { case ((_, act1, _), (_, act2, _)) => act1 > act2 }
  }

  def dispatchCausedByAt(nextLevel: Int, attributes: Seq[SymptomAttribute]): Seq[SymptomAttribute] = {
    reportDiameter()
    val higherLevelAttributes: mutable.HashSet[SymptomAttribute] = mutable.HashSet.empty
    for (attribute <- attributes) {
      val (causes, activation) = attribute match {
        case name@SymptomName(_) => (
          List((sources.omim.symptomNameCausedBySymptomOmim(name), "Omim"),
               (sources.orphadata.symptomNameCausedBySymptomName(name), "Orphadata"))
          ,
          getSymptomAttributeActivation(attribute).get
        )
        case cui@SymptomCui(_) => (
          List()
          ,
          getSymptomAttributeActivation(attribute).get
        )
        case hp@SymptomHp(_) => (
          List((sources.hpAnnotations.symptomHpCausedBySymptomName(hp), "HpAnnotations"),
               (sources.hpAnnotations.symptomHpCausedBySymptomOmim(hp), "HpAnnotations"))
          ,
          getSymptomAttributeActivation(attribute).get
        )
        case omim@SymptomOmim(_) => (
          List()
          ,
          getSymptomAttributeActivation(attribute).get
        )
      }
      for ((causeSet, source) <- causes) {
        for (causeAttribute <- causeSet) {
          higherLevelAttributes.add(causeAttribute)
          getSymptomAttributeActivation(causeAttribute) match {
            case None => {
              val map = getSymptomAttributeMap(causeAttribute)
              val theAct = SymptomActivation(
                for ((act, i) <- activation.levelActivation.zipWithIndex) yield { if (i == nextLevel) { activation.levelActivation.slice(0, nextLevel).max * Parameters.HIGHER_SYMPTOM_TRANSMISSION_COEFF } else { 0.0 } },
                for ((act, i) <- activation.levelActivation.zipWithIndex) yield { if (i == nextLevel) { SymptomActivationOrigin.HigherLevel(ArrayBuffer((attribute, source))) } else { SymptomActivationOrigin.NoOrigin } }
                )
              map.put(
                causeAttribute.value, theAct
                )
              println(causeAttribute) // TODO: remove
              println(theAct) // TODO: remove
            }
            case Some(causeActivation) => {
              causeActivation.levelOrigin(nextLevel) match {
                case SymptomActivationOrigin.NoOrigin => {
                  causeActivation.levelOrigin(nextLevel) = SymptomActivationOrigin.HigherLevel(ArrayBuffer((attribute, source)))
                  causeActivation.levelActivation(nextLevel) = activation.levelActivation.slice(0, nextLevel).max * Parameters.HIGHER_SYMPTOM_TRANSMISSION_COEFF
                }
                case SymptomActivationOrigin.HigherLevel(attributesSources) => {
                  attributesSources.addOne((attribute, source))
                  causeActivation.levelActivation(nextLevel) += activation.levelActivation.slice(0, nextLevel).max * Parameters.HIGHER_SYMPTOM_TRANSMISSION_COEFF
                }
                case _ => throw new Exception("Higher level symptom has an activation origin different from NoOrigin or HigherLevel")
              }
              println(causeAttribute) // TODO: remove
              println(causeActivation) // TODO: remove
            }
          }
        }
      }
    }
    higherLevelAttributes.toSeq
  }

  def dispatchIsSideEffectAt(attributes: Seq[SymptomAttribute]): Unit = {
    for (attribute <- attributes) {
      reportDiameter()
      val (causes, activation) = attribute match {
        case name@SymptomName(_) => (
          List((sources.drugbank.symptomNameIsSideEffectDrugName(name), "Drugbank"),
               (sources.meddra.symptomNameIsSideEffectDrugCompound(name), "Meddra"))
          ,
          getSymptomAttributeActivation(attribute).get
        )
        case cui@SymptomCui(_) => (
          List((sources.meddra.symptomCuiIsSideEffectDrugCompound(cui), "Meddra"))
          ,
          getSymptomAttributeActivation(attribute).get
        )
        case hp@SymptomHp(_) => (
          List()
          ,
          getSymptomAttributeActivation(attribute).get
        )
        case omim@SymptomOmim(_) => (
          List()
          ,
          getSymptomAttributeActivation(attribute).get
        )
      }
      for ((causeSet, source) <- causes) {
        for (causeAttribute <- causeSet) {
          getDrugAttributeActivation(causeAttribute) match {
            case None => {
              val map = getDrugAttributeMap(causeAttribute)
              val theAct = DrugActivation(0.0, CureActivationOrigin.NoOrigin, activation.levelActivation.max, SideEffectActivationOrigin.ResponsibleFor(ArrayBuffer((attribute, source))))
              map.put(
                causeAttribute.value, theAct)
              println(causeAttribute) // TODO: remove
              println(theAct) // TODO: remove
            }
            case Some(causeActivation) => {

              causeActivation.sideEffectOrigin match {
                case SideEffectActivationOrigin.NoOrigin => {
                  causeActivation.sideEffectOrigin = SideEffectActivationOrigin.ResponsibleFor(ArrayBuffer((attribute, source)))
                  causeActivation.sideEffectActivation = activation.levelActivation.max
                }
                case SideEffectActivationOrigin.ResponsibleFor(attributesSources) => {
                  attributesSources.addOne((attribute, source))
                  causeActivation.sideEffectActivation += activation.levelActivation.max
                }
                case _ => throw new Exception("Side effect drug has a side effect activation origin different from NoOrigin or ResponsibleFor")
              }
              println(causeAttribute) // TODO: remove
              println(causeActivation) // TODO: remove
            }
          }
        }
      }
    }
  }

  def dispatchCuredByAt(attributes: Seq[SymptomAttribute]): Unit = {
    for (attribute <- attributes) {
      reportDiameter()
      val (cures, activation) = attribute match {
        case name@SymptomName(_) => (
          List((sources.drugbank.symptomNameCuredByDrugName(name), "Drugbank"),
               (sources.meddra.symptomNameCuredByDrugCompound(name), "Meddra"))
          ,
          getSymptomAttributeActivation(attribute).get
        )
        case cui@SymptomCui(_) => (
          List((sources.meddra.symptomCuiCuredByDrugCompound(cui), "Meddra"))
          ,
          getSymptomAttributeActivation(attribute).get
        )
        case hp@SymptomHp(_) => (
          List()
          ,
          getSymptomAttributeActivation(attribute).get
        )
        case omim@SymptomOmim(_) => (
          List()
          ,
          getSymptomAttributeActivation(attribute).get
        )
      }
      for ((cureSet, source) <- cures) {
        for (cureAttribute <- cureSet) {
          getDrugAttributeActivation(cureAttribute) match {
            case None => {
              val map = getDrugAttributeMap(cureAttribute)
              val theAct = DrugActivation(activation.levelActivation.max, CureActivationOrigin.Cures(ArrayBuffer((attribute, source))), 0.0, SideEffectActivationOrigin.NoOrigin)
              map.put(
                cureAttribute.value, theAct
                )
              println(cureAttribute) // TODO: Remove
              println(theAct)  // TODO: Remove
            }
            case Some(cureActivation) => {
              cureActivation.cureOrigin match {
                case CureActivationOrigin.NoOrigin => {
                  cureActivation.cureOrigin = CureActivationOrigin.Cures(ArrayBuffer((attribute, source)))
                  cureActivation.cureActivation = activation.levelActivation.max
                }
                case CureActivationOrigin.Cures(attributesSources) => {
                  attributesSources.addOne((attribute, source))
                  cureActivation.cureActivation += activation.levelActivation.max
                }
                case _ => throw new Exception("Cure drug has a cure activation origin different from NoOrigin or Cures")
              }
              println(cureAttribute) // TODO: Remove
              println(cureActivation)  // TODO: Remove
            }
          }
        }
      }
    }
  }

  def dispatchDrugEqSynonymAt(attributes: Seq[DrugAttribute]): Unit = {
    drugAttributesQueue.enqueueAll(attributes)
    while (drugAttributesQueue.nonEmpty) {
      val attribute = drugAttributesQueue.dequeue()
      dispatchDrugEqSynonymAtOneStep(attribute)
    }
  }

  def dispatchSymptomEqSynonymAt(attributes: Seq[SymptomAttribute]): Unit = {
    symptomAttributesQueue.enqueueAll(attributes)
    while (symptomAttributesQueue.nonEmpty) {
      val attribute = symptomAttributesQueue.dequeue()
      dispatchSymptomEqSynonymAtOneStep(attribute)
    }
  }

  def dispatchDrugEqSynonymAtOneStep(attribute: DrugAttribute): Unit = {
    reportDiameter()
    val (eqs, synonyms, activation) = attribute match {
      case name@DrugName(_) => (
        List((sources.drugbank.drugNameEqDrugAtc(name), "Drugbank"),
             (sources.br08303.drugNameEqDrugAtc(name), "Br08303"))
        ,
        List((sources.drugbank.drugNameSynonymDrugName(name), "Drugbank"))
        ,
        getDrugAttributeActivation(attribute).get
      )
      case atc@DrugAtc(_) => (
        List((sources.drugbank.drugAtcEqDrugName(atc), "Drugbank"),
             (sources.br08303.drugAtcEqDrugName(atc), "Br08303"),
             (sources.chemicalSources.drugAtcEqDrugCompound(atc), "ChemicalSources"))
        ,
        List()
        ,
        getDrugAttributeActivation(attribute).get
      )
      case compound@DrugCompound(_) => (
        List((sources.chemicalSources.drugCompoundEqDrugAtc(compound), "ChemicalSources"))
        ,
        List()
        ,
        getDrugAttributeActivation(attribute).get
      )
    }
    for ((eqsSynonyms, transmissionCoeff, cureOriginApp, sideEffectOriginApp) <- List((eqs, Parameters.EQUAL_TRANSMISSION_COEFF, CureActivationOrigin.Equals, SideEffectActivationOrigin.Equals), (synonyms, Parameters.SYNONYM_TRANSMISSION_COEFF, CureActivationOrigin.IsSynonym, SideEffectActivationOrigin.IsSynonym))) {
      for ((eqSynonymSet, source) <- eqsSynonyms) {
        for (eqSynonymAttribute <- eqSynonymSet) {
          getDrugAttributeActivation(eqSynonymAttribute) match {
            case None => {
              val map = getDrugAttributeMap(eqSynonymAttribute)
              val theAct = DrugActivation(
                activation.cureActivation * transmissionCoeff,
                cureOriginApp(attribute, source),
                activation.sideEffectActivation * transmissionCoeff,
                sideEffectOriginApp(attribute, source)
                )
              map.put(
                eqSynonymAttribute.value, theAct
              )
              if (activation.sideEffectActivation * transmissionCoeff > cutOff || activation.sideEffectActivation * transmissionCoeff > cutOff) {
                drugAttributesQueue.enqueue(eqSynonymAttribute)
              }
              println(eqSynonymAttribute) // TODO: remove
              println(theAct)  // TODO: remove
            }
            case Some(eqSynonymActivation) => {
              var updated = false
              if (activation.cureActivation * transmissionCoeff > eqSynonymActivation.cureActivation) {
                updated = true
                eqSynonymActivation.cureActivation = activation.cureActivation * transmissionCoeff
                eqSynonymActivation.cureOrigin = cureOriginApp(attribute, source)
              }
              if (activation.sideEffectActivation * transmissionCoeff > eqSynonymActivation.sideEffectActivation) {
                updated = true
                eqSynonymActivation.sideEffectActivation = activation.sideEffectActivation * transmissionCoeff
                eqSynonymActivation.sideEffectOrigin = sideEffectOriginApp(attribute, source)
              }
              if (updated && (activation.cureActivation * transmissionCoeff > cutOff || activation.sideEffectActivation > cutOff)) {
                drugAttributesQueue.enqueue(eqSynonymAttribute)
              }
              println(eqSynonymAttribute) // TODO: remove
              println(eqSynonymActivation)  // TODO: remove
            }
          }
        }
      }
    }
  }

  def dispatchSymptomEqSynonymAtOneStep(attribute: SymptomAttribute): Unit = {
    reportDiameter()
    val (eqs, synonyms, activation) = attribute match {
      case name@SymptomName(_) => (
        List((sources.meddra.symptomNameEqSymptomCui(name).asInstanceOf[mutable.Set[SymptomAttribute]], "Meddra"),
          (sources.omimOntology.symptomNameEqSymptomCui(name).asInstanceOf[mutable.Set[SymptomAttribute]], "OmimOntology"),
          (sources.omimOntology.symptomNameEqSymptomOmim(name).asInstanceOf[mutable.Set[SymptomAttribute]], "OmimOntology"),
          (sources.hpOntology.symptomNameEqSymptomHp(name).asInstanceOf[mutable.Set[SymptomAttribute]], "HpOntology"),
          (sources.hpAnnotations.symptomNameEqSymptomOmim(name).asInstanceOf[mutable.Set[SymptomAttribute]], "HpAnnotations"),
          (sources.omim.symptomNameEqSymptomOmim(name).asInstanceOf[mutable.Set[SymptomAttribute]], "Omim")
          )
        ,
        List(
          (sources.omimOntology.symptomNameSynonymSymptomName(name).asInstanceOf[mutable.Set[SymptomAttribute]], "OmimOntology"),
          (sources.hpOntology.symptomNameSynonymSymptomName(name).asInstanceOf[mutable.Set[SymptomAttribute]], "HpOntology")
          )
        ,
        getSymptomAttributeActivation(attribute).get
      )
      case cui@SymptomCui(_) => (
        List(
          (sources.meddra.symptomCuiEqSymptomName(cui).asInstanceOf[mutable.Set[SymptomAttribute]], "Meddra"),
          (sources.omimOntology.symptomCuiEqSymptomName(cui).asInstanceOf[mutable.Set[SymptomAttribute]], "OmimOntology")
          )
        ,
        List()
        ,
        getSymptomAttributeActivation(attribute).get
      )
      case hp@SymptomHp(_) => (
        List((sources.hpOntology.symptomHpEqSymptomName(hp).asInstanceOf[mutable.Set[SymptomAttribute]], "HpOntology"))
        ,
        List()
        ,
        getSymptomAttributeActivation(attribute).get
      )
      case omim@SymptomOmim(_) => (
        List(
          (sources.omimOntology.symptomOmimEqSymptomName(omim).asInstanceOf[mutable.Set[SymptomAttribute]], "OmimOntology"),
          (sources.hpAnnotations.symptomOmimEqSymptomName(omim).asInstanceOf[mutable.Set[SymptomAttribute]], "HpAnnotations"),
          (sources.omim.symptomOmimEqSymptomName(omim).asInstanceOf[mutable.Set[SymptomAttribute]], "Omim")
          )
        ,
        List()
        ,
        getSymptomAttributeActivation(attribute).get
      )
    }
    for ((eqsSynonyms, transmissionCoeff, originApp) <- List((eqs, Parameters.EQUAL_TRANSMISSION_COEFF, SymptomActivationOrigin.Equals), (synonyms, Parameters.SYNONYM_TRANSMISSION_COEFF, SymptomActivationOrigin.IsSynonym))) {
      for ((eqSynonymSet, source) <- eqsSynonyms) {
        for (eqSynonymAttribute <- eqSynonymSet) {
          getSymptomAttributeActivation(eqSynonymAttribute) match {
            case None => {
              val map = getSymptomAttributeMap(eqSynonymAttribute)
              val theAct = SymptomActivation(
                for (act <- activation.levelActivation) yield { act * transmissionCoeff },
                for (_ <- activation.levelActivation) yield { originApp(attribute, source) }
                )
              map.put(
                eqSynonymAttribute.value,
                theAct
                )
              if (theAct.levelActivation.max > cutOff) {
                symptomAttributesQueue.enqueue(eqSynonymAttribute)
              }
              println(eqSynonymAttribute) // TODO: remove
              println(theAct)  // TODO: remove
            }
            case Some(eqSynonymActivation) => {
              var updated = false
              for (i <- activation.levelActivation.indices) {
                if (activation.levelActivation(i) * transmissionCoeff > eqSynonymActivation.levelActivation(i)) {
                  updated = true
                  eqSynonymActivation.levelActivation(i) = activation.levelActivation(i) * transmissionCoeff
                  eqSynonymActivation.levelOrigin(i) = originApp(attribute, source)
                }
              }
              if (updated && eqSynonymActivation.levelActivation.max > cutOff) {
                symptomAttributesQueue.enqueue(eqSynonymAttribute)
              }
              println(eqSynonymAttribute) // TODO: remove
              println(eqSynonymActivation)  // TODO: remove
            }
          }
        }
      }
    }
  }
}