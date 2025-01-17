package ru.evotor.framework.receipt

import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.annotation.WorkerThread
import org.json.JSONArray
import ru.evotor.framework.*
import ru.evotor.framework.component.PaymentPerformer
import ru.evotor.framework.component.PaymentPerformerTable
import ru.evotor.framework.inventory.AttributeValue
import ru.evotor.framework.inventory.ProductType
import ru.evotor.framework.payment.PaymentSystem
import ru.evotor.framework.payment.PaymentSystemTable
import ru.evotor.framework.payment.PaymentType
import ru.evotor.framework.receipt.ReceiptDiscountTable.DISCOUNT_COLUMN_NAME
import ru.evotor.framework.receipt.ReceiptDiscountTable.POSITION_DISCOUNT_UUID_COLUMN_NAME
import ru.evotor.framework.receipt.mapper.FiscalReceiptMapper
import ru.evotor.framework.receipt.position.ImportationData
import ru.evotor.framework.receipt.position.Mark
import ru.evotor.framework.receipt.position.PreferentialMedicine
import ru.evotor.framework.receipt.position.mapper.AgentRequisitesMapper
import ru.evotor.framework.receipt.position.mapper.PositionPartialRealizationMapper
import ru.evotor.framework.receipt.position.mapper.PreferentialMedicineMapper
import ru.evotor.framework.receipt.position.mapper.SettlementMethodMapper
import ru.evotor.framework.receipt.provider.FiscalReceiptContract
import java.math.BigDecimal
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@WorkerThread
object ReceiptApi {

    @Deprecated(message = "Используйте методы API")
    const val AUTHORITY = "ru.evotor.evotorpos.receipt"

    @Deprecated(message = "Используйте методы API. Данная константа будет удалена")
    @JvmField
    val BASE_URI = Uri.parse("content://$AUTHORITY")

    private const val AUTHORITY_V2 = "ru.evotor.evotorpos.v2.receipt"
    private const val RECEIPTS_PATH = "receipts"
    private const val CURRENT_SELL_PATH = "sell"
    private const val CURRENT_PAYBACK_PATH = "payback"
    private const val CURRENT_BUY_PATH = "buy"
    private const val CURRENT_BUYBACK_PATH = "buyback"
    private const val POSITIONS_PATH = "positions"
    private const val PAYMENTS_PATH = "payments"
    private const val DISCOUNTS_PATH = "discounts"
    private const val CURRENT_CORRECTION_INCOME_PATH = "correctionIncome"
    private const val CURRENT_CORRECTION_OUTCOME_PATH = "correctionOutcome"
    private const val CURRENT_CORRECTION_RETURN_INCOME_PATH = "correctionReturnIncome"
    private const val CURRENT_CORRECTION_RETURN_OUTCOME_PATH = "correctionReturnOutcome"

    private val BASE_URI_V2 = Uri.parse("content://$AUTHORITY_V2")
    private val RECEIPTS_URI = Uri.withAppendedPath(BASE_URI_V2, RECEIPTS_PATH)
    private val CURRENT_SELL_RECEIPT_URI = Uri.withAppendedPath(BASE_URI_V2, CURRENT_SELL_PATH)
    private val CURRENT_PAYBACK_RECEIPT_URI = Uri.withAppendedPath(BASE_URI_V2, CURRENT_PAYBACK_PATH)
    private val CURRENT_BUY_RECEIPT_URI = Uri.withAppendedPath(BASE_URI_V2, CURRENT_BUY_PATH)
    private val CURRENT_BUYBACK_RECEIPT_URI = Uri.withAppendedPath(BASE_URI_V2, CURRENT_BUYBACK_PATH)
    private val CURRENT_CORRECTION_INCOME_RECEIPT_URI = Uri.withAppendedPath(BASE_URI_V2, CURRENT_CORRECTION_INCOME_PATH)
    private val CURRENT_CORRECTION_OUTCOME_RECEIPT_URI = Uri.withAppendedPath(BASE_URI_V2, CURRENT_CORRECTION_OUTCOME_PATH)
    private val CURRENT_CORRECTION_RETURN_INCOME_RECEIPT_URI = Uri.withAppendedPath(BASE_URI_V2, CURRENT_CORRECTION_RETURN_INCOME_PATH)
    private val CURRENT_CORRECTION_RETURN_OUTCOME_RECEIPT_URI = Uri.withAppendedPath(BASE_URI_V2, CURRENT_CORRECTION_RETURN_OUTCOME_PATH)


