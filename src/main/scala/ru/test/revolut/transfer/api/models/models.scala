package ru.test.revolut.transfer.api.models

import ru.test.revolut.transfer.core.accounting.models.AccountDescriptor

case class AccountDescriptorView(number: String, ccy: String) {
  def asModel = AccountDescriptor.tupled(AccountDescriptorView.unapply(this).get)
}

case class SingleAccountRequest(account: AccountDescriptorView, amount: BigDecimal, comment: Option[String])

case class DoubleAccountsRequest(source: AccountDescriptorView,
                                 destination: AccountDescriptorView,
                                 amount: BigDecimal,
                                 comment: Option[String])
