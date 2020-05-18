package com.tbagrel1.gmd.project

object Utils {
  def normalize(input: String): String = {
    // TODO
    input
      .toUpperCase
      .map(c => if ("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 -" contains c) { c } else { ' ' })
      .replaceAll(" +", " ")
  }
}
