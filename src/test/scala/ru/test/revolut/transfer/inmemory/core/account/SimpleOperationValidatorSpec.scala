package ru.test.revolut.transfer.inmemory.core.account

import java.util.UUID

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{FlatSpecLike, Matchers}
import ru.test.revolut.transfer.core.accounting.InsufficientFundsException
import ru.test.revolut.transfer.core.accounting.models.{Account, MaterialOperation, OperationTypes}

import scala.util.{Failure, Success}

class SimpleOperationValidatorSpec extends FlatSpecLike with Matchers {
  private val Operation = MaterialOperation(UUID.randomUUID(), 42, OperationTypes.Withdrawal)

  private def newAccount(total: BigDecimal) = {
    val m = mock[Account]
    when(m.total).thenReturn(total)
    m
  }

  it should "return failure on insufficient account funds" in {
    SimpleOperationValidator
      .validate(newAccount(Operation.amount - 1))(Operation) should matchPattern {
      case Failure(_: InsufficientFundsException) =>
    }
  }

  it should "return success on account funds exceeding operation" in {
    SimpleOperationValidator
      .validate(newAccount(Operation.amount + 10))(Operation) should be(Success(()))
  }
}
