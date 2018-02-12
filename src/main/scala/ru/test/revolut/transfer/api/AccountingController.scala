package ru.test.revolut.transfer.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer
import ru.test.revolut.transfer.api.models._
import ru.test.revolut.transfer.core.accounting.models.AccountDescriptor
import ru.test.revolut.transfer.core.accounting.{Accounting, AccountingException}

class AccountingController(system: ActorSystem,
                           accounting: Accounting) extends SprayJsonSupport with AccountingJsonProtocol {

  import AccountingController._

  implicit private val sys = system
  implicit private val materializer = ActorMaterializer()
  implicit private val ec = system.dispatcher

  private val ping = pathPrefix("ping") {
    get {
      complete("working!")
    }
  }

  private[api] val account = pathPrefix("account") {
    get {
      path("total") {
        parameter("ccy", "number") { (ccy, number) =>
          complete {
            accounting.total(AccountDescriptor(number, ccy)).map(b => s"${formatAmount(b)}($ccy)")
          }
        }
      } ~
        path("locks") {
          parameter("ccy", "number") { (ccy, number) =>
            complete(accounting.locks(AccountDescriptor(number, ccy)))
          }
        }
    } ~ put {
      entity(as[AccountDescriptorView]) { view =>
        complete {
          for {
            _ <- accounting.newAccount(view.asModel)
          } yield "account created"
        }
      }
    }
  }
  private[api] val operations = pathPrefix("do") {
    post {
      path("withdrawal") {
        entity(as[SingleAccountRequest]) { r =>
          complete {
            for {
              _ <- accounting.withdrawal(r.account.asModel, r.amount, r.comment)
            } yield "complete"
          }
        }
      } ~
        path("refill") {
          entity(as[SingleAccountRequest]) { r =>
            complete {
              for {
                _ <- accounting.refill(r.account.asModel, r.amount, r.comment)
              } yield "complete"
            }
          }
        } ~
        path("transfer") {
          entity(as[DoubleAccountsRequest]) { r =>
            complete {
              for {
                _ <- accounting.transfer(r.source.asModel, r.destination.asModel, r.amount, r.comment)
              } yield "complete"
            }
          }
        }
    }
  }

  private[api] val route = ping ~ account ~ operations

  def start(host: String, port: Int) = {
    Http().bindAndHandle(route, host, port)
  }

}

object AccountingController {
  def apply(accounting: Accounting)
           (implicit system: ActorSystem): AccountingController = new AccountingController(system, accounting)

  implicit def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case e: AccountingException =>
        complete(HttpResponse(StatusCodes.BadRequest, entity = e.getMessage))
    }

  private def formatAmount(amount: BigDecimal) = amount.formatted("%.2f")
}