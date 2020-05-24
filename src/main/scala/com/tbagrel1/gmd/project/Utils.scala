package com.tbagrel1.gmd.project

import scala.collection.{IterableOps, mutable}

object Utils {
  def stripPrefix(input: String, prefix: String): String = {
    if (!input.startsWith(prefix)) {
      throw new Exception(s"""Prefix "${prefix}" not found in "${input}"""")
    }
    input.slice(prefix.length, input.length)
  }

  def normalize(input: String): String = {
    input
      .strip
      .toUpperCase
      .map(c => if ("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 -" contains c) { c } else { ' ' })
      .replaceAll(" +", " ")
  }

  def enrichFlatMap[A](set: mutable.Set[A], f: A => IterableOnce[A]): mutable.Set[A] = {
    set union set.flatMap(f)
  }

  def enrichFlatMapN[A](set: mutable.Set[A], f: A => IterableOnce[A], n: Int): mutable.Set[A] = {
    var theSet = set
    for (_ <- 0 until n) {
      theSet = enrichFlatMap(theSet, f)
    }
    theSet
  }

  def enrichFlatMapAll[A](set: mutable.Set[A], f: A => IterableOnce[A], n: Int): mutable.Set[A] = {
    var prevSet = mutable.Set.empty[A]
    var newSet = set
    while (newSet != prevSet) {
      prevSet = newSet
      newSet = enrichFlatMap(prevSet, f)
    }
    newSet
  }
}
