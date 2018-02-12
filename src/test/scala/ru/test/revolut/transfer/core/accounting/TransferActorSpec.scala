package ru.test.revolut.transfer.core.accounting

import java.time.OffsetDateTime
import java.util.UUID

import akka.actor.{Props, Terminated}
import akka.testkit.{ImplicitSender, TestActorRef}
import org.scalatest.Matchers
import ru.test.revolut.transfer.core.accounting.AccountingProtocol.{DoTransaction, OperationResult}
import ru.test.revolut.transfer.core.accounting.TransferActor.DoTransfer
import ru.test.revolut.transfer.core.accounting.models.OperationTypes._
import ru.test.revolut.transfer.core.accounting.models._

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class TransferActorSpec extends AkkaSpec with Matchers with ImplicitSender {

  private val UsdAccount1 = AccountDescriptor("account1", "USD")
  private val UsdAccount2 = AccountDescriptor("account2", "USD")
  private val EurAccount = AccountDescriptor("account", "EUR")
  private val Amount = BigDecimal(42)
  private val TheTime = OffsetDateTime.now()
  private val Id = UUID.randomUUID()
  private val Comment = Some("comment")
  private val Transfer = TransferOperation(Id, Amount, TheTime, Comment)
  private val ExpectedLock = MaterialOperation(Id, Amount, Lock, TheTime, Comment)
  private val ExpectedRefill = MaterialOperation(Id, Amount, Refill, TheTime, Comment)
  private val ExpectedConfirm = ConfirmOperation(Id, TheTime)
  private val ExpectedUnlock = UnlockOperation(Id, TheTime)

  it should "abort transfer on equal account descriptors" in {
    val subj = newSubj
    subj ! DoTransfer(UsdAccount1, UsdAccount1, Transfer)
    receiveN(2, 1.second).toList should matchPattern {
      case OperationResult(Failure(_: TransferException)) :: Terminated(`subj`) :: Nil =>
    }
  }

  it should "abort transfer on unmatched account currencies" in {
    val subj = newSubj
    subj ! DoTransfer(UsdAccount1, EurAccount, Transfer)
    receiveN(2, 1.second).toList should matchPattern {
      case OperationResult(Failure(_: TransferException)) :: Terminated(`subj`) :: Nil =>
    }
  }

  it should "successfully complete transfer on successful lock, refill and confirm" in {
    val subj = newSubj
    subj ! DoTransfer(UsdAccount1, UsdAccount2, Transfer)
    receiveOne(1.second) should matchPattern {
      case DoTransaction(UsdAccount1, ExpectedLock) =>
    }
    subj ! OperationResult(Success(()))
    receiveOne(1.second) should matchPattern {
      case DoTransaction(UsdAccount2, ExpectedRefill) =>
    }
    subj ! OperationResult(Success(()))
    receiveOne(1.second) should matchPattern {
      case DoTransaction(UsdAccount1, ExpectedConfirm) =>
    }
    subj ! OperationResult(Success(()))
    receiveN(2, 1.second).toList should matchPattern {
      case OperationResult(Success(_)) :: Terminated(`subj`) :: Nil =>
    }
  }

  it should "unsuccessfully complete transfer on failed lock" in {
    val subj = newSubj
    subj ! DoTransfer(UsdAccount1, UsdAccount2, Transfer)
    receiveOne(1.second) should matchPattern {
      case DoTransaction(UsdAccount1, ExpectedLock) =>
    }
    subj ! OperationResult(Failure(new InsufficientFundsException(UsdAccount1)))
    receiveN(2, 1.second).toList should matchPattern {
      case OperationResult(Failure(_: InsufficientFundsException)) :: Terminated(`subj`) :: Nil =>
    }
  }

  it should "unsuccessfully complete transfer and redo lock on failed refill" in {
    val subj = newSubj
    subj ! DoTransfer(UsdAccount1, UsdAccount2, Transfer)
    receiveOne(1.second) should matchPattern {
      case DoTransaction(UsdAccount1, ExpectedLock) =>
    }
    subj ! OperationResult(Success(()))
    receiveOne(1.second) should matchPattern {
      case DoTransaction(UsdAccount2, ExpectedRefill) =>
    }
    subj ! OperationResult(Failure(new RuntimeException))
    receiveN(3, 1.second).toList should matchPattern {
      case DoTransaction(UsdAccount1, ExpectedUnlock) :: OperationResult(Failure(_: RuntimeException)) :: Terminated(`subj`) :: Nil =>
    }
  }

  it should "unsuccessfully complete transfer on failed confirm" in {
    val subj = newSubj
    subj ! DoTransfer(UsdAccount1, UsdAccount2, Transfer)
    receiveOne(1.second) should matchPattern {
      case DoTransaction(UsdAccount1, ExpectedLock) =>
    }
    subj ! OperationResult(Success(()))
    receiveOne(1.second) should matchPattern {
      case DoTransaction(UsdAccount2, ExpectedRefill) =>
    }
    subj ! OperationResult(Success(()))
    receiveOne(1.second) should matchPattern {
      case DoTransaction(UsdAccount1, ExpectedConfirm) =>
    }
    subj ! OperationResult(Failure(new RuntimeException))
    receiveN(2, 1.second).toList should matchPattern {
      case OperationResult(Failure(_: RuntimeException)) :: Terminated(`subj`) :: Nil =>
    }
  }

  private def newSubj = {
    val a = TestActorRef(Props(new TransferActor(self)))
    watch(a)
    a
  }
}
