package ru.test.revolut.transfer.core.accounting

import akka.actor.Actor
import ru.test.revolut.transfer.core.accounting.AccountingProtocol.OperationResult._
import ru.test.revolut.transfer.core.accounting.AccountingProtocol._
import ru.test.revolut.transfer.core.accounting.models.AccountsRegistry

import scala.util.{Failure, Success}

/**
  * Dispatching actor perform operational messages routing to specific account-serving actors and
  * perform account management features (new accounts creation) in sequential thread-safe manner.
  *
  * @param registry instance of <b>AccountsRegistry</b>
  */
private[accounting] class DispatchingActor(registry: AccountsRegistry) extends Actor {

  implicit private val ec = context.dispatcher

  def receive = {
    case msg: OperationRequest[_] =>
      val replyTo = sender
      val result = registry.findAccount(msg.accountDescriptor)
      result.onComplete {
        case Success(Some(account)) => account.tell(msg.payload, replyTo)
        case Success(None) => replyTo ! ErrorResult(new UnknownAccountException(msg.accountDescriptor))
        case Failure(e) => replyTo ! ErrorResult(e)
      }
    case NewAccount(desc) =>
      val replyTo = sender
      registry.addAccount(desc).onComplete {
        case Success(_) => replyTo ! OperationResult(Success(()))
        case Failure(e) => replyTo ! OperationResult(Failure(e))
      }
  }

}
