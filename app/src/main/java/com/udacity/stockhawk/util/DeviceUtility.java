package com.udacity.stockhawk.util;

import android.content.Context;
import com.udacity.stockhawk.R;

/**
 * Created by Carl on 4/25/2017.
 */

public final class DeviceUtility {
    /**
     * Checks if device is in RTL orientation.
     * @return True if displaying as RTL, false otherwise.
     */
    public static boolean isRTL(Context context) {
        return context.getResources().getBoolean(R.bool.is_RTL);
    }
}
