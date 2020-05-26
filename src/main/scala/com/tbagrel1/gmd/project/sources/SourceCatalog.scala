package com.tbagrel1.gmd.project.sources

import scala.collection.mutable

class SourceCatalog() {
  val br08303: Br08303 = new Br08303
  val chemicalSources: ChemicalSources = new ChemicalSources
  val drugbank: Drugbank = new Drugbank
  val hpAnnotations: HpAnnotations = new HpAnnotations
  val hpOntology: HpOntology = new HpOntology
  val meddra: Meddra = new Meddra
  val omim: Omim = new Omim
  val omimOntology: OmimOntology = new OmimOntology
  val orphadata: Orphadata = new Orphadata

  def createIndex(verbose: Boolean = false): Unit = {
    br08303.createIndex(verbose)
    chemicalSources.createIndex(verbose)
    drugbank.createIndex(verbose)
    hpOntology.createIndex(verbose)
    omim.createIndex(verbose)
    omimOntology.createIndex(verbose)
    orphadata.createIndex(verbose)
  }

  def getAllSymptomNames: mutable.Set[String] = {
    omimOntology.getSymptomNames union
    meddra.getSymptomNames union
    hpAnnotations.getSymptomNames union
    hpOntology.getSymptomNames union
    omim.getSymptomNames
  }
  def printStats: Unit = {
    println("--------------- DRUG STATS ---------------\n")
    println("------------- DRUG NAME STATS ------------\n")
    getDrugNamesStats
    println("------------------------------------------\n")
    println("------------- DRUG ATC STATS -------------\n")
    getDrugAtcStats
    println("------------------------------------------\n")
    println("----------- DRUG COMPOUND STATS ----------\n")
    getDrugCompoundStats
    println("------------------------------------------\n")

    println("------------- SYMPTOM STATS --------------\n")
    println("------------ SYMPTOM HP STATS ------------\n")
    getSymtpomHpStats
    println("------------------------------------------\n")
    println("------------ SYMPTOM HP STATS ------------\n")
    getSymtpomHpStats
    println("------------------------------------------\n")
  }

