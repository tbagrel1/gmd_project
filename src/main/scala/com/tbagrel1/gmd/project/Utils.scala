package com.tbagrel1.gmd.project

object Utils {
  def stripPrefix(input: String, prefix: String): String = {
    if (!input.startsWith(prefix)) {
      throw new Exception(s"""Prefix "${prefix}" not found in "${input}"""")
    }
    input.slice(prefix.length, input.length)
  }

  def normalize(input: String): String = {
    input
      .toUpperCase
      .map(c => if ("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 -" contains c) { c } else { ' ' })
      .replaceAll(" +", " ")
  }
}
