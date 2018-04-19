package com.satansk

/**
  * Author: Song Kun
  * Date:   上午8:59 at 18/4/19
  * Email:  satansk@hotmail.com
  */
class ActorFSM {

}

object ActorFSM {
  sealed trait State

  case object Disconnected extends State
  case object Connected extends State
  case object ConnectedAndPending extends State
}