  def getSymtpomHpStats: Unit = { // TODO: check
    val symptomHpHpOntology = hpOntology.getSymtpomHp
    val symptomHpHpAnnotations = hpAnnotations.getSymtpomHp
    val allSymtpomHp = symptomHpHpOntology union symptomHpHpAnnotations
    val symptomHpHpOntologySize = symptomHpHpOntology.size.toFloat
    val symptomHpHpAnnotationsSize = symptomHpHpAnnotations.size.toFloat
    val allSymtpomHpSize = allSymtpomHp.size.toFloat
    val symptomHpHpOntologyRate = symptomHpHpOntologySize / allSymtpomHpSize
    val dsymptomHpHpAnnotationsRate = symptomHpHpAnnotationsSize / allSymtpomHpSize
    val drugCompoundAtcDrugbank = (symptomHpHpOntology intersect symptomHpHpAnnotations).size.toFloat / allSymtpomHpSize
    println(symptomHpHpOntology)
    println(symptomHpHpAnnotations)
    println(s"Nb symptomHp total : ${allSymtpomHpSize}\nNb symptomHp HpOntology : ${symptomHpHpOntologySize}\nNb symptomHp HpAnnotations ${symptomHpHpAnnotationsSize}\n" +
      s"Taux symptomHp HpOntology : ${symptomHpHpOntologyRate}\nTaux symptomHp HpAnnotations : ${dsymptomHpHpAnnotationsRate}\nTaux symptomHp HpOntology/HpAnnotations : ${drugCompoundAtcDrugbank}")
  }
  /*
  def getAllSymptomCui: Unit = { // TODO: check
    val symptomCuiOmimOntology = omimOntology.getSymptomCui
    val symptomCuiMeddra = meddra.getSymptomCui
    val allSymptomCui = symptomCuiOmimOntology union symptomCuiMeddra
    val symptomCuiOmimOntologySize = symptomCuiOmimOntology.size.toFloat
    val symptomCuiMeddraSize = symptomCuiMeddra.size.toFloat
    val allSymptomCuiSize = allSymptomCui.size.toFloat
    val symptomCuiOmimOntologyRate = symptomCuiOmimOntologySize /
    val symptomCuiMeddraRate = symptomCuiMeddraSize /


  }*/
  def getSymptomOmimStats: mutable.Set[String] = { // TODO: check
    omim.getSymptomOmim union
    omimOntology.getSymptomOmim union
    hpAnnotations.getSymptomOmim
  }
  def getDrugNamesStats: Unit = { // TODO: check
    br08303.getDrugNames union
    drugbank.getDrugNames
    val drugNameAtc = br08303.getDrugNames
    val drugNameDrugbank = drugbank.getDrugNames
    val allDrugName = drugNameAtc union drugNameDrugbank
    val drugNameAtcSize = drugNameAtc.size.toFloat
    val drugNameDrugbankSize = drugNameDrugbank.size.toFloat
    val allDrugNameSize = allDrugName.size.toFloat
    val drugNameAtcRate = drugNameAtcSize / allDrugNameSize
    val drugNameDrugBankRate = drugNameDrugbankSize / allDrugNameSize
    val drugCompoundAtcDrugbank = (drugNameAtc intersect drugNameDrugbank).size.toFloat / allDrugNameSize
    println(s"Nb drugName total : ${allDrugNameSize}\nNb drugName Atc : ${drugNameAtcSize}\nNb drugName Drugbank ${drugNameDrugbankSize}\n" +
      s"Taux drugName Atc : ${drugNameAtcRate}\nTaux drugName Drugbank : ${drugNameDrugBankRate}\nTaux drugName Atc/Drugbank : ${drugCompoundAtcDrugbank}")
  }
  def getDrugAtcStats:Unit = { // TODO: check
    val drugAtcAtc = br08303.getDrugAtc
    val drugAtcStich = chemicalSources.getDrugAtc
    val drugAtcDrugbank = drugbank.getDrugAtc
    val allDrugAtc = drugAtcAtc union drugAtcStich union drugAtcDrugbank
    val allDrugAtcSize = allDrugAtc.size.toFloat
    val drugAtcAtcRate = drugAtcAtc.size.toFloat / allDrugAtcSize // taux de DrugAtc dans la source ATC par rapport au total des DrugAtc trouvés
    val drugAtcStichRate = drugAtcStich.size.toFloat / allDrugAtcSize // taux de DrugAtc dans la source Stich par rapport au total des DrugAtc trouvés
    val drugAtcDrugbankRate = drugAtcDrugbank.size.toFloat / allDrugAtcSize // taux de DrugAtc dans la source Drugbank par rapport au total des DrugAtc trouvés
    val drugAtcAtcStichRate = (drugAtcAtc intersect drugAtcStich).size.toFloat / (drugAtcAtc union drugAtcStich).size.toFloat //intersect ou &
    val drugAtcAtcDrugbankRate = (drugAtcAtc intersect drugAtcDrugbank).size.toFloat / (drugAtcAtc union drugAtcDrugbank).size.toFloat
    val drugAtcStichDrugbankRate = (drugAtcStich intersect drugAtcDrugbank).size.toFloat / (drugAtcStich union drugAtcDrugbank).size.toFloat
    println(s"drugAtc pas dans Atc (car pas à jour) ${allDrugAtc.diff(drugAtcAtc)}")
    println(s"Nb drugAtc total : ${allDrugAtc.size}\nNb drugAtc Atc : ${drugAtcAtc.size}\nNb drugAtc Stich : ${drugAtcStich.size}\nNb drugAtc Drugbank : ${drugAtcDrugbank.size}\n" +
          s"\nTaux drugAtc Atc : ${drugAtcAtcRate}\nTaux drugAtc Stich : ${drugAtcStichRate}\nTaux drugAtc Drugbank : ${drugAtcDrugbankRate}\n" +
          s"\nTaux drugAtc Atc/Stich : ${drugAtcAtcStichRate}\nTaux drugAtc Atc/Drugbank : ${drugAtcAtcDrugbankRate}\nTaux drugAtc Stich/Drugbank : ${drugAtcStichDrugbankRate}\n")
  }
  def getDrugCompoundStats: Unit = { // TODO: check
      val drugCompoundStich = chemicalSources.getDrugCompound
      val drugCompoundMeddra = meddra.getDrugCompound
      val allDrugCompound = drugCompoundStich union drugCompoundMeddra
      val drugCompoundStichSize = drugCompoundStich.size.toFloat
      val drugCompoundMeddraSize = drugCompoundMeddra.size.toFloat
      val allDrugCompoundSize = allDrugCompound.size.toFloat
      val drugCompoundStichRate = drugCompoundStichSize / allDrugCompoundSize
      val drugCompoundMeddraRate = drugCompoundMeddraSize / allDrugCompoundSize
      val drugCompoundStichMeddra = (drugCompoundStich intersect drugCompoundMeddra).size.toFloat / allDrugCompoundSize
      println(s"Nb drugCompound total : ${allDrugCompoundSize}\nNb drugCompound Stich : ${drugCompoundStichSize}\nNb drugCompound Meddra ${drugCompoundMeddraSize}\n" +
              s"Taux drugCompound Stich : ${drugCompoundStichRate}\nTaux drugCompound Meddra : ${drugCompoundMeddraRate}\nTaux drugCompound Stich/Meddra : ${drugCompoundStichMeddra}")
  }

}
