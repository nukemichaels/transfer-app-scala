package ru.test.revolut.transfer.core.accounting.models

import org.mockito.Mockito.{times, verify, when}
import org.mockito.{Matchers => MM}
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{FlatSpecLike, Matchers}
import ru.test.revolut.transfer.core.accounting.RequestId

import scala.util.{Failure, Success, Try}

class AccountSpec extends FlatSpecLike with Matchers {
  behavior of "Account"
  private val operation = mock[Operation]

  it should "return successful result on validator and operation success" in {
    implicit val validator = newValidator(Success(()))
    val subj = newSubj(Success(operation))
    val result = subj.applyOperation(operation)
    result should matchPattern {
      case Success(`subj`) =>
    }
    verify(validator, times(1)).validate(MM.eq(subj))(MM.eq(operation))
  }

  it should "return failed result on validator failure" in {
    implicit val validator = newValidator(Failure(new RuntimeException))
    val subj = newSubj(Success(operation))
    val result = subj.applyOperation(operation)
    result should matchPattern {
      case Failure(_: RuntimeException) =>
    }
    verify(validator, times(1)).validate(MM.eq(subj))(MM.eq(operation))
  }

  it should "return failed result on operation failure" in {
    implicit val validator = newValidator(Success(()))
    val subj = newSubj(Failure(new RuntimeException))
    val result = subj.applyOperation(operation)
    result should matchPattern {
      case Failure(_: RuntimeException) =>
    }
    verify(validator, times(1)).validate(MM.eq(subj))(MM.eq(operation))
  }

  private def newValidator(result: Try[Unit]) = {
    val m = mock[OperationValidator]
    when(m.validate(MM.anyObject())(MM.anyObject())).thenReturn(result)
    m
  }

  private def newSubj(operationResult: Try[Operation]) = new Account {
    def descriptor: AccountDescriptor = ???

    def locks: Map[RequestId, Amount] = ???

    def total: BigDecimal = ???

    protected def doApplyOperation(op: Operation): Try[Account] = operationResult.map(_ => this)

    def fixed: Set[Amount] = ???
  }
}
