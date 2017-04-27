package com.udacity.stockhawk.util;

import android.content.Context;
import android.util.TypedValue;

/**
 * Created by Carl on 4/25/2017.
 */

public final class ThemeUtility {

    public static int getAttributeFromCurrentTheme(Context context, int resId) {
        final TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(resId, typedValue, true);
        return typedValue.data;
    }

}
