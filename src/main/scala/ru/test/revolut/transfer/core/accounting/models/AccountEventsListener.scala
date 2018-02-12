package ru.test.revolut.transfer.core.accounting.models

import ru.test.revolut.transfer.core.accounting.RequestId
import ru.test.revolut.transfer.core.accounting.models.OperationTypes.OperationType

/**
  * Events listener contract. Might be used to monitor/react on incoming account operations.
  */
trait AccountEventsListener {
  /**
    * Register operation event
    *
    * @param operationType operation type
    * @param id            request ID
    * @param amount        optional operation amount
    * @param descriptor    affected account descriptor
    */
  def event(operationType: OperationType, id: RequestId, amount: Option[Amount])
           (implicit descriptor: AccountDescriptor): Unit
}
