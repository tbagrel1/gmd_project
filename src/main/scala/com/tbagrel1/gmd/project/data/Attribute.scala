package com.tbagrel1.gmd.project.data

sealed trait Attribute {
  def value: String
}

sealed trait DrugAttribute extends Attribute
sealed trait SymptomAttribute extends Attribute

case class DrugName(value: String) extends DrugAttribute
case class DrugAtc(value: String) extends DrugAttribute
case class DrugCompound(value: String) extends DrugAttribute
case class SymptomName(value: String) extends SymptomAttribute
case class SymptomCui(value: String) extends SymptomAttribute
case class SymptomOmim(value: String) extends SymptomAttribute
case class SymptomHp(value: String) extends SymptomAttribute
