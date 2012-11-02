package net.renalias.tawebf.core

import java.util.logging.{Logger=>JavaLogger}
import java.util.logging.Level._

trait Logger {

  lazy val log = new {
    self =>

    val logger = JavaLogger.getLogger(self.getClass.getName)

    def debug[T, U >: Throwable](s: => T, e:Option[U] = None) = logger.log(FINE, s.toString, e.getOrElse(null))
    def warn[T, U >: Throwable](s: => T, e:Option[U] = None) = logger.log(WARNING, s.toString, e.getOrElse(null))
    def error[T, U >: Throwable](s: => T, e:Option[U] = None) = logger.log(SEVERE, s.toString, e.getOrElse(null))
    def info[T, U >: Throwable](s: => T, e:Option[U] = None) = logger.log(INFO, s.toString, e.getOrElse(null))
  }
}