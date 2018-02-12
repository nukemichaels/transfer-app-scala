package ru.test.revolut.transfer.inmemory.core.account

import java.time.OffsetDateTime

import ru.test.revolut.transfer.core.accounting._
import ru.test.revolut.transfer.core.accounting.models.OperationTypes._
import ru.test.revolut.transfer.core.accounting.models._

import scala.util.{Failure, Success, Try}

case class InMemoryAccount(balance: BigDecimal,
                           descriptor: AccountDescriptor,
                           locks: Map[RequestId, Amount] = Map(),
                           fixed: Set[Amount] = Set()) extends Account {

  private val squashRecords = fixed.map(_.value).fold(balance)(_ + _) + locks.values.map(_.value).sum

  protected def doApplyOperation(op: Operation): Try[Account] = op match {
    case op: MaterialOperation if op.operationType == Lock => addLock(op.requestId, op.toAmount)
    case op: MaterialOperation => addAmount(op.toAmount)
    case UnlockOperation(id, _) => remLock(id)
    case ConfirmOperation(id, ts) => confirmLock(id, ts)
  }

  def total: BigDecimal = squashRecords

  private def confirmLock(id: RequestId, ts: OffsetDateTime): Try[Account] = locks
    .get(id)
    .map {
      amount => this.copy(locks = locks - id, fixed = fixed + amount.copy(timeStamp = ts))
    } match {
    case Some(s) => Success(s)
    case None => Failure(new IllegalArgumentException(s"No lock found for id $id"))
  }

  private def addLock(id: RequestId, amount: Amount) = Try {
    this.copy(locks = locks + (id -> amount))
  }

  private def remLock(id: RequestId) = Try {
    this.copy(locks = locks - id)
  }

  private def addAmount(amount: Amount) = Try {
    this.copy(fixed = fixed + amount)
  }
}

object InMemoryAccount {
  def generate(desc: AccountDescriptor) = InMemoryAccount(0, desc)
}