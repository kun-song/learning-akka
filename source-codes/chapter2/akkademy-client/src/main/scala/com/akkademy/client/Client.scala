package com.akkademy.client

import akka.pattern._
import akka.actor.ActorSystem
import akka.util.Timeout
import com.akkademy.messages.{Delete, GetRequest, SetRequest}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Author: Kyle Song
  * Date:   PM5:11 at 18/4/15
  * Email:  satansk@hotmail.com
  */
class Client(remoteAddress: String) {

  private implicit val timeout = Timeout(5 seconds)
  private implicit val system = ActorSystem("LocalSystem")

  /**
    * 通过 actorSelection 查找远程 Actor
    */
  private val remoteDb = system.actorSelection(s"akka.tcp://akkademy@$remoteAddress/user/akkademy-db")

  /**
    * 异步函数
    */
  def set(key: String, value: Object): Future[Any] = remoteDb ? SetRequest(key, value)
  def get(key: String): Future[Any] = remoteDb ? GetRequest(key)
  def delete(key: String): Future[Any] = remoteDb ? Delete(key)
}
