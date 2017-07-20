package ru.evotor.framework.core.action.datamapper;

import android.os.Bundle;
import android.support.annotation.Nullable;

import ru.evotor.framework.Utils;
import ru.evotor.framework.receipt.TaxNumber;


public final class TaxNumberMapper {

    private static final String KEY_TAX_NUMBER = "taxNumber";

    @Nullable
    public static TaxNumber from(@Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        String taxNumber = bundle.getString(KEY_TAX_NUMBER);

        return Utils.safeValueOf(TaxNumber.class, taxNumber, TaxNumber.NO_VAT);
    }

    @Nullable
    public static Bundle toBundle(@Nullable TaxNumber taxNumber) {
        if (taxNumber == null) {
            return null;
        }
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TAX_NUMBER, taxNumber.getValue().toPlainString());

        return bundle;
    }

    private TaxNumberMapper() {
    }

}
