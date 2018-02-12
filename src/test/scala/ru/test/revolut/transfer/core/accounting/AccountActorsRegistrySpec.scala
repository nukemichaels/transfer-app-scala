package ru.test.revolut.transfer.core.accounting

import akka.actor.ActorRef
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar._
import ru.test.revolut.transfer.core.accounting.models.{Account, AccountDescriptor, AccountRegistrySetup, OperationValidator}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Random}

class AccountActorsRegistrySpec extends AkkaSpec with Matchers with ScalaFutures {
  private val Validator = mock[OperationValidator]
  private val TheAccount = mock[Account]
  private val AccountGen: AccountDescriptor => Account = _ => TheAccount
  private val FaultyAccountGen: AccountDescriptor => Account = _ => throw new RuntimeException

  "findAccount" should "return None if no account registered for descriptor" in {
    val subj = newSubj(AccountGen)
    val descriptor = newDescriptor
    whenReady(subj.findAccount(descriptor)) { r =>
      r should matchPattern {
        case None =>
      }
    }
  }

  "addAccount" should "add new account on successful account generation" in {
    val subj = newSubj(AccountGen)
    val descriptor = newDescriptor
    whenReady(subj.addAccount(descriptor)) { r =>
      r should not be null
    }
  }

  it should "fail on unsuccessful account generation" in {
    val subj = newSubj(FaultyAccountGen)
    val descriptor = newDescriptor
    Await.ready(subj.addAccount(descriptor), 1.second).value should matchPattern {
      case Some(Failure(_: RuntimeException)) =>
    }
  }

  it should "fail on illegal account actor name generation" in {
    val subj = newSubj(AccountGen)
    val descriptor = AccountDescriptor("/123/asd.", "USD")
    Await.ready(subj.addAccount(descriptor), 1.second).value should matchPattern {
      case Some(Failure(_: RuntimeException)) =>
    }
  }

  "findAccount + addAccount" should "return actor after account creation" in {
    val subj = newSubj(AccountGen)
    val descriptor = newDescriptor
    val fut = for {
      noAccount <- subj.findAccount(descriptor)
      createdAccount <- subj.addAccount(descriptor)
      theAccount <- subj.findAccount(descriptor)
    } yield (noAccount, createdAccount, theAccount)
    whenReady(fut) { case (noAccount, createdAccount, theAccount) =>
      noAccount should be(None)
      createdAccount should matchPattern {
        case _: ActorRef =>
      }
      theAccount should matchPattern {
        case Some(`createdAccount`) =>
      }
    }
  }

  private def newSubj(gen: AccountDescriptor => Account) =
    new AccountActorsRegistry(system, AccountRegistrySetup(None, Validator, gen))

  private def newDescriptor = AccountDescriptor(Random.nextLong().toString, "USD")
}
