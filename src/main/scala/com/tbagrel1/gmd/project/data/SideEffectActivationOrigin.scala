package com.tbagrel1.gmd.project.data

import scala.collection.mutable.ArrayBuffer

object SideEffectActivationOrigin {
  case object NoOrigin extends SideEffectActivationOrigin
  case class Equals(attribute: DrugAttribute, source: String)  extends SideEffectActivationOrigin
  case class IsSynonym(attribute: DrugAttribute, source: String)  extends SideEffectActivationOrigin
  case class ResponsibleFor(attributesSources: ArrayBuffer[(SymptomAttribute, String)])  extends SideEffectActivationOrigin
}

sealed trait SideEffectActivationOrigin
