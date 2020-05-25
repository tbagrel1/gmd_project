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

  def getAllSymptomName(symptomName : String): mutable.Set[String] = {/*
    br08303.getSymptomName(symptomName)
    chemicalSources.getSymptomName(symptomName)
    drugbank.getSymptomName(symptomName)
    hpOntology.getSymptomName(symptomName)
    meddra.getSymptomName(symptomName)
    hpAnnotations.getSymptomName(symptomName)
    omim.getSymptomName(symptomName)
    omimOntology.getSymptomName(symptomName)
    orphadata.getSymptomName(symptomName)*/
    mutable.Set.empty
  }
}
