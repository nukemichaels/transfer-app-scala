package ru.test.revolut.transfer.api

import akka.http.scaladsl.model._
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import ru.test.revolut.transfer.core.accounting.models.AccountDescriptor

class AccountManagementSpec extends ApiSpec with Matchers with ScalaFutures {

  import AccountingController._

  private val route = controller.account

  "/account" should "create new account with PUT request" in {
    val account = AccountDescriptor(nextAccount, "USD")
    newAccountRequest(account.number, account.ccy) ~> route ~> check {
      responseAs[String] shouldBe "account created"
      whenReady(accounting.total(account)) { total =>
        total should be(BigDecimal(0))
      }
    }
  }

  it should "return BadRequest on already existing account creation" in {
    val account = AccountDescriptor(nextAccount, "USD")
    whenReady(accounting.newAccount(account)) { _ =>
      newAccountRequest(account.number, account.ccy) ~> route ~> check {
        response.status should be(StatusCodes.BadRequest)
      }
    }
  }

  "/account/total" should "return total value from existing account" in {
    val account = AccountDescriptor(nextAccount, "USD")
    val amount = BigDecimal(50)
    val setup = for {
      _ <- accounting.newAccount(account)
      _ <- accounting.refill(account, amount, None)
    } yield ()
    whenReady(setup) { _ =>
      Get(s"/account/total?ccy=${account.ccy}&number=${account.number}") ~> route ~> check {
        responseAs[String] shouldBe s"${amount.formatted("%.2f")}(${account.ccy})"
      }
    }
  }

  it should "return BadRequest on unknown account request for total" in {
    Get(s"/account/total?ccy=USD&number=$nextAccount") ~> route ~> check {
      response.status should be(StatusCodes.BadRequest)
    }
  }

  "/account/locks" should "return locks value from existing account" in {
    val account = AccountDescriptor(nextAccount, "USD")
    whenReady(accounting.newAccount(account)) { _ =>
      Get(s"/account/locks?ccy=${account.ccy}&number=${account.number}") ~> route ~> check {
        responseAs[String] shouldBe "{}"
      }
    }
  }

  it should "return BadRequest on unknown account request for locks" in {
    Get(s"/account/total?ccy=USD&number=$nextAccount") ~> route ~> check {
      response.status should be(StatusCodes.BadRequest)
    }
  }

}
