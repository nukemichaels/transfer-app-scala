package ru.test.revolut.transfer.core.accounting

import java.time.OffsetDateTime
import java.util.UUID

import akka.actor.{ActorRef, ActorRefFactory, Props}
import ru.test.revolut.transfer.core.accounting.AccountingProtocol._
import ru.test.revolut.transfer.core.accounting.TransferActor.DoTransfer
import ru.test.revolut.transfer.core.accounting.models.OperationTypes._
import ru.test.revolut.transfer.core.accounting.models._

import scala.concurrent.{Future, Promise}

/**
  * Client facade for accounting operations.
  *
  * @param operationsDispatcher Reference to the instance of dispatching actor { @see DispatchingActor}
  * @param actorRefFactory ActorRefFactory instance
  */
final class Accounting(operationsDispatcher: ActorRef,
                       actorRefFactory: ActorRefFactory) {

  import Accounting._

  /**
    * Generates new account for given descriptor
    */
  def newAccount(desc: AccountDescriptor): Future[Unit] = oneShot(NewAccount(desc))

  /**
    * Execute withdrawal operation on target account.
    * Would return <b>InsufficientFundsException<b/> if requested amount exceeding current balance.
    *
    * @param desc    target account descriptor
    * @param amount  withdrawal amount
    * @param comment optional commentary
    */
  def withdrawal(desc: AccountDescriptor, amount: BigDecimal, comment: Option[String]): Future[Unit] =
    oneShot[Unit](
      DoTransaction(
        desc,
        MaterialOperation(nextUUID, amount, Withdrawal, OffsetDateTime.now(), comment)
      )
    )

  /**
    * Execute refill operation on target account
    *
    * @param desc    target account descriptor
    * @param amount  refill amount
    * @param comment optional commentary
    */
  def refill(desc: AccountDescriptor, amount: BigDecimal, comment: Option[String] = None): Future[Unit] =
    oneShot[Unit](
      DoTransaction(
        desc,
        MaterialOperation(nextUUID, amount, Refill, OffsetDateTime.now(), comment)
      )
    )

  /**
    * Execute money transfer from one account to another.
    * Transfer executed in three steps: amount lock on source account, amount refill on destination,
    * lock confirmation in case of successful refill.
    * If refill operation was'n successful, locked would be removed.
    * Would return <b>InsufficientFundsException</b> in transfer amount exceed available balance of source account.
    *
    * @param from    Source account
    * @param to      Destination account
    * @param amount  transfer amount
    * @param comment optional commentary
    */
  def transfer(from: AccountDescriptor,
               to: AccountDescriptor,
               amount: BigDecimal,
               comment: Option[String]): Future[Unit] = {
    val a = actorRefFactory.actorOf(Props(new TransferActor(operationsDispatcher)))
    val transfer = TransferOperation(nextUUID, amount, OffsetDateTime.now(), comment)
    oneShot(DoTransfer(from, to, transfer), a)
  }

  /**
    * @param desc account descriptor to calculate total balance amount
    * @return total account balance for given descriptor
    */
  def total(desc: AccountDescriptor): Future[BigDecimal] =
    oneShot[BigDecimal](DoHousekeeping(desc, GetTotal))

  /**
    * @param desc account descriptor
    * @return locks registered for given account
    */
  def locks(desc: AccountDescriptor): Future[Map[RequestId, Amount]] =
    oneShot[Map[RequestId, Amount]](DoHousekeeping(desc, GetLocks))

  private def oneShot[T](request: Any, target: ActorRef = operationsDispatcher) = {
    val p = Promise[T]()
    val a = actorRefFactory.actorOf(Props(new OneShotActor[T](target)))
    a ! CompletableRequest(request, p)
    p.future
  }

}

object Accounting {

  def apply(accountsRegistry: AccountsRegistry)
           (implicit actorRefFactory: ActorRefFactory): Accounting =
    new Accounting(
      actorRefFactory.actorOf(Props(new DispatchingActor(accountsRegistry))),
      actorRefFactory
    )

  private def nextUUID = UUID.randomUUID()
}