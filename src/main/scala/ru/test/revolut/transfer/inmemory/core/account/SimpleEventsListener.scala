package ru.test.revolut.transfer.inmemory.core.account

import com.typesafe.scalalogging.LazyLogging
import ru.test.revolut.transfer.core.accounting.RequestId
import ru.test.revolut.transfer.core.accounting.models.OperationTypes.OperationType
import ru.test.revolut.transfer.core.accounting.models.{AccountDescriptor, AccountEventsListener, Amount}

object SimpleEventsListener extends AccountEventsListener with LazyLogging {

  def event(operationType: OperationType, id: RequestId, amount: Option[Amount])
           (implicit descriptor: AccountDescriptor): Unit = {
    val base = s"request:[$id] with $operationType on account ${descriptor.number}"
    logger.info(amount.map(a => base + s" for $a(${descriptor.ccy})").getOrElse(base))
  }

}
