package ru.evotor.framework.core.action.event.receipt.header

import android.os.Bundle
import ru.evotor.IBundlable
import ru.evotor.framework.core.action.event.receipt.changes.position.SetExtra
import ru.evotor.framework.core.action.event.receipt.changes.position.SetExtra.Companion.from
import ru.evotor.framework.core.action.event.receipt.changes.position.SetPrintGroup

/**
 * TODO
 * Результат обработки события [PurchaserContactDataRequiredEvent].
 */
class PurchaserContactDataRequiredEventResult(
    /**
     * Возвращает дополнителье поля чека, которые были указаны при создании результата.
     * @return список объектов [SetExtra].
     */
    val extra: SetExtra?,
    /**
     * Возвращает список печатных групп, указанных при создании результата.
     * @return список объектов [SetPrintGroup].
     */
    val contacts: List<ContactData>
) : IBundlable {
    override fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putBundle(KEY_RECEIPT_EXTRA, extra?.toBundle())
        bundle.putParcelableArray(KEY_CONTACTS, contacts.map { it.toBundle() }.toTypedArray())
        return bundle
    }

    data class ContactData(
        val type: Type,
        val value: String?
    ) : IBundlable {

        override fun toBundle(): Bundle {
            return Bundle().apply {
                putInt(KEY_TYPE, type.ordinal)
                putString(KEY_VALUE, value)
            }
        }

        companion object {
            private const val KEY_TYPE = "TYPE"
            private const val KEY_VALUE = "VALUE"

            fun fromBundle(bundle: Bundle?): ContactData? {
                bundle ?: return null
                return ContactData(
                    type = restoreType(
                        bundle.getInt(KEY_TYPE, -1)
                    ),
                    value = bundle.getString(KEY_VALUE, null)
                )
            }

            private fun restoreType(ordinal: Int): Type {
                val types = Type.values()
                return if (ordinal >= 0 && ordinal < types.size) {
                    types[ordinal]
                } else {
                    Type.EMAIL
                }
            }

            enum class Type {
                EMAIL,
                PHONE
            }
        }
    }

    companion object {
        private const val KEY_RECEIPT_EXTRA = "extra"
        private const val KEY_CONTACTS = "contacts"
        fun create(bundle: Bundle?): PurchaserContactDataRequiredEventResult? {
            return if (bundle == null) {
                null
            } else PurchaserContactDataRequiredEventResult(
                from(bundle.getBundle(KEY_RECEIPT_EXTRA)),
                bundle.getParcelableArray(KEY_CONTACTS)
                    ?.asSequence()
                    ?.map { parcelable ->
                        if (parcelable is Bundle) {
                            ContactData.fromBundle(parcelable)
                        } else {
                            null
                        }
                    }
                    ?.filterNotNull()
                    ?.toList()
                    ?: emptyList()
            )
        }
    }
}
