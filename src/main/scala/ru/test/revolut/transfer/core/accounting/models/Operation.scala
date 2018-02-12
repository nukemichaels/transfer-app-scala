package ru.test.revolut.transfer.core.accounting.models

import java.time.OffsetDateTime
import java.util.Objects

import ru.test.revolut.transfer.core.accounting.RequestId
import ru.test.revolut.transfer.core.accounting.models.OperationTypes._

/**
  * Account operation contract
  */
sealed trait Operation {
  def requestId: RequestId

  def operationType: OperationType

  def timeStamp: OffsetDateTime
}

/**
  * Account operation that would affect current balance
  *
  * @param requestId     Operation ID
  * @param amount        Operation amount
  * @param operationType operation type, should be of material subcategory { @see OperationTypes.isMaterial}
  * @param timeStamp operation timestamp
  * @param comment   optional operation comment
  */
case class MaterialOperation(requestId: RequestId,
                             amount: BigDecimal,
                             operationType: OperationType,
                             timeStamp: OffsetDateTime = OffsetDateTime.now(),
                             comment: Option[String] = None) extends Operation {
  if (!operationType.isMaterial) throw new IllegalArgumentException(s"$operationType is not a material operation type")
  Objects.requireNonNull(requestId, "request id must be set")
  Objects.requireNonNull(amount, "amount should be set")
  Objects.requireNonNull(operationType, "operation type must be set")
}

/**
  * Funds transfer operation
  *
  * @param requestId Operation ID
  * @param amount    Operation amount
  * @param timeStamp operation timestamp
  * @param comment   optional operation comment
  */
case class TransferOperation(requestId: RequestId,
                             amount: BigDecimal,
                             timeStamp: OffsetDateTime = OffsetDateTime.now(),
                             comment: Option[String] = None) extends Operation {
  Objects.requireNonNull(requestId, "request id must be set")
  Objects.requireNonNull(amount, "amount should be set")
  if (amount <= 0) throw new IllegalArgumentException("Amount should be greater than 0")

  def operationType = Transfer

  /**
    * @return Locking operation with original requestId, amount, timeStamp and comment
    */
  def lockingOperation = MaterialOperation(requestId, amount, Lock, timeStamp, comment)

  /**
    * @return Lock confirmation operation with original requestId and timeStamp
    */
  def confirmOperation = ConfirmOperation(requestId, timeStamp)

  /**
    * @return Unlocking operation with original requestId and timeStamp
    */
  def unlockOperation = UnlockOperation(requestId, timeStamp)

  /**
    * @return Refill operation with original requestId, amount, timeStamp and comment
    */
  def refillOperation = MaterialOperation(requestId, amount, Refill, timeStamp, comment)
}

/**
  * Unlock operation. Might be used to release locked amounts.
  *
  * @param requestId operation request ID, should be equal to original lock operation ID
  * @param timeStamp operation timestamp
  */
case class UnlockOperation(requestId: RequestId, timeStamp: OffsetDateTime = OffsetDateTime.now()) extends Operation {
  Objects.requireNonNull(requestId, "request id must be set")

  def operationType = Unlock
}

/**
  * Confirm operation. Might be used to confirm locked amounts.
  *
  * @param requestId operation request ID, should be equal to original lock operation ID
  * @param timeStamp operation timestamp
  */
case class ConfirmOperation(requestId: RequestId, timeStamp: OffsetDateTime = OffsetDateTime.now()) extends Operation {
  Objects.requireNonNull(requestId, "request id must be set")

  def operationType = Complete
}
