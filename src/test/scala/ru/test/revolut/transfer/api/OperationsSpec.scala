package ru.test.revolut.transfer.api

import akka.http.scaladsl.model._
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import ru.test.revolut.transfer.core.accounting.models.AccountDescriptor

class OperationsSpec extends ApiSpec with Matchers with ScalaFutures {

  import AccountingController._

  private val route = controller.operations

  "/do/withdrawal" should "withdrawal amount from existing account with POST request" in {
    val account = AccountDescriptor(nextAccount, "USD")
    val initial = BigDecimal(100)
    val requesting = BigDecimal(35)
    val setup = for {
      _ <- accounting.newAccount(account)
      _ <- accounting.refill(account, initial)
    } yield ()
    whenReady(setup) { _ =>
      newOperation(requesting, "withdrawal", account) ~> route ~> check {
        responseAs[String] shouldBe "complete"
        whenReady(accounting.total(account)) { total =>
          total should be(initial - requesting)
        }
      }
    }
  }

  it should "return BadRequest on unknown account withdrawal" in {
    val account = AccountDescriptor(nextAccount, "USD")
    newOperation(42, "withdrawal", account) ~> route ~> check {
      response.status should be(StatusCodes.BadRequest)
    }
  }

  "/do/refill" should "refill amount to existing account with POST request" in {
    val account = AccountDescriptor(nextAccount, "USD")
    val requesting = BigDecimal(40)
    whenReady(accounting.newAccount(account)) { _ =>
      newOperation(requesting, "refill", account) ~> route ~> check {
        responseAs[String] shouldBe "complete"
        whenReady(accounting.total(account)) { total =>
          total should be(requesting)
        }
      }
    }
  }

  it should "return BadRequest on unknown account refill" in {
    val account = AccountDescriptor(nextAccount, "USD")
    newOperation(42, "refill", account) ~> route ~> check {
      response.status should be(StatusCodes.BadRequest)
    }
  }

  "/do/transfer" should "transfer amount between existing accounts with POST request" in {
    val account1 = AccountDescriptor(nextAccount, "USD")
    val account2 = AccountDescriptor(nextAccount, "USD")
    val initial = BigDecimal(100)
    val requesting = BigDecimal(35)
    val setup = for {
      _ <- accounting.newAccount(account1)
      _ <- accounting.newAccount(account2)
      _ <- accounting.refill(account1, initial)
    } yield ()
    whenReady(setup) { _ =>
      newOperation(requesting, "transfer", account1, Some(account2)) ~> route ~> check {
        responseAs[String] shouldBe "complete"
        val result = for {
          total1 <- accounting.total(account1)
          total2 <- accounting.total(account2)
        } yield (total1, total2)
        whenReady(result) { case (total1, total2) =>
          total1 should be(initial - requesting)
          total2 should be(requesting)
        }
      }
    }
  }

  it should "return BadRequest for transfer attempt on accounts with unmatched currency" in {
    val account1 = AccountDescriptor(nextAccount, "USD")
    val account2 = AccountDescriptor(nextAccount, "EUR")
    val initial = BigDecimal(100)
    val requesting = BigDecimal(35)
    val setup = for {
      _ <- accounting.newAccount(account1)
      _ <- accounting.newAccount(account2)
      _ <- accounting.refill(account1, initial)
    } yield ()
    whenReady(setup) { _ =>
      newOperation(requesting, "transfer", account1, Some(account2)) ~> route ~> check {
        response.status should be(StatusCodes.BadRequest)
        val result = for {
          total1 <- accounting.total(account1)
          total2 <- accounting.total(account2)
        } yield (total1, total2)
        whenReady(result) { case (total1, total2) =>
          total1 should be(initial)
          total2 should be(BigDecimal(0))
        }
      }
    }
  }

  it should "return BadRequest for transfer attempt on unknown target account" in {
    val account1 = AccountDescriptor(nextAccount, "USD")
    val account2 = AccountDescriptor(nextAccount, "USD")
    val initial = BigDecimal(100)
    val requesting = BigDecimal(35)
    val setup = for {
      _ <- accounting.newAccount(account1)
      _ <- accounting.refill(account1, initial)
    } yield ()
    whenReady(setup) { _ =>
      newOperation(requesting, "transfer", account1, Some(account2)) ~> route ~> check {
        response.status should be(StatusCodes.BadRequest)
        whenReady(accounting.total(account1)) { total1 =>
          total1 should be(initial)
        }
      }
    }
  }

  it should "return BadRequest for transfer attempt on unknown source account" in {
    val account1 = AccountDescriptor(nextAccount, "USD")
    val account2 = AccountDescriptor(nextAccount, "USD")
    val requesting = BigDecimal(35)
    newOperation(requesting, "transfer", account1, Some(account2)) ~> route ~> check {
      response.status should be(StatusCodes.BadRequest)
    }
  }

  it should "return BadRequest for transfer attempt on account with unsufficient funds" in {
    val account1 = AccountDescriptor(nextAccount, "USD")
    val account2 = AccountDescriptor(nextAccount, "USD")
    val initial = BigDecimal(100)
    val requesting = BigDecimal(350)
    val setup = for {
      _ <- accounting.newAccount(account1)
      _ <- accounting.newAccount(account2)
      _ <- accounting.refill(account1, initial)
    } yield ()
    whenReady(setup) { _ =>
      newOperation(requesting, "transfer", account1, Some(account2)) ~> route ~> check {
        response.status should be(StatusCodes.BadRequest)
        val result = for {
          total1 <- accounting.total(account1)
          total2 <- accounting.total(account2)
        } yield (total1, total2)
        whenReady(result) { case (total1, total2) =>
          total1 should be(initial)
          total2 should be(BigDecimal(0))
        }
      }
    }
  }
}
