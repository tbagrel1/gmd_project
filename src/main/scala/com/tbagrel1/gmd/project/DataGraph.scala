package com.tbagrel1.gmd.project

import java.io.{File, PrintWriter}

import com.tbagrel1.gmd.project.sources.SourceCatalog
import scalax.collection.edge.LkDiEdge
import scalax.collection.io.dot._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

class DataGraph(val sources: SourceCatalog, val initialSymptoms: mutable.Set[(String, String, Double)]) {

  val initialAttributesActivations: mutable.Set[(SymptomAttribute, SymptomActivation)] = {
    (for ((value, attributeType, weight) <- initialSymptoms) yield {
      val activation = SymptomActivation(
        for (i <- ArrayBuffer.from(0 until Parameters.CAUSE_LEVELS)) yield { if (i == 0) { weight } else { 0.0 } },
        for (i <- ArrayBuffer.from(0 until Parameters.CAUSE_LEVELS)) yield { if (i == 0) { SymptomActivationOrigin.UserInput } else { SymptomActivationOrigin.NoOrigin } })
        attributeType match {
        case "name" => mutable.Set((SymptomName(Utils.normalize(value)), activation))
        case "cui" => mutable.Set((SymptomCui(Utils.normalize(value)), activation))
        case "hp" => mutable.Set((SymptomHp(Utils.normalize(value)), activation))
        case "omim" => mutable.Set((SymptomOmim(Utils.normalize(value)), activation))
        case "nameRegex" => getSymptomNamesMatching(value.r).map(symptomName => (SymptomName(Utils.normalize(symptomName)), activation))
      }
    }).flatten
  }

  val initialActivationAvg: Double = initialSymptoms.map(_._3).sum / initialSymptoms.size
  val cutOff: Double = initialActivationAvg * Parameters.CUT_OFF_THRESHOLD

  val symptomNameNodes: mutable.HashMap[String, SymptomActivation] = symptomNodes[SymptomName]
  val symptomHpNodes: mutable.HashMap[String, SymptomActivation] = symptomNodes[SymptomHp]
  val symptomCuiNodes: mutable.HashMap[String, SymptomActivation] = symptomNodes[SymptomCui]
  val symptomOmimNodes: mutable.HashMap[String, SymptomActivation] = symptomNodes[SymptomOmim]

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

  def getSymptomNamesMatching(pattern: Regex): mutable.Set[String] = {
    val resultSet = mutable.HashSet.empty[String]
    val scalaPattern = s"(?i)${pattern}".r
    val allSymptomNames = sources.getAllSymptomNames
    for (symptomName <- allSymptomNames)
      scalaPattern.findAllMatchIn(symptomName)
    resultSet  // TODO: implement
  }

