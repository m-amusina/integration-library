package ru.evotor.framework.core.action.event.receipt.header

import android.os.Bundle

/**
 * TODO
 * Событие, которое смарт-терминал рассылает перед печатью чека покупки, продажи или возврата.
 * Обрабатывая это событие вы сможете разделить чек на несколько печатных групп ([ru.evotor.framework.receipt.PrintGroup]).
 *
 *
 * Пользователь каждый раз вручную выбирает приложение, которое обработает событие.
 *
 *
 * Для обработки события используется обработчик [PurchaserContactDataRequiredEventProcessor].
 * Обрабатывая событие приложение возвращает смарт-терминалу результат [PurchaserContactDataRequiredEventResult].
 *
 *
 * Константы {@value NAME_SELL_RECEIPT}, {@value NAME_PAYBACK_RECEIPT} и {@value NAME_BUY_RECEIPT} указывают тип чека, который будет разделён на печатные группы в результате обработки события.
 *
 *
 * Чтобы приложение получало событие, значение константы необходимо указать в элементе `action` intent-фильтра соотвествующей службы.
 *
 * @see ["Разделение чека на печатные группы"](https://developer.evotor.ru/docs/doc_java_receipt_printgroups_division.html)
 */
class PurchaserContactDataRequiredEvent : PurchaserContactDataEvent() {

    companion object {
        @JvmStatic
        fun create(bundle: Bundle?): PurchaserContactDataRequiredEvent? {
            return if (bundle == null) {
                null
            } else PurchaserContactDataRequiredEvent()
        }

        /**
         * TODO
         * Чек продажи разделён на несколько печатных групп.
         *
         * Значение константы: `evo.v2.receipt.sell.printGroup.REQUIRED`.
         */
        const val NAME_SELL_RECEIPT = "evo.v2.receipt.sell.header.PURCHASER_CONTACT_DATA_REQUIRED"

        /**
         * TODO
         * Чек покупки разделён на несколько печатных групп.
         *
         *
         * Значение константы: `evo.v2.receipt.buy.printGroup.REQUIRED`.
         */
        const val NAME_BUY_RECEIPT = "evo.v2.receipt.buy.header.PURCHASER_CONTACT_DATA_REQUIRED"

        /**
         * TODO
         * Чек возврата разделён на несколько печатных групп.
         *
         *
         * Значение константы: `evo.v2.receipt.payback.printGroup.REQUIRED`.
         */
        const val NAME_PAYBACK_RECEIPT =
            "evo.v2.receipt.payback.header.PURCHASER_CONTACT_DATA_REQUIRED"
    }
}
