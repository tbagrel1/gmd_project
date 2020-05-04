package com.tbagrel1.gmd.project.data

import scala.collection.mutable.ArrayBuffer

object Activation {
  val EQUAL_TRANSMISSION_COEFF: Double = 0.95
  val SYNONYM_TRANSMISSION_COEFF: Double = 0.75
  val HIGHER_SYMPTOM_TRANSMISSION_COEFF: Double = 0.90
}

case class DrugActivation(cureActivation: Double, cureOrigin: CureActivationOrigin, sideEffectActivation: Double, sideEffectOrigin: SideEffectActivationOrigin) {
}

case class SymptomActivation(levelActivation: ArrayBuffer[Double], levelOrigin: ArrayBuffer[SymptomActivationOrigin]) {

}
