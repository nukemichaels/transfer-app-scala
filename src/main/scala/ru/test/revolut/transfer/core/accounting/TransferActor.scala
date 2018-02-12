package ru.test.revolut.transfer.core.accounting

import akka.actor.{Actor, ActorRef, PoisonPill}
import ru.test.revolut.transfer.core.accounting.AccountingProtocol.OperationResult._
import ru.test.revolut.transfer.core.accounting.AccountingProtocol.{DoTransaction, OperationResult}
import ru.test.revolut.transfer.core.accounting.TransferActor.DoTransfer
import ru.test.revolut.transfer.core.accounting.models.{AccountDescriptor, TransferOperation}

import scala.util.{Failure, Success}

/**
  * One shot actor that performs money transfer from source account to destination and then takes the <b>PoisonPill</b>.
  * Transfer executed in three steps: amount lock on source account, amount refill on destination,
  * lock confirmation in case of successful refill.
  * If refill operation was'n successful, locked would be removed.
  * Would return <b>InsufficientFundsException</b> in transfer amount exceed available balance of source account.
  *
  * @param operationsDispatcher reference to the instance of dispatching actor { @see DispatchingActor}
  */
private[accounting] class TransferActor(operationsDispatcher: ActorRef) extends Actor {

  private def initial: Receive = {
    case DoTransfer(from, to, _) if from.equals(to) =>
      sender ! ErrorResult(new TransferException("Source and destination account should not be equal", from, to))
      die
    case DoTransfer(from, to, _) if !from.ccy.equals(to.ccy) =>
      sender ! ErrorResult(new TransferException("Source and destination currencies should be equal", from, to))
      die
    case DoTransfer(from, to, op) =>
      context.become(waitForLock(OperationContext(from, to, op, sender)))
      operationsDispatcher ! DoTransaction(from, op.lockingOperation)
    case other =>
      sender ! ErrorResult(new IllegalArgumentException(s"Unexpected message received $other"))
      die
  }

  private def waitForLock(opCtx: OperationContext): Receive = {
    case OperationResult(Success(_)) =>
      context.become(waitForRefill(opCtx))
      operationsDispatcher ! DoTransaction(opCtx.to, opCtx.transfer.refillOperation)
    case r: OperationResult[Failure[_]] =>
      opCtx.abort(r)
  }

  private def waitForRefill(opCtx: OperationContext): Receive = {
    case OperationResult(Success(_)) =>
      context.become(waitForFinish(opCtx))
      operationsDispatcher ! DoTransaction(opCtx.from, opCtx.transfer.confirmOperation)

    case r: OperationResult[Failure[_]] =>
      operationsDispatcher ! DoTransaction(opCtx.from, opCtx.transfer.unlockOperation)
      opCtx.abort(r)
  }

  private def waitForFinish(opCtx: OperationContext): Receive = {
    case s@OperationResult(Success(_)) =>
      opCtx.replyTo ! s
      die
    case r: OperationResult[Failure[_]] =>
      opCtx.abort(r)
  }

  private def ignorant: Receive = {
    case _ =>
  }

  private case class OperationContext(from: AccountDescriptor,
                                      to: AccountDescriptor,
                                      transfer: TransferOperation,
                                      replyTo: ActorRef) {

    def abort[T](errorMsg: OperationResult[Failure[T]]) = {
      replyTo ! errorMsg
      die
    }
  }

  private def die: Unit = {
    context.become(ignorant)
    self ! PoisonPill
  }

  def receive = initial

}

private[accounting] object TransferActor {

  case class DoTransfer(from: AccountDescriptor, to: AccountDescriptor, operation: TransferOperation)

}
