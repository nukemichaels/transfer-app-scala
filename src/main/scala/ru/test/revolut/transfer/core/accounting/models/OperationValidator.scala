package ru.test.revolut.transfer.core.accounting.models

import scala.util.Try

/**
  * Operation validator contact. Implementations should contain operations validation logic based on certain business rules or smth...
  * U know, "validation"...
  */
trait OperationValidator {
  /**
    * Perform validation logic.
    *
    * @param account   target operation account
    * @param operation account operation
    * @return Failure if operation should not be applied to account. Otherwise Success(())
    */
  def validate(account: Account)(operation: Operation): Try[Unit]
}
