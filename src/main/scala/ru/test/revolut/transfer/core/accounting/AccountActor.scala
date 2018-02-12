package ru.test.revolut.transfer.core.accounting

import akka.actor.Actor
import ru.test.revolut.transfer.core.accounting.AccountingProtocol.OperationResult._
import ru.test.revolut.transfer.core.accounting.models.{Account, AccountEventsListener, Operation, OperationValidator}

import scala.util.{Success, Try}

/**
  * Implementation of account-serving actor.
  * Encapsulate sequential processing of account events, e.g. mutation and housekeeping operations.
  * Handle messages of two kinds: subtypes of <b>Operation</b> and <b>HousekeepingOperation</b>
  *
  * @param initialState       Initial account state
  * @param eventsListener     Optional events listener
  * @param operationValidator Operations validator
  */
private[accounting] class AccountActor(initialState: Account)
                                      (eventsListener: Option[AccountEventsListener] = None,
                                       implicit private val operationValidator: OperationValidator) extends Actor {

  import AccountingProtocol._

  private def doReceive(state: Account): Receive = {
    case op: Operation =>
      reply(switchState(state.applyOperation(op)))
      eventsListener.foreach(_.event(op.operationType, op.requestId, op.toAmountOpt)(state.descriptor))
    case GetTotal => reply(OperationResult(Success(state.total)))
    case GetLocks => reply(OperationResult(Success(state.locks)))
    case other => reply(ErrorResult(new IllegalArgumentException(s"Unsupported message $other")))
  }

  private def switchState(state: Try[Account]) = state
    .map(s => context.become(doReceive(s)))
    .transform(_ => Success(OkResult), e => Success(ErrorResult(e)))
    .get

  private def reply[T](response: AccountingResponse[T]) = sender ! response

  def receive = doReceive(initialState)

}
