package ru.evotor.framework.core.action.event.receipt.header

import android.os.Bundle
import ru.evotor.IBundlable
import ru.evotor.framework.kkt.FiscalTags

/**
 * Родительский класс события запроса контактных данных клиента для отправки ему чека в электронной форме [PurchaserContactDataRequiredEvent].
 *
 * Данные будут записаны в тег [FiscalTags.PURCHASER_PHONE_OR_EMAIL]
 */
abstract class PurchaserContactDataEvent : IBundlable {
    override fun toBundle(): Bundle {
        return Bundle()
    }
}
