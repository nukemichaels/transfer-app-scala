package ru.test.revolut.transfer.core.accounting

import java.time.OffsetDateTime
import java.util.UUID

import akka.actor.Props
import akka.testkit.{ImplicitSender, TestActorRef}
import org.mockito.Mockito.{times, verify, verifyZeroInteractions, when}
import org.mockito.{Matchers => MM}
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar._
import ru.test.revolut.transfer.core.accounting.AccountingProtocol._
import ru.test.revolut.transfer.core.accounting.models._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class AccountActorSpec extends AkkaSpec with Matchers with ImplicitSender {
  private val Descriptor = AccountDescriptor("account", "USD")
  private val Total = BigDecimal(666)
  private val Id: RequestId = UUID.randomUUID()
  private val Locks: Map[RequestId, Amount] = Map(UUID.randomUUID() -> Amount(42, OffsetDateTime.now(), Some("comment")))
  private val Operation = {
    val m = mock[Operation]
    when(m.operationType).thenReturn(OperationTypes.Withdrawal)
    when(m.requestId).thenReturn(Id)
    when(m.timeStamp).thenReturn(OffsetDateTime.now())
    m
  }

  it should "pass mutation operations and validator to current account state" in {
    val nextState = newAccount()
    val initial = newAccount(Success(nextState))
    val (subj, _, validator) = newSubj(initial)
    subj ! Operation
    expectMsg(OperationResult(Success(())))
    subj ! Operation
    receiveOne(1.second) should matchPattern {
      case OperationResult(Failure(_: RuntimeException)) =>
    }
    verify(initial, times(1)).applyOperation(MM.eq(Operation))(MM.eq(validator))
    verify(nextState, times(1)).applyOperation(MM.eq(Operation))(MM.eq(validator))
  }

  it should "pass notifications to events listener on material operations" in {
    val initial = newAccount(Success(newAccount()))
    val (subj, Some(listener), validator) = newSubj(initial, withEventsListener = true)
    subj ! Operation
    expectMsg(OperationResult(Success(())))
    verify(initial, times(1)).applyOperation(MM.eq(Operation))(MM.eq(validator))
    verify(listener, times(1))
      .event(MM.eq(OperationTypes.Withdrawal), MM.eq(Id), MM.eq(None))(MM.eq(Descriptor))
  }

  it should "pass housekeeping operations to current account state bypassing validator" in {
    val state = newAccount()
    val (subj, Some(listener), validator) = newSubj(state, withEventsListener = true)
    subj ! GetTotal
    subj ! GetLocks
    expectMsgAllOf(OperationResult(Success(Total)), OperationResult(Success(Locks)))
    verifyZeroInteractions(listener, validator)
    verify(state, times(1)).total
    verify(state, times(1)).locks
  }

  it should "return failure on unsupported message" in {
    val state = newAccount()
    val (subj, Some(listener), validator) = newSubj(state, withEventsListener = true)
    subj ! "Some strange message"
    val result = receiveOne(1.second)
    result should matchPattern {
      case OperationResult(Failure(_: IllegalArgumentException)) =>
    }
    verifyZeroInteractions(listener, validator)
  }

  private def newSubj(initialState: Account, withEventsListener: Boolean = false) = {
    val validator = mock[OperationValidator]
    val listener = if (withEventsListener) Some(mock[AccountEventsListener]) else None
    (TestActorRef(Props(new AccountActor(initialState)(listener, validator))), listener, validator)
  }

  private def newAccount(nextState: Try[Account] = Failure(new RuntimeException)) = {
    val m = mock[Account]
    when(m.applyOperation(MM.any())(MM.any())).thenReturn(nextState)
    when(m.total).thenReturn(Total)
    when(m.locks).thenReturn(Locks)
    when(m.descriptor).thenReturn(Descriptor)
    m
  }
}
