package ru.test.revolut.transfer.core.accounting

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Suite}

abstract class AkkaSpec extends TestKit(ActorSystem()) with FlatSpecLike with BeforeAndAfterAll {
  this: Suite =>

  override protected def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }
}
