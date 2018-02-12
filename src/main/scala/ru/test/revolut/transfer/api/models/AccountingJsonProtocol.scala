package ru.test.revolut.transfer.api.models

import ru.test.revolut.transfer.core.accounting.RequestId
import ru.test.revolut.transfer.core.accounting.models.Amount
import spray.json.{DefaultJsonProtocol, JsObject, JsString, JsValue, RootJsonFormat}

trait AccountingJsonProtocol extends DefaultJsonProtocol {
  implicit val descriptorFormat = jsonFormat2(AccountDescriptorView)
  implicit val singleAccountRequestFormat = jsonFormat3(SingleAccountRequest)
  implicit val doubleAccountRequestFormat = jsonFormat4(DoubleAccountsRequest)

  implicit val locksFormat = new RootJsonFormat[Map[RequestId, Amount]] {

    override def write(obj: Map[RequestId, Amount]) = {
      val formatted = obj.map { case (id, lock) =>
        val base = s"${lock.value.formatted("%.2f")} at ${lock.timeStamp.toString}"
        id.toString -> JsString(lock.comment.map(c => s"$base ($c)").getOrElse(base))
      }
      JsObject(formatted)
    }

    override def read(json: JsValue) = throw new NotImplementedError()
  }

}
