package ru.test.revolut.transfer.core.accounting

import akka.actor.{Props, Terminated}
import akka.testkit.{ImplicitSender, TestActorRef}
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import ru.test.revolut.transfer.core.accounting.AccountingProtocol.OperationResult

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import scala.util.{Failure, Success}

class OneShotActorSpec extends AkkaSpec with Matchers with ScalaFutures with ImplicitSender {

  private val Operation = "operation"

  it should "forward requests and complete promise with result and die on target success" in {
    val p = Promise[String]()
    val subj = newSubj()
    subj ! CompletableRequest(Operation, p)
    expectMsg(Operation)
    subj ! OperationResult(Success(Operation))
    receiveOne(1.second) should matchPattern {
      case Terminated(`subj`) =>
    }
    whenReady(p.future) { r =>
      r should be(Operation)
    }
  }

  it should "forward requests and complete promise with failure and die on target failure" in {
    val p = Promise[String]()
    val subj = newSubj()
    subj ! CompletableRequest(Operation, p)
    expectMsg(Operation)
    subj ! OperationResult(Failure(new RuntimeException))
    receiveOne(1.second) should matchPattern {
      case Terminated(`subj`) =>
    }
    Await.ready(p.future, 1.second).value should matchPattern {
      case Some(Failure(_: RuntimeException)) =>
    }
  }

  it should "forward requests and complete promise with failure and die on unexpected response" in {
    val p = Promise[String]()
    val subj = newSubj()
    subj ! CompletableRequest(Operation, p)
    expectMsg(Operation)
    subj ! Operation
    receiveOne(1.second) should matchPattern {
      case Terminated(`subj`) =>
    }
    Await.ready(p.future, 1.second).value should matchPattern {
      case Some(Failure(_: IllegalArgumentException)) =>
    }
  }

  private def newSubj() = {
    val a = TestActorRef(Props(new OneShotActor(self)))
    watch(a)
    a
  }
}