  def symptomNodes[S <: SymptomAttribute]: mutable.HashMap[String, SymptomActivation] = {
    mutable.HashMap.from(initialAttributesActivations
      .filter { case (attribute, _) => attribute.isInstanceOf[S] }
      .map { case (attribute, activation) => (attribute.value, activation) }
    )
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

  def createDotFile(limit: Int): Unit = {
    sealed trait Reason
    case object Cure extends Reason
    case object SideEffectCause extends Reason
    case class Cause(nextLevel: Int) extends Reason

    val g = scalax.collection.mutable.Graph.empty[Attribute, LkDiEdge]

    val attributeQueue = mutable.Queue.empty[(Attribute, Reason)]

    val theCures = cures
    val theSideEffectSources = sideEffectSources
    val theCauses = causes

    for ((attribute, _) <- initialAttributesActivations) {
      g += LkDiEdge(SymptomName("User input"), attribute)("initial_symptom/user")
    }

    attributeQueue.enqueueAll(theCures.slice(0, limit min theCures.length).map(t => (DrugName(t._1), Cure)))
    attributeQueue.enqueueAll(theSideEffectSources.slice(0, limit min theSideEffectSources.length).map(t => (DrugName(t._1), SideEffectCause)))
    attributeQueue.enqueueAll(theCauses.slice(0, limit min theCauses.length).map(t => (SymptomName(t._1), Cause(t._3))))

    def processNode(attributeReason: (Attribute, Reason)): Unit = {
      val (attribute, reason) = attributeReason
      attribute match {
        case drugAttribute: DrugAttribute => {
          val activation = getDrugAttributeActivation(drugAttribute)

          reason match {
            case Cure => {
              activation.get.cureOrigin match {
                case CureActivationOrigin.NoOrigin => {}
                case CureActivationOrigin.Cures(attributesSources) => {
                  for ((childAttribute, source) <- attributesSources) {
                    g += LkDiEdge(childAttribute, attribute)(s"cured_by/${ source }")
                    attributeQueue.enqueue((childAttribute, reason))
                  }
                }
                case CureActivationOrigin.Equals(childAttribute, source) => {
                  g += LkDiEdge(childAttribute, attribute)(s"equals/${ source }")
                  attributeQueue.enqueue((childAttribute, reason))
                }
                case CureActivationOrigin.IsSynonym(childAttribute, source) => {
                  g += LkDiEdge(childAttribute, attribute)(s"synonym_of/${ source }")
                  attributeQueue.enqueue((childAttribute, reason))
                }
              }
            }
            case SideEffectCause => {
              activation.get.sideEffectOrigin match {
                case SideEffectActivationOrigin.NoOrigin => {}
                case SideEffectActivationOrigin.ResponsibleFor(attributesSources) => {
                  for ((childAttribute, source) <- attributesSources) {
                    g += LkDiEdge(childAttribute, attribute)(s"caused_by_drug/${source}")
                    attributeQueue.enqueue((childAttribute, reason))
                  }
                }
                case SideEffectActivationOrigin.Equals(childAttribute, source) => {
                  g += LkDiEdge(childAttribute, attribute)(s"equals/${ source }")
                  attributeQueue.enqueue((childAttribute, reason))
                }
                case SideEffectActivationOrigin.IsSynonym(childAttribute, source) => {
                  g += LkDiEdge(childAttribute, attribute)(s"synonym_of/${ source }")
                  attributeQueue.enqueue((childAttribute, reason))
                }
              }
            }
            case Cause(_) => throw new Exception("Wrong reason for a drug attribute: Cause")
          }
        }
        case symptomAttribute: SymptomAttribute => {
          val activation = getSymptomAttributeActivation(symptomAttribute)

          val imax = reason match {
            case Cause(nextLevel) => {
              activation.get.levelActivation.slice(0, nextLevel).zipWithIndex.maxBy(_._1)._2
            }
            case _ => activation.get.levelActivation.zipWithIndex.maxBy(_._1)._2
          }
          activation.get.levelOrigin(imax) match {
            case SymptomActivationOrigin.NoOrigin => {}
            case SymptomActivationOrigin.UserInput => {
              // already processed
            }
            case SymptomActivationOrigin.HigherLevel(attributesSources) => {
              for ((childAttribute, source) <- attributesSources) {
                g += LkDiEdge(childAttribute, attribute)(s"caused_by_disease/${source}")
                attributeQueue.enqueue((childAttribute, Cause(imax)))
              }
            }
            case SymptomActivationOrigin.Equals(childAttribute, source) => {
              g += LkDiEdge(childAttribute, attribute)(s"equals/${ source }")
              attributeQueue.enqueue((childAttribute, reason))
            }
            case SymptomActivationOrigin.IsSynonym(childAttribute, source) => {
              g += LkDiEdge(childAttribute, attribute)(s"synonym_of/${ source }")
              attributeQueue.enqueue((childAttribute, reason))
            }
          }
        }
      }
    }

    while (attributeQueue.nonEmpty) {
      processNode(attributeQueue.dequeue())
    }

    val root = DotRootGraph(directed = true, id = Some(Id("MediNode Map")))
    def edgeTransformer(innerEdge: scalax.collection.Graph[Attribute, LkDiEdge]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = innerEdge.edge match {
      case LkDiEdge(source, target, rawLabel) => {
        val label = rawLabel.asInstanceOf[String]
        val color = if (label.startsWith("equals/")) {
          "darkgreen"
        } else if (label.startsWith("synonym_of/")) {
          "chartreuse"
        } else if (label.startsWith("caused_by_disease/")) {
          "deeppink1"
        } else if (label.startsWith("initial_symptom/")) {
          "cornflowerblue"
        } else if (label.startsWith("caused_by_drug/")) {
          "crimson"
        } else if (label.startsWith("cured_by/")) {
          "gold1"
        } else {
          throw new Exception(s"Unknow label prefix: ${label.split('/').head}")
        }
        Some((root, DotEdgeStmt(
          NodeId(source.toString),
          NodeId(target.toString),
          List(DotAttr(Id("label"), Id(label)),
               DotAttr(Id("color"), Id(color)))
        )))
      }
    }
    def nodeTransformer(innerNode: scalax.collection.Graph[Attribute, LkDiEdge]#NodeT): Option[(DotGraph, DotNodeStmt)] = {
      val attribute = innerNode.value
      if (attribute.isInstanceOf[SymptomName] && attribute.value == "User input") {
        Some((root, DotNodeStmt(NodeId(attribute.toString), List(
          DotAttr(Id("label"), Id("User input"))))))
      } else {
        val actsString = attribute match {
          case symptomAttribute: SymptomAttribute => {
            val activation = getSymptomAttributeActivation(symptomAttribute).get
            activation.levelActivation.zipWithIndex.map { case (act, lvl) => s"lvl_${lvl}: ${act}" }.mkString(", ")
          }
          case drugAttribute: DrugAttribute => {
            val activation = getDrugAttributeActivation(drugAttribute).get
            s"cure: ${activation.cureActivation}, se_source: ${activation.sideEffectActivation}"
          }
        }
        Some((root, DotNodeStmt(NodeId(attribute.toString), List(
          DotAttr(Id("label"), Id(
            s"${attribute.toString}\n${actsString}"
            ))
          ))))
      }
    }

    val dotContent = g.toDot(root, edgeTransformer, None, Some(nodeTransformer), Some(nodeTransformer))

    val writer = new PrintWriter(new File("graph_output/graph.dot"))
    writer.write(dotContent)
    writer.close()
  }

  def cures: Seq[(String, Double, CureActivationOrigin)] = {
    val theCures = for ((name, act) <- drugNameNodes if act.cureActivation > 0) yield { (name, act.cureActivation, act.cureOrigin) }
    theCures.toSeq.sortWith { case ((_, act1, _), (_, act2, _)) => act1 > act2 }
  }

  def sideEffectSources: Seq[(String, Double, SideEffectActivationOrigin)] = {
    val theSideEffectSources = for ((name, act) <- drugNameNodes if act.sideEffectActivation > 0) yield { (name, act.sideEffectActivation, act.sideEffectOrigin) }
    theSideEffectSources.toSeq.sortWith { case ((_, act1, _), (_, act2, _)) => act1 > act2 }
  }

  def causes: Seq[(String, Double, Int, SymptomActivationOrigin)] = {
    val theCauses = (for (i <- 1 until Parameters.CAUSE_LEVELS) yield {
      (for ((name, act) <- symptomNameNodes if act.levelActivation(i) > 0) yield {
        (name, act.levelActivation(i), i, act.levelOrigin(i))
      }).toSeq
    }).flatten
    theCauses.sortWith { case ((_, act1, _, _), (_, act2, _, _)) => act1 > act2 }
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
