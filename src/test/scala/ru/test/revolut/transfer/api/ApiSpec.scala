package ru.test.revolut.transfer.api

import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest, MediaTypes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.FlatSpecLike
import ru.test.revolut.transfer.core.accounting.models.{AccountDescriptor, AccountRegistrySetup}
import ru.test.revolut.transfer.core.accounting.{AccountActorsRegistry, Accounting}
import ru.test.revolut.transfer.inmemory.core.account.{InMemoryAccount, SimpleEventsListener, SimpleOperationValidator}

import scala.util.Random

trait ApiSpec extends FlatSpecLike with ScalatestRouteTest {
  private val setup = AccountRegistrySetup(Some(SimpleEventsListener), SimpleOperationValidator, InMemoryAccount.generate)
  protected val accounting = Accounting(AccountActorsRegistry(setup))
  protected val controller = AccountingController(accounting)
  protected val Comment = "commentary"

  protected def newAccountRequest(number: String, ccy: String) =
    HttpRequest(
      HttpMethods.PUT,
      uri = "/account",
      entity = HttpEntity(MediaTypes.`application/json`, s"""{"number": "$number", "ccy": "$ccy"}""")
    )

  protected def newOperation(amount: BigDecimal,
                             operation: String,
                             source: AccountDescriptor,
                             dest: Option[AccountDescriptor] = None) = {
    val accountToJson = (a: AccountDescriptor) => s"""{"number": "${a.number}", "ccy": "${a.ccy}"}"""
    val desc = dest.map { d =>
      s""""source": ${accountToJson(source)}, "destination": ${accountToJson(d)}"""
    }.getOrElse(s""" "account": ${accountToJson(source)}""")
    val body = s"""{ "amount": $amount, "comment": "$Comment", $desc }"""
    HttpRequest(
      HttpMethods.POST,
      uri = s"/do/$operation",
      entity = HttpEntity(MediaTypes.`application/json`, body)
    )
  }

  protected def nextAccount = Random.nextLong().toString

}
