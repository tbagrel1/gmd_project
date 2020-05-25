package com.tbagrel1.gmd.project

import com.outr.lucene4s.field.FieldType

object Parameters {
  val EQUAL_TRANSMISSION_COEFF: Double = 0.9
  val SYNONYM_TRANSMISSION_COEFF: Double = 0.75
  val HIGHER_SYMPTOM_TRANSMISSION_COEFF: Double = 0.8
  val NAME_FIELD_TYPE: FieldType = FieldType.Untokenized  // exact matching for names
  val EXACT_SQL: Boolean = NAME_FIELD_TYPE == FieldType.Untokenized
  val CAUSE_LEVELS: Int = 2
  val CUT_OFF_THRESHOLD: Double = 0.65
}
