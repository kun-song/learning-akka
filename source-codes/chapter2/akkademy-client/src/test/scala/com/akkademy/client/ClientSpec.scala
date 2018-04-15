package com.akkademy.client

import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Author: Kyle Song
  * Date:   PM5:17 at 18/4/15
  * Email:  satansk@hotmail.com
  */
class ClientSpec extends FunSpecLike with Matchers {

  val client = new Client("127.0.0.1:2552")

  describe("akkademyDb client") {
    it("should set a value") {
      client.set("a", "666")

      val resultF = client.get("a")
      val result = Await.result(resultF, 1 second)

      result shouldEqual "666"
    }
  }
}
