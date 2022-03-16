package ru.evotor.framework.core.action.event.receipt.header

import ru.evotor.framework.core.action.event.receipt.header.PurchaserContactDataRequiredEvent.Companion.create
import ru.evotor.framework.core.action.processor.ActionProcessor
import kotlin.Throws
import android.os.RemoteException
import android.os.Bundle
import ru.evotor.framework.core.action.event.receipt.header.PurchaserContactDataRequiredEvent

/**
 * TODO
 * Обработчик события [PurchaserContactDataRequiredEvent].
 *
 * @see ["Разделение чека на печатные группы"](https://developer.evotor.ru/docs/doc_java_receipt_printgroups_division.html)
 */
abstract class PurchaserContactDataRequiredEventProcessor : ActionProcessor() {
    @Throws(RemoteException::class)
    override fun process(action: String, bundle: Bundle?, callback: Callback) {
        val event = create(bundle)
        if (event == null) {
            callback.skip()
            return
        }
        call(
            action,
            event,
            callback
        )
    }

    /**
     * TODO
     * Используйте метод, чтобы обработать событие [PurchaserContactDataRequiredEvent] и сохранить результат [PurchaserContactDataRequiredEventResult].
     *
     * @param action
     * @param event    экземпляр события разделения чека на печатные группы.
     * @param callback функция обратного вызова. Методы функции позволяют пропускать обработку события, возвращать результат,
     * запускать операции и обрабатывать ошибки.
     */
    abstract fun call(action: String, event: PurchaserContactDataRequiredEvent, callback: Callback)
}