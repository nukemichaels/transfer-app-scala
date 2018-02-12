package ru.test.revolut.transfer.core.accounting

import ru.test.revolut.transfer.core.accounting.models.{AccountDescriptor, Operation}

import scala.util.{Failure, Success, Try}

private[accounting] object AccountingProtocol {

  sealed trait AccountingRequest {
    def accountDescriptor: AccountDescriptor
  }

  sealed trait OperationRequest[T] extends AccountingRequest {
    def payload: T
  }

  case class NewAccount(accountDescriptor: AccountDescriptor) extends AccountingRequest

  case class DoTransaction(accountDescriptor: AccountDescriptor,
                           payload: Operation) extends OperationRequest[Operation]

  case class DoHousekeeping(accountDescriptor: AccountDescriptor,
                            payload: HousekeepingOperation) extends OperationRequest[HousekeepingOperation]

  sealed trait HousekeepingOperation

  case object GetTotal extends HousekeepingOperation

  case object GetLocks extends HousekeepingOperation

  sealed trait AccountingResponse[T] {
    def result: Try[T]
  }

  case class OperationResult[T](result: Try[T]) extends AccountingResponse[T]

  object OperationResult {
    val OkResult = OperationResult(Success(()))

    def ErrorResult(t: Throwable) = OperationResult(Failure(t))
  }

}