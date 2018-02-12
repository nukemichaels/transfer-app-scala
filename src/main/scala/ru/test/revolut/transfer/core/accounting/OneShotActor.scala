package ru.test.revolut.transfer.core.accounting

import akka.actor.{Actor, ActorRef, PoisonPill}
import ru.test.revolut.transfer.core.accounting.AccountingProtocol.OperationResult

import scala.concurrent.Promise
import scala.util.{Failure, Success, Try}

/**
  * One shot actor might be used as a substitution of <b>ask</b> pattern for operations accounting operations.
  * It send an operation from received <b>CompletableRequest</b> to <b>executor</b> actor,
  * wait for response of <b>OperationResult</b> type, completes promise given in <b>CompletableRequest</b> and
  * takes the <b>PoisonPill</b>.
  *
  * @param executor operations executor actor reference
  * @tparam RESPONSE type of resulting payload
  */
private[accounting] class OneShotActor[RESPONSE](executor: ActorRef) extends Actor {

  private def initial: Receive = {
    case rqst: CompletableRequest[RESPONSE] =>
      context.become(waiting(rqst.promise))
      executor ! rqst.request
  }

  private def waiting(implicit promise: Promise[RESPONSE]): Receive = {
    case OperationResult(Success(any)) if any.isInstanceOf[RESPONSE] => finish(Success(any.asInstanceOf[RESPONSE]))
    case OperationResult(Failure(e)) => finish(Failure(e))
    case other => finish(Failure(new IllegalArgumentException(s"Unexpected result $other")))
  }

  private def finish(result: Try[RESPONSE])
                    (implicit promise: Promise[RESPONSE]) = {
    context.become(ignorant)
    promise.complete(result)
    self ! PoisonPill
  }

  private def ignorant: Receive = {
    case _ =>
  }

  override def receive = initial
}

private[accounting] case class CompletableRequest[RESPONSE](request: Any, promise: Promise[RESPONSE])