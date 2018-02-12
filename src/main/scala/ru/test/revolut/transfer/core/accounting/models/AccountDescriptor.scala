package ru.test.revolut.transfer.core.accounting.models

import ru.test.revolut.transfer.core.accounting.{AccountNumber, Ccy}

/**
  * Account metadata
  *
  * @param number account number
  * @param ccy    account currency
  */
case class AccountDescriptor(number: AccountNumber, ccy: Ccy)