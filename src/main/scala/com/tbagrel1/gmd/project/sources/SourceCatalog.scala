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

  def getAllSymtpomHp: mutable.Set[String] = { // TODO: check
    hpOntology.getSymtpomHp union
    hpAnnotations.getSymtpomHp
  }
  def getAllSymptomCui: mutable.Set[String] = { // TODO: check
    omimOntology.getSymptomCui union
    meddra.getSymptomCui
  }
  def getAllSymptomOmim: mutable.Set[String] = { // TODO: check
    omim.getSymptomOmim union
    omimOntology.getSymptomOmim union
    hpAnnotations.getSymptomOmim
  }
  def getAllDrugNames: mutable.Set[String] = { // TODO: check
    br08303.getDrugNames union
    drugbank.getDrugNames
  }
  def getAllDrugAtc: mutable.Set[String] = { // TODO: check
    val drugAtcAtc = br08303.getDrugAtc
    val drugAtcStich = chemicalSources.getDrugAtc
    val drugAtcDrugbank = drugbank.getDrugAtc
    val allDrugAtc = drugAtcAtc union drugAtcStich union drugAtcDrugbank
    val drugAtcAtcRate = br08303.getDrugAtc.size / allDrugAtc.size // taux de DrugAtc dans la source ATC par rapport au total des DrugAtc trouvés
    val drugAtcStichRate = chemicalSources.getDrugAtc.size / allDrugAtc.size // taux de DrugAtc dans la source Stich par rapport au total des DrugAtc trouvés
    val drugAtcDrugbankRate = drugbank.getDrugAtc.size / allDrugAtc.size // taux de DrugAtc dans la source Drugbank par rapport au total des DrugAtc trouvés
    val drugAtcAtcStichRate = (drugAtcAtc intersect drugAtcStich).size / allDrugAtc.size //intersect ou &
    val drugAtcAtcDrugbankRate = (drugAtcAtc intersect drugAtcDrugbank).size / allDrugAtc.size
    println(s"Nb drugAtc total : ${allDrugAtc.size}\nNb drugAtc Atc : ${drugAtcAtc.size}\nNb drugAtc Stich : ${drugAtcStich.size}\nNb drugAtc Drugbank : ${drugAtcDrugbank}" +
          s"\nTaux drugAtc Atc : ${drugAtcAtcRate}\nTaux drugAtc Stich : ${drugAtcStichRate}\nTaux drugAtc Drugbank : ${drugAtcDrugbankRate}" +
          s"\nTaux drugAtc Atc/Stich : ${drugAtcAtcStichRate}\nTaux drugAtc Atc/Drugbank : ${drugAtcAtcDrugbankRate}\n")
    allDrugAtc
  }
  def getAllDrugCompound: mutable.Set[String] = { // TODO: check
    chemicalSources.getDrugCompound union
    meddra.getDrugCompound
  }

}
