package com.example.backend

/**
  * Created by ezhoga on 05.11.16.
  */
trait Checker {
  def process(): Unit
}

class SomeChecker extends Checker {
  def process() = ()
}
class AnotherChecker extends Checker {
  def process() = ()
}

