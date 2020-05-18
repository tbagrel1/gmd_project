package com.tbagrel1.gmd.project

import scala.collection.mutable.ArrayBuffer

object SymptomActivationOrigin {
  case object NoOrigin extends SymptomActivationOrigin
  case object UserInput  extends SymptomActivationOrigin
  case class Equals(attribute: SymptomAttribute, source: String)  extends SymptomActivationOrigin
  case class IsSynonym(attribute: SymptomAttribute, source: String)  extends SymptomActivationOrigin
  case class HigherLevel(attributesSources: ArrayBuffer[(SymptomAttribute, String)])  extends SymptomActivationOrigin
}

sealed trait SymptomActivationOrigin