    @JvmStatic
    fun getPositionsByBarcode(context: Context, barcode: String): List<Position> {
        val positionsList = ArrayList<Position>()

        context.contentResolver.query(
                Uri.withAppendedPath(PositionTable.URI, barcode),
                null, null, null, null)
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        createPosition(cursor)?.let { positionsList.add(it) }
                    }
                }

        return positionsList
    }

    /**
     * Получить текущий открытый чек.
     * @param context контекст приложения
     * @param type тип чека
     * @return чек или null, если чек закрыт
     */
    @JvmStatic
    fun getReceipt(context: Context, type: Receipt.Type): Receipt? = getReceipt(context, type, null)

    /**
     * Получить чек по uuid. Чек может быть уже закрыт
     * @param context контекст приложения
     * @param uuid uuid чека
     * @return чек или null, если чек не найден
     */
    @JvmStatic
    fun getReceipt(context: Context, uuid: String): Receipt? = getReceipt(context, null, uuid)

    private fun getReceipt(context: Context, type: Receipt.Type?, uuid: String? = null): Receipt? {
        if (type == null && uuid == null) {
            throw IllegalArgumentException("type or uuid should be not null")
        }

        val baseUri = when (type) {
            Receipt.Type.SELL -> CURRENT_SELL_RECEIPT_URI
            Receipt.Type.PAYBACK -> CURRENT_PAYBACK_RECEIPT_URI
            Receipt.Type.BUY -> CURRENT_BUY_RECEIPT_URI
            Receipt.Type.BUYBACK -> CURRENT_BUYBACK_RECEIPT_URI
            Receipt.Type.CORRECTION_INCOME -> CURRENT_CORRECTION_INCOME_RECEIPT_URI
            Receipt.Type.CORRECTION_OUTCOME -> CURRENT_CORRECTION_OUTCOME_RECEIPT_URI
            Receipt.Type.CORRECTION_RETURN_INCOME -> CURRENT_CORRECTION_RETURN_INCOME_RECEIPT_URI
            Receipt.Type.CORRECTION_RETURN_OUTCOME -> CURRENT_CORRECTION_RETURN_OUTCOME_RECEIPT_URI
            else -> Uri.withAppendedPath(RECEIPTS_URI, uuid)
        }

        val header = context.contentResolver.query(
                baseUri,
                null,
                null,
                null,
                null
        )?.use {
            if (it.moveToNext()) {
                return@use createReceiptHeader(it)
            } else {
                return null
            }
        } ?: return null

        val printGroups = HashSet<PrintGroup?>()
        val getPositionResults = ArrayList<GetPositionResult>()
        val getSubpositionResults = ArrayList<GetSubpositionResult>()
        context.contentResolver.query(
                Uri.withAppendedPath(baseUri, POSITIONS_PATH),
                null,
                null,
                null,
                null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                createGetPositionResult(cursor)?.let {
                    getPositionResults.add(it)
                    printGroups.add(it.printGroup)
                } ?: createGetSubpositionResult(cursor)?.let {
                    getSubpositionResults.add(it)
                }
            }
        }

        for (getPositionResult in getPositionResults) {
            val subpositions = getSubpositionResults
                    .filter { it.parentUuid == getPositionResult.position.uuid }
                    .map { it.position }
            getPositionResult.position = Position.Builder
                    .copyFrom(getPositionResult.position)
                    .setSubPositions(subpositions)
                    .build()
        }

        val getPaymentsResults = ArrayList<GetPaymentsResult>()
        context.contentResolver.query(
                Uri.withAppendedPath(baseUri, PAYMENTS_PATH),
                null,
                null,
                null,
                null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                createGetPaymentResult(cursor)?.let {
                    getPaymentsResults.add(it)
                    printGroups.add(it.printGroup)
                }
            }
        }

        val receiptDiscount = try {
            val discountMap = HashMap<String, BigDecimal>()

            context.contentResolver.query(
                    Uri.withAppendedPath(baseUri, DISCOUNTS_PATH),
                    null,
                    null,
                    null,
                    null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val posDiscountUuid = cursor.getString(cursor.getColumnIndex(POSITION_DISCOUNT_UUID_COLUMN_NAME))
                    val discount = cursor.getMoney(DISCOUNT_COLUMN_NAME)

                    discountMap[posDiscountUuid] = discount
                }
            }

            discountMap
        } catch (error: IllegalArgumentException) {
            //old version of evopos, does not support discounts
            error.printStackTrace()
            null
        }

        val printDocuments = ArrayList<Receipt.PrintReceipt>()
        val groupByPrintGroupPaymentResults = getPaymentsResults
                .groupBy { it.printGroup }
        for (printGroup in printGroups) {
            val payments = groupByPrintGroupPaymentResults[printGroup]?.associateBy { it.payment }
                    ?: HashMap<Payment, ReceiptApi.GetPaymentsResult>()
            printDocuments.add(Receipt.PrintReceipt(
                    printGroup,
                    getPositionResults
                            .filter { it.printGroup == printGroup }
                            .map { it.position },
                    payments.mapValues { it.value.value },
                    payments.mapValues { it.value.change },
                    receiptDiscount
            ))
        }

        return Receipt(
                header,
                printDocuments
        )
    }


    /**
     * Получить заголовок текущего открытого чека.
     * @param context контекст приложения
     * @param type тип чека
     * @return чек или null, если чек закрыт
     */
    @JvmStatic
    fun getReceiptHeader(context: Context, type: Receipt.Type): Receipt.Header? {
        val baseUri = when (type) {
            Receipt.Type.SELL -> CURRENT_SELL_RECEIPT_URI
            Receipt.Type.PAYBACK -> CURRENT_PAYBACK_RECEIPT_URI
            Receipt.Type.BUY -> CURRENT_BUY_RECEIPT_URI
            Receipt.Type.BUYBACK -> CURRENT_BUYBACK_RECEIPT_URI
            Receipt.Type.CORRECTION_INCOME -> CURRENT_CORRECTION_INCOME_RECEIPT_URI
            Receipt.Type.CORRECTION_OUTCOME -> CURRENT_CORRECTION_OUTCOME_RECEIPT_URI
            Receipt.Type.CORRECTION_RETURN_INCOME -> CURRENT_CORRECTION_RETURN_INCOME_RECEIPT_URI
            Receipt.Type.CORRECTION_RETURN_OUTCOME -> CURRENT_CORRECTION_RETURN_OUTCOME_RECEIPT_URI
        }

        return context.contentResolver.query(
                baseUri,
                null,
                null,
                null,
                null
        )?.use {
            if (it.moveToNext()) {
                createReceiptHeader(it)
            } else {
                null
            }
        }
    }

    /**
     * Запрос списка заголовков чека
     * @param context контекст приложения
     * @param type фильтр по типу чека
     * @return курсор с заголовками чека
     */
    @JvmStatic
    fun getReceiptHeaders(context: Context, type: Receipt.Type? = null): ru.evotor.query.Cursor<Receipt.Header?>? {
        return context.contentResolver.query(
                RECEIPTS_URI,
                null,
                type?.let { "${ReceiptHeaderTable.COLUMN_TYPE} = ?" },
                type?.let { arrayOf(it.name) },
                null
        )?.let {
            object : ru.evotor.query.Cursor<Receipt.Header?>(it) {
                override fun getValue(): Receipt.Header? = createReceiptHeader(this)
            }
        }
    }

    /**
     * Получить фискальные чеки по идентификатору ["чека"][ru.evotor.framework.receipt.Receipt].
     * @param context контекст приложения
     * @param receiptUuid uuid ["чека"][ru.evotor.framework.receipt.Receipt]
     */
    @JvmStatic
    fun getFiscalReceipts(context: Context, receiptUuid: String): ru.evotor.query.Cursor<FiscalReceipt>? =
            context.contentResolver.query(FiscalReceiptContract.URI, null, null, arrayOf(receiptUuid), null)
                    ?.let {
                        object : ru.evotor.query.Cursor<FiscalReceipt>(it) {
                            override fun getValue(): FiscalReceipt = FiscalReceiptMapper.read(this)
                        }
                    }


    private fun createGetPositionResult(cursor: Cursor): GetPositionResult? {
        return if (cursor.getString(cursor.getColumnIndex(PositionTable.COLUMN_PARENT_POSITION_UUID)) == null)
            GetPositionResult(
                    createPosition(cursor) ?: return null,
                    createPrintGroup(cursor)
            )
        else
            null
    }

    private fun createGetSubpositionResult(cursor: Cursor): GetSubpositionResult? {
        val parentUuid = cursor.getString(cursor.getColumnIndex(PositionTable.COLUMN_PARENT_POSITION_UUID))
        return if (parentUuid != null)
            GetSubpositionResult(
                    createPosition(cursor) ?: return null,
                    parentUuid
            )
        else
            null
    }

    private fun createGetPaymentResult(cursor: Cursor): GetPaymentsResult? {
        return GetPaymentsResult(
            createPayment(cursor) ?: return null,
            createPrintGroup(cursor),
            cursor.getMoney(PaymentTable.COLUMN_VALUE_BY_PRINT_GROUP),
            cursor.getMoney(PaymentTable.COLUMN_CHANGE_BY_PRINT_GROUP)
        )
    }

    private fun createPrintGroup(cursor: Cursor): PrintGroup? {
        val purchaser = createPurchaser(cursor)
        val medicineAttribute = createMedicineAttribute(cursor)
        return PrintGroup(
            cursor.getString(cursor.getColumnIndex(PrintGroupSubTable.COLUMN_IDENTIFIER))
                ?: return null,
            Utils.safeValueOf(PrintGroup.Type::class.java, cursor.getString(cursor.getColumnIndex(PrintGroupSubTable.COLUMN_TYPE)), null),
            cursor.getString(cursor.getColumnIndex(PrintGroupSubTable.COLUMN_ORG_NAME)),
            cursor.getString(cursor.getColumnIndex(PrintGroupSubTable.COLUMN_ORG_INN)),
            cursor.getString(cursor.getColumnIndex(PrintGroupSubTable.COLUMN_ORG_ADDRESS)),
            Utils.safeValueOf(TaxationSystem::class.java, cursor.getString(cursor.getColumnIndex(PrintGroupSubTable.COLUMN_TAXATION_SYSTEM)), null),
            cursor.getInt(cursor.getColumnIndex(PrintGroupSubTable.COLUMN_SHOULD_PRINT_RECEIPT)) == 1,
            purchaser,
            medicineAttribute
        )
    }

    private fun createPurchaser(cursor: Cursor): Purchaser? {
        val purchaserName = cursor.optString(PrintGroupSubTable.COLUMN_PURCHASER_NAME)
        val purchaserDocumentNumber = cursor.optString(PrintGroupSubTable.COLUMN_PURCHASER_DOCUMENT_NUMBER)

        return if (purchaserName != null && purchaserDocumentNumber != null) {
            val purchaserType = cursor.optLong(PrintGroupSubTable.COLUMN_PURCHASER_TYPE)?.let {
                PurchaserType.values()[it.toInt()]
            }
            Purchaser(purchaserName, purchaserDocumentNumber, purchaserType)
        } else {
            null
        }
    }

    private fun createMedicineAttribute(cursor: Cursor): MedicineAttribute? {
        val subjectId = cursor.optString(MedicineAttributeSubTable.COLUMN_SUBJECT_ID)
            ?: return null

        val preferentialMedicineType: PreferentialMedicine.PreferentialMedicineType? =
                cursor.optString(MedicineAttributeSubTable.COLUMN_PREFERENTIAL_MEDICINE_TYPE)?.let {
                    PreferentialMedicine.PreferentialMedicineType.valueOf(it)
                }

        val documentNumber: String? = cursor.optString(MedicineAttributeSubTable.COLUMN_MEDICINE_DOCUMENT_NUMBER)
        val documentDate: Date? = cursor.optLong(MedicineAttributeSubTable.COLUMN_MEDICINE_DOCUMENT_DATE)?.let { Date(it) }
        val serialNumber: String? = cursor.optString(MedicineAttributeSubTable.COLUMN_MEDICINE_SERIAL_NUMBER)

        val medicineAdditionalDetails: MedicineAdditionalDetails? =
                if (documentDate != null && documentNumber != null && serialNumber != null)
                    MedicineAdditionalDetails(documentNumber, documentDate, serialNumber)
                else null

        return MedicineAttribute(
                subjectId = subjectId,
                preferentialMedicineType = preferentialMedicineType,
                medicineAdditionalDetails = medicineAdditionalDetails
        )
    }

    private fun createPosition(cursor: Cursor): Position? {
        val price = cursor.getMoney(PositionTable.COLUMN_PRICE)
        val priceWithDiscountPosition = if (cursor.getColumnIndex(PositionTable.COLUMN_PRICE_WITH_DISCOUNT_POSITION) != -1) {
            cursor.getMoney(PositionTable.COLUMN_PRICE_WITH_DISCOUNT_POSITION)
        } else {
            price
        }
        val extraKeys = cursor.optString(PositionTable.COLUMN_EXTRA_KEYS)?.let {
            createExtraKeysFromDBFormat(cursor.optString(PositionTable.COLUMN_EXTRA_KEYS))
        }
        val attributes = cursor.optString(PositionTable.COLUMN_ATTRIBUTES)?.let {
            createAttributesFromDBFormat(cursor.optString(PositionTable.COLUMN_ATTRIBUTES))
        }

        val classificationCode = cursor.optString(PositionTable.COLUMN_CLASSIFICATION_CODE)
        val excise = cursor.optString(PositionTable.COLUMN_EXCISE)?.let {
            BigDecimal(it)
        }

        val importationData = createImportationData(
                cursor.optString(PositionTable.COLUMN_IMPORTATION_DATA_COUNTRY_ORIGIN_CODE),
                cursor.optString(PositionTable.COLUMN_IMPORTATION_DATA_CUSTOMS_DECLARATION_NUMBER)
        )

        val builder = Position.Builder
            .copyFrom(Position(
                cursor.getString(cursor.getColumnIndex(PositionTable.COLUMN_POSITION_UUID)),
                cursor.getString(cursor.getColumnIndex(PositionTable.COLUMN_PRODUCT_UUID)),
                cursor.getString(cursor.getColumnIndex(PositionTable.COLUMN_PRODUCT_CODE)),
                Utils.safeValueOf(ProductType::class.java, cursor.getString(cursor.getColumnIndex(PositionTable.COLUMN_PRODUCT_TYPE)), ProductType.NORMAL),
                cursor.getString(cursor.getColumnIndex(PositionTable.COLUMN_NAME)),
                readFromPositionCursor(cursor),
                cursor.optString(PositionTable.COLUMN_TAX_NUMBER)?.let { TaxNumber.valueOf(it) },
                price,
                priceWithDiscountPosition,
                cursor.getQuantity(PositionTable.COLUMN_QUANTITY),
                cursor.optString(PositionTable.COLUMN_BARCODE),
                cursor.optString(PositionTable.COLUMN_MARK)?.let {
                    val rawMark = cursor.getString(cursor.getColumnIndex(PositionTable.COLUMN_MARK))
                    Mark.RawMark(rawMark)
                },
                cursor.optVolume(PositionTable.COLUMN_ALCOHOL_BY_VOLUME),
                cursor.getLong(cursor.getColumnIndex(PositionTable.COLUMN_ALCOHOL_PRODUCT_KIND_CODE)),
                cursor.optVolume(PositionTable.COLUMN_TARE_VOLUME),
                extraKeys,
                emptyList()
            ))
            .setAttributes(attributes)
            .setAgentRequisites(AgentRequisitesMapper.read(cursor))
            .setSettlementMethod(SettlementMethodMapper.fromCursor(cursor))
            .setPreferentialMedicine(PreferentialMedicineMapper.readFromCursor(cursor))
            .setClassificationCode(classificationCode)
            .setImportationData(importationData)
            .setExcise(excise)
            .setPartialRealization(PositionPartialRealizationMapper.fromCursor(cursor))
        return builder.build()
    }

    private fun createImportationData(countryOriginCode: String?, customsDeclarationNumber: String?): ImportationData? {
        return if (countryOriginCode.isNullOrBlank() || customsDeclarationNumber.isNullOrBlank()) {
            null
        } else {
            ImportationData(countryOriginCode, customsDeclarationNumber)
        }
    }

    private fun readFromPositionCursor(cursor: Cursor): Measure {
        return cursor.let {
            Measure(
                    it.getString(cursor.getColumnIndex(PositionTable.COLUMN_MEASURE_NAME)),
                    it.getInt(cursor.getColumnIndex(PositionTable.COLUMN_MEASURE_PRECISION)),
                    it.optInt(cursor.getColumnIndex(PositionTable.COLUMN_MEASURE_CODE)) ?: Measure.UNKNOWN_MEASURE_CODE
            )
        }
    }


    private fun createAttributesFromDBFormat(value: String?): Map<String, AttributeValue> {
        if (value == null) return emptyMap()
        val array = JSONArray(value)
        return (0 until array.length()).toList()
                .map { array.getJSONObject(it) }
                .map {
                    val attributeUuid = it.optString(PositionTable.AttributeJSONKeys.DICTIONARY_UUID)
                    attributeUuid to AttributeValue(
                            attributeUuid,
                            it.optString(PositionTable.AttributeJSONKeys.DICTIONARY_NAME),
                            it.optString(PositionTable.AttributeJSONKeys.UUID),
                            it.optString(PositionTable.AttributeJSONKeys.NAME)
                    )
                }.toMap()
    }

    private fun createPayment(cursor: Cursor): Payment? {
        val identifierColumnIndex = cursor.getColumnIndex(PaymentTable.COLUMN_IDENTIFIER)
        val identifier = if (identifierColumnIndex != -1) {
            cursor.getString(identifierColumnIndex)
        } else {
            null
        }

        return Payment(
            cursor.getString(cursor.getColumnIndex(PaymentTable.COLUMN_UUID)),
            cursor.getMoney(PaymentTable.COLUMN_VALUE),
            createPaymentSystem(cursor),
            createPaymentPerformer(cursor) ?: return null,
            cursor.optString(PaymentTable.COLUMN_PURPOSED_IDENTIFIER),
            cursor.optString(PaymentTable.COLUMN_ACCOUNT_ID),
            cursor.optString(PaymentTable.COLUMN_ACCOUNT_USER_DESCRIPTION),
            identifier)
    }

    private fun createPaymentPerformer(cursor: Cursor): PaymentPerformer? {
        return PaymentPerformer(
            createPaymentSystem(cursor) ?: return null,
            cursor.optString(PaymentPerformerTable.COLUMN_PACKAGE_NAME),
            cursor.optString(PaymentPerformerTable.COLUMN_COMPONENT_NAME),
            cursor.optString(PaymentPerformerTable.COLUMN_APP_UUID),
            cursor.optString(PaymentPerformerTable.COLUMN_APP_NAME)
        )
    }

    private fun createPaymentSystem(cursor: Cursor): PaymentSystem? {
        return PaymentSystem(
            Utils.safeValueOf(PaymentType::class.java, cursor.getString(cursor.getColumnIndex(PaymentSystemTable.COLUMN_PAYMENT_TYPE)), null)
                ?: return null,
            cursor.getString(cursor.getColumnIndex(PaymentSystemTable.COLUMN_PAYMENT_SYSTEM_USER_DESCRIPTION))
                ?: return null,
            cursor.getString(cursor.getColumnIndex(PaymentSystemTable.COLUMN_PAYMENT_SYSTEM_ID))
                ?: return null
        )
    }

    private fun createExtraKeysFromDBFormat(value: String?): Set<ExtraKey> {
        val result = HashSet<ExtraKey>()
        value ?: return result
        val jsonExtraKeys = JSONArray(value)
        for (i in 0 until jsonExtraKeys.length()) {
            jsonExtraKeys.getJSONObject(i).let {
                result.add(ExtraKey(
                        it.optString(PositionTable.ExtraKeyJSONKeys.KEY_IDENTITY),
                        it.optString(PositionTable.ExtraKeyJSONKeys.KEY_APP_ID),
                        it.optString(PositionTable.ExtraKeyJSONKeys.KEY_DESCRIPTION)
                ))
            }
        }

        return result
    }

    private fun createReceiptHeader(cursor: Cursor): Receipt.Header? {
        val extraIndex = cursor.getColumnIndex(ReceiptHeaderTable.COLUMN_EXTRA)
        val extra = if (extraIndex == -1) null else cursor.getString(extraIndex)

        return Receipt.Header(
            uuid = cursor.getString(cursor.getColumnIndex(ReceiptHeaderTable.COLUMN_UUID)),
            baseReceiptUuid = cursor.optString(ReceiptHeaderTable.COLUMN_BASE_RECEIPT_UUID),
            number = cursor.optString(ReceiptHeaderTable.COLUMN_NUMBER),
            type = Utils.safeValueOf(Receipt.Type::class.java, cursor.getString(cursor.getColumnIndex(ReceiptHeaderTable.COLUMN_TYPE)), null)
                ?: return null,
            date = cursor.optLong(ReceiptHeaderTable.COLUMN_DATE)?.let { Date(it) },
            clientEmail = cursor.optString(ReceiptHeaderTable.COLUMN_CLIENT_EMAIL),
            clientPhone = cursor.optString(ReceiptHeaderTable.COLUMN_CLIENT_PHONE),
            extra = extra
        )
    }

    private data class GetPositionResult(var position: Position, val printGroup: PrintGroup?)
    private data class GetSubpositionResult(val position: Position, val parentUuid: String?)
    private data class GetPaymentsResult(val payment: Payment, val printGroup: PrintGroup?, val value: BigDecimal, val change: BigDecimal)

    @Deprecated(message = "Используйте метод getSellReceipt")
    object Description {

        const val PATH_RECEIPT_DESCRIPTION = "information"

        @JvmField
        val URI = Uri.withAppendedPath(BASE_URI, PATH_RECEIPT_DESCRIPTION)

        const val ROW_ID = "_id"
        const val ROW_UUID = "uuid"
        const val ROW_DISCOUNT = "discount"

    }

    @Deprecated(message = "Используйте метод getSellReceipt")
    object Positions {

        const val PATH_RECEIPT_POSITIONS = "positions"

        @JvmField
        val URI = Uri.withAppendedPath(BASE_URI, PATH_RECEIPT_POSITIONS)

        const val ROW_UUID = "uuid"
        const val ROW_PRODUCT_UUID = "productUuid"
        const val ROW_TYPE = "type"
        const val ROW_CODE = "code"
        const val ROW_MEASURE_NAME = "measureName"
        const val ROW_MEASURE_PRECISION = "measurePrecision"
        const val ROW_PRICE = "price"
        const val ROW_QUANTITY = "quantity"
        const val ROW_MARK = "mark"
        const val ROW_NAME = "name"
    }

    @Deprecated(message = "Используйте метод getSellReceipt")
    object Payments {

        const val PATH_RECEIPT_PAYMENTS = "payments"

        @JvmField
        val URI: Uri = Uri.withAppendedPath(BASE_URI, PATH_RECEIPT_PAYMENTS)

        const val ROW_ID = "_id"
        const val ROW_UUID = "uuid"
        const val ROW_SUM = "sum"
        const val ROW_TYPE = "type"
        const val ROW_RRN = "rrn"
        const val ROW_PIN_PAD_UUID = "pin_pad_uuid"

        object Type {
            const val TYPE_CASH = 0
            const val TYPE_CARD = 1
        }

    }
}
