package com.tbagrel1.gmd.project

import com.outr.lucene4s.field.FieldType

object Parameters {
  val EQUAL_TRANSMISSION_COEFF: Double = 0.95
  val SYNONYM_TRANSMISSION_COEFF: Double = 0.75
  val HIGHER_SYMPTOM_TRANSMISSION_COEFF: Double = 0.90
  val NAME_FIELD_TYPE: FieldType = FieldType.Stored  // "in" matching for names
}
