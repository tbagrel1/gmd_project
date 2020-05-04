package com.tbagrel1.gmd.project.data

import scala.collection.mutable.ArrayBuffer

object CureActivationOrigin {
  case object NoOrigin extends CureActivationOrigin
  case class Equals(attribute: DrugAttribute, source: String)  extends CureActivationOrigin
  case class IsSynonym(attribute: DrugAttribute, source: String)  extends CureActivationOrigin
  case class Cures(attributesSources: ArrayBuffer[(SymptomAttribute, String)]) extends CureActivationOrigin
}

sealed trait CureActivationOrigin
