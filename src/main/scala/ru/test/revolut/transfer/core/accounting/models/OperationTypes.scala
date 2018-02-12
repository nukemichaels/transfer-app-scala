package ru.test.revolut.transfer.core.accounting.models

object OperationTypes extends Enumeration {
  type OperationType = OperationTypes.Value
  val Withdrawal = Value("withdrawal")
  val Refill = Value("refill")
  val Lock = Value("lock")
  val Complete = Value("complete")
  val Unlock = Value("unlock")
  val Transfer = Value("transfer")
}