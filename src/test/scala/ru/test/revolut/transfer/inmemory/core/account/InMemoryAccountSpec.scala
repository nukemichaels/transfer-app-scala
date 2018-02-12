package ru.test.revolut.transfer.inmemory.core.account

import java.time.OffsetDateTime
import java.util.UUID

import org.mockito.Mockito.when
import org.mockito.{Matchers => MM}
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{FlatSpecLike, Matchers}
import ru.test.revolut.transfer.core.accounting.models.OperationTypes._
import ru.test.revolut.transfer.core.accounting.models._

import scala.util.Success

class InMemoryAccountSpec extends FlatSpecLike with Matchers {
  private val InitialBalance = BigDecimal(42)
  private val Descriptor = AccountDescriptor("account", "USD")
  private val DeltaAmount = BigDecimal(10)
  private val TheTime = OffsetDateTime.now()
  implicit private val alwaysOkValidator = {
    val m = mock[OperationValidator]
    when(m.validate(MM.any())(MM.any())).thenReturn(Success(()))
    m
  }

  "for refill" should "add positive amount to fixed category" in {
    val subj = InMemoryAccount(InitialBalance, Descriptor)
    val result = subj.applyOperation(MaterialOperation(UUID.randomUUID(), DeltaAmount, Refill, TheTime))
    result match {
      case Success(InMemoryAccount(InitialBalance, Descriptor, locks, fixed)) =>
        locks shouldBe empty
        fixed should contain only Amount(DeltaAmount, TheTime, None)
      case other => fail(s"Invalid account state: $other")
    }
  }

  "for withdrawal" should "add negative amount to fixed category" in {
    val subj = InMemoryAccount(InitialBalance, Descriptor)
    val result = subj.applyOperation(MaterialOperation(UUID.randomUUID(), DeltaAmount, Withdrawal, TheTime))
    result match {
      case Success(InMemoryAccount(InitialBalance, Descriptor, locks, fixed)) =>
        locks shouldBe empty
        fixed should contain only Amount(DeltaAmount * -1, TheTime, None)
      case other => fail(s"Invalid account state: $other")
    }
  }

  "for lock" should "add negative amount to locked category" in {
    val id = UUID.randomUUID()
    val subj = InMemoryAccount(InitialBalance, Descriptor)
    val result = subj.applyOperation(MaterialOperation(id, DeltaAmount, Lock, TheTime))
    result match {
      case Success(InMemoryAccount(InitialBalance, Descriptor, locks, fixed)) =>
        locks should contain only id -> Amount(DeltaAmount * -1, TheTime, None)
        fixed shouldBe empty
      case other => fail(s"Invalid account state: $other")
    }
  }

  it should "remove amounts from locked category on unlock" in {
    val id = UUID.randomUUID()
    val subj = InMemoryAccount(InitialBalance, Descriptor, Map(id -> Amount(DeltaAmount * -1, TheTime, None)))
    val result = subj.applyOperation(UnlockOperation(id, TheTime))
    result match {
      case Success(InMemoryAccount(InitialBalance, Descriptor, locks, fixed)) =>
        locks shouldBe empty
        fixed shouldBe empty
      case other => fail(s"Invalid account state: $other")
    }
  }

  it should "move amounts from locked category to fixed on confirm" in {
    val id = UUID.randomUUID()
    val theAmount = Amount(DeltaAmount * -1, TheTime, None)
    val subj = InMemoryAccount(InitialBalance, Descriptor, Map(id -> theAmount))
    val result = subj.applyOperation(ConfirmOperation(id, TheTime))
    result match {
      case Success(InMemoryAccount(InitialBalance, Descriptor, locks, fixed)) =>
        locks shouldBe empty
        fixed should contain only theAmount
      case other => fail(s"Invalid account state: $other")
    }
  }

  "for total" should "return account total value" in {
    val lock1 = DeltaAmount * -1
    val lock2 = DeltaAmount * -2
    val fix1 = DeltaAmount * 30
    val fix2 = DeltaAmount * 10
    val locks = Map(
      UUID.randomUUID() -> Amount(lock1, TheTime, None),
      UUID.randomUUID() -> Amount(lock2, TheTime, None)
    )
    val fix = Set(Amount(fix1, TheTime, None), Amount(fix2, TheTime, None))
    val subj = InMemoryAccount(InitialBalance, Descriptor, locks, fix)
    subj.total should be(InitialBalance + lock1 + lock2 + fix1 + fix2)
  }

}
