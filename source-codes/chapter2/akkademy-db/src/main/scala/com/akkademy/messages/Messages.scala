package com.akkademy.messages

/**
  * Author: Kyle Song
  * Date:   PM4:21 at 18/4/15
  * Email:  satansk@hotmail.com
  *
  * case class 默认支持序列化、反序列化
  */
case class SetRequest(key: String, value: Object)
case class SetIfNotExists(key: String, value: Object)
case class GetRequest(key: String)
case class Delete(key: String)
case class KeyNotFoundException(key: String) extends Exception