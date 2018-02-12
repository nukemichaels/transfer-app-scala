package ru.test.revolut.transfer.core.accounting

import akka.actor.{ActorNotFound, ActorRefFactory, Props}
import akka.util.Timeout
import ru.test.revolut.transfer.core.accounting.models._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * @inheritdoc
  */
final class AccountActorsRegistry(actorRefFactory: ActorRefFactory,
                                  setup: AccountRegistrySetup) extends AccountsRegistry {

  implicit private val timeout: Timeout = 2.seconds

  def findAccount(desc: AccountDescriptor)
                 (implicit ec: ExecutionContext) = actorRefFactory
    .actorSelection(s"/user/${desc.toActorPath}")
    .resolveOne()
    .map(a => Some(a))
    .recover {
      case ActorNotFound(_) => None
    }

  def addAccount(desc: AccountDescriptor)
                (implicit ec: ExecutionContext) = findAccount(desc).map {
    case Some(_) => throw new AccountAlreadyExistException(desc)
    case None =>
      val account = setup.accountsGen(desc)
      actorRefFactory.actorOf(
        Props(new AccountActor(account)(setup.eventsListener, setup.operationValidator)),
        desc.toActorPath
      )
  }

}

object AccountActorsRegistry {
  def apply(setup: AccountRegistrySetup)
           (implicit actorRefFactory: ActorRefFactory): AccountActorsRegistry =
    new AccountActorsRegistry(actorRefFactory, setup)
}

