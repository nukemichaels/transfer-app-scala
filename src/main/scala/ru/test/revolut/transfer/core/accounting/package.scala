package ru.test.revolut.transfer.core

import java.util.UUID

import ru.test.revolut.transfer.core.accounting.models.OperationTypes._
import ru.test.revolut.transfer.core.accounting.models.{AccountDescriptor, Amount, MaterialOperation, Operation}

package object accounting {
  type AccountNumber = String
  type Ccy = String
  type RequestId = UUID

  implicit class OperationTypeHelper(opType: OperationType) {
    private val materialOperations = Set(Withdrawal, Refill, Lock)
    private val completeOperations = Set(Withdrawal, Refill, Complete)
    private val compoundOperations = Set(Transfer)

    def isMaterial: Boolean = materialOperations.contains(opType)

    def isComplete: Boolean = completeOperations.contains(opType)

    def isCompound: Boolean = compoundOperations.contains(opType)

    def signum: Int = opType match {
      case Withdrawal | Complete | Lock => -1
      case _ => 1
    }
  }

  implicit class AmountGen(operation: Operation) {
    def toAmount = operation match {
      case op: MaterialOperation => Amount(op.operationType.signum * op.amount, op.timeStamp, op.comment)
      case _ => throw new IllegalArgumentException("Amount might be generated only for material operations")
    }

    def toAmountOpt = operation match {
      case op: MaterialOperation => Some(Amount(op.operationType.signum * op.amount, op.timeStamp, op.comment))
      case _ => None
    }
  }

  implicit class ActorPathSupport(desc: AccountDescriptor) {
    def toActorPath: String = s"${desc.ccy}-${desc.number}-actor"
  }

}
