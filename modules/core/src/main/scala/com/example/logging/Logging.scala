package com.example.logging

import org.log4s._
/**
  * Created by ezhoga on 26.08.16.
  */
trait Logging {
  val log = getLogger(super.getClass)
}
