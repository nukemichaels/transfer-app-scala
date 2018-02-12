package ru.test.revolut.transfer.inmemory.core.account

import ru.test.revolut.transfer.core.accounting.InsufficientFundsException
import ru.test.revolut.transfer.core.accounting.models.{Account, MaterialOperation, Operation, OperationValidator}

import scala.util.{Failure, Success}

object SimpleOperationValidator extends OperationValidator {
  def validate(account: Account)(operation: Operation) = operation match {
    case op: MaterialOperation if account.total + op.toAmount.value < 0 =>
      Failure(new InsufficientFundsException(account.descriptor))
    case _ => Success(())
  }
}
