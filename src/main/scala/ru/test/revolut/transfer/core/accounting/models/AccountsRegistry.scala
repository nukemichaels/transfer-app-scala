package ru.test.revolut.transfer.core.accounting.models

import akka.actor.ActorRef

import scala.concurrent.{ExecutionContext, Future}

/**
  * A discovery and registration service for account-serving actors.
  */
trait AccountsRegistry {

  /**
    * Performs a search for account-serving actor for given descriptor
    *
    * @param desc account descriptor
    * @param ec   execution context
    * @return Return optional account-serving actor reference if it was previously registered with <b>addActor<b/>
    */
  def findAccount(desc: AccountDescriptor)
                 (implicit ec: ExecutionContext): Future[Option[ActorRef]]

  /**
    * Register an account-serving actor for given account descriptor
    *
    * @param desc account descriptor
    * @param ec   execution context
    * @return Account-serving actor reference if actor was successfully created
    */
  def addAccount(desc: AccountDescriptor)
                (implicit ec: ExecutionContext): Future[ActorRef]
}

case class AccountRegistrySetup(eventsListener: Option[AccountEventsListener] = None,
                                operationValidator: OperationValidator,
                                accountsGen: AccountDescriptor => Account)