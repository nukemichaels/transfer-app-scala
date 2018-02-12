package ru.test.revolut.transfer.core.accounting

import akka.actor.{ActorRef, Props}
import akka.testkit.{ImplicitSender, TestActorRef}
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{Matchers => MM}
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar._
import ru.test.revolut.transfer.core.accounting.AccountingProtocol.{DoTransaction, NewAccount, OperationResult}
import ru.test.revolut.transfer.core.accounting.models.{AccountDescriptor, AccountsRegistry, Operation}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class DispatchingActorSpec extends AkkaSpec with Matchers with ImplicitSender {

  private val Descriptor = AccountDescriptor("account", "USD")
  private val Operation = mock[Operation]
  private val Transaction = DoTransaction(Descriptor, Operation)
  private val AddAccount = NewAccount(Descriptor)
  private val timeout = 1.seconds

  "on operation" should "forward operation to actor on existing account" in {
    val registry = newSuccessfulRegistry(Some(self))
    val subj = TestActorRef(Props(new DispatchingActor(registry)))
    subj ! Transaction
    expectMsg(timeout, Operation)
    lastSender should be(self)
    verify(registry, times(1)).findAccount(MM.eq(Descriptor))(MM.any())
  }

  it should "return error on unknown account" in {
    val registry = newSuccessfulRegistry(None)
    val subj = TestActorRef(Props(new DispatchingActor(registry)))
    subj ! Transaction
    val result = receiveOne(timeout)
    result should matchPattern {
      case OperationResult(Failure(_: UnknownAccountException)) =>
    }
    lastSender should be(subj)
    verify(registry, times(1)).findAccount(MM.eq(Descriptor))(MM.any())
  }

  it should "return error result on addAccount failure" in {
    val registry = newFaultyRegistry()
    val subj = TestActorRef(Props(new DispatchingActor(registry)))
    subj ! Transaction
    val result = receiveOne(timeout)
    result should matchPattern {
      case OperationResult(Failure(_: RuntimeException)) =>
    }
    lastSender should be(subj)
    verify(registry, times(1)).findAccount(MM.eq(Descriptor))(MM.any())
  }

  "on add account" should "add account if it not exist" in {
    val registry = newSuccessfulRegistry(Some(self))
    val subj = TestActorRef(Props(new DispatchingActor(registry)))
    subj ! AddAccount
    expectMsg(timeout, OperationResult(Success(())))
    lastSender should be(subj)
    verify(registry, times(1)).addAccount(MM.eq(Descriptor))(MM.any())
  }

  it should "fail if account already exist" in {
    val registry = newFaultyRegistry()
    val subj = TestActorRef(Props(new DispatchingActor(registry)))
    subj ! AddAccount
    val result = receiveOne(timeout)
    result should matchPattern {
      case OperationResult(Failure(_: AccountAlreadyExistException)) =>
    }
    lastSender should be(subj)
    verify(registry, times(1)).addAccount(MM.eq(Descriptor))(MM.any())
  }

  private def newSuccessfulRegistry(actorRef: Option[ActorRef]) = {
    val m = mock[AccountsRegistry]
    when(m.findAccount(MM.any())(MM.any())).thenReturn(Future {
      actorRef
    })
    when(m.addAccount(MM.any())(MM.any()))
      .thenReturn(Future {
        actorRef.getOrElse(throw new AccountAlreadyExistException(Descriptor))
      })
    m
  }

  private def newFaultyRegistry() = {
    val m = mock[AccountsRegistry]
    when(m.findAccount(MM.any())(MM.any())).thenReturn(Future.failed(new RuntimeException))
    when(m.addAccount(MM.any())(MM.any())).thenReturn(Future.failed(new AccountAlreadyExistException(Descriptor)))
    m
  }
}
