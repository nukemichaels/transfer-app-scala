package ru.test.revolut.transfer

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import ru.test.revolut.transfer.api.AccountingController
import ru.test.revolut.transfer.core.accounting.models.AccountRegistrySetup
import ru.test.revolut.transfer.core.accounting.{AccountActorsRegistry, Accounting}
import ru.test.revolut.transfer.inmemory.core.account.{InMemoryAccount, SimpleEventsListener, SimpleOperationValidator}

object Starter extends App with LazyLogging {
  implicit val system = ActorSystem("transfer-app")
  implicit val ec = system.dispatcher
  val setup = AccountRegistrySetup(Some(SimpleEventsListener), SimpleOperationValidator, InMemoryAccount.generate)
  val registry = AccountActorsRegistry(setup)
  val accounting = Accounting(registry)
  val api = AccountingController(accounting)
  api.start("0.0.0.0", 8080)
  sys.addShutdownHook {
    system.terminate()
    logger.info("Application terminated")
  }
  logger.info("Application started")
}
