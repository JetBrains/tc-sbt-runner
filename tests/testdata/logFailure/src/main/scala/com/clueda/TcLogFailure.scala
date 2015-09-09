/*
 * Copyright (C) 2014 Clueda AG.
 * This work is proprietary and confidential. Any distribution,
 * reproduction, or modification is strictly prohibited under any
 * circumstances without the express prior written permission of Clueda
 * AG. All rights reserved.
 */
package com.clueda

/** The main entry point for TC log failure. */
object TcLogFailure extends App {
  val enabled = sys.env.get("TEAMCITY_VERSION")
  println(s"TC version: $enabled")
}
