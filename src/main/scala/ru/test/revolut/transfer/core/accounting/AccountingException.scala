package ru.test.revolut.transfer.core.accounting

import ru.test.revolut.transfer.core.accounting.models.AccountDescriptor

class AccountingException(msg: String, desc: AccountDescriptor*) extends RuntimeException(msg)

class UnknownAccountException(desc: AccountDescriptor)
  extends AccountingException(s"No [${desc.ccy}] account found for number: ${desc.number}", desc)

class AccountAlreadyExistException(desc: AccountDescriptor)
  extends AccountingException(s"Account already exist [${desc.ccy}] ${desc.number}", desc)

class InsufficientFundsException(desc: AccountDescriptor)
  extends AccountingException(s"Unable to perform operation due to insufficient funds", desc)

class TransferException(msg: String, sourceDesc: AccountDescriptor, destDesc: AccountDescriptor)
  extends AccountingException(msg, sourceDesc, destDesc)