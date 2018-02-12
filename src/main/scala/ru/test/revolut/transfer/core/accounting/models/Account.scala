package ru.test.revolut.transfer.core.accounting.models

import java.time.OffsetDateTime

import ru.test.revolut.transfer.core.accounting.RequestId

import scala.util.Try

/**
  * Account entity contract. All implementations would be treated as immutable objects.
  * Thread safety is an optional feature for implementations - all Account methods would be executed in
  * thread-safe sequential manner.
  */
trait Account {

  /**
    * Perform given operation on account with method <b>doApplyOperation</b> if validation passed successfully
    *
    * @param op                 account operation
    * @param operationValidator validator instance
    * @return New account state derived from current or Failure if operation or validation were unsuccessful
    */
  final def applyOperation(op: Operation)
                          (implicit operationValidator: OperationValidator): Try[Account] = for {
    _ <- operationValidator.validate(this)(op)
    result <- doApplyOperation(op)
  } yield result

  /**
    * Perform given operation on account
    *
    * @param op account operation
    * @return New account state derived from current or Failure if operation was unsuccessful
    */
  protected def doApplyOperation(op: Operation): Try[Account]

  /**
    * @return total amount for current account state as a balance of fixed, locked and initial amounts
    */
  def total: BigDecimal

  /**
    * @return locked amounts for current account state
    */
  def locks: Map[RequestId, Amount]

  /**
    * @return set of fixed amounts including all withdrawals, refills and confirmed locks
    */
  def fixed: Set[Amount]

  /**
    * Current account descriptor {@see AccountDescriptor}
    *
    * @return account descriptor
    */
  def descriptor: AccountDescriptor
}

case class Amount(value: BigDecimal, timeStamp: OffsetDateTime, comment: Option[String])
