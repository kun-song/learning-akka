package com.satansk

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, OneForOneStrategy, SupervisorStrategy}
import com.satansk.ManagerActor.{BrokenPlateException, DrunkenException, FireError, TiredException}

/**
  * Author: Kyle Song
  * Date:   PM11:29 at 18/4/17
  * Email:  satansk@hotmail.com
  */
class ManagerActor extends Actor {
  override def receive: Receive = ???

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10) {
      case BrokenPlateException ⇒ Resume
      case DrunkenException     ⇒ Restart
      case TiredException       ⇒ Stop
      case FireError            ⇒ Escalate
      case _                    ⇒ Escalate
    }
}

object ManagerActor {
  case object BrokenPlateException extends Throwable
  case object DrunkenException extends Throwable
  case object FireError extends Error
  case object TiredException extends Throwable
}