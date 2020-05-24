package com.tbagrel1.gmd.project

import scala.collection.mutable.ArrayBuffer

case class DrugActivation(var cureActivation: Double, var cureOrigin: CureActivationOrigin, var sideEffectActivation: Double, var sideEffectOrigin: SideEffectActivationOrigin) {
}

case class SymptomActivation(levelActivation: ArrayBuffer[Double], levelOrigin: ArrayBuffer[SymptomActivationOrigin]) {

}
