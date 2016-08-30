package com.microsoft.sonoma.core.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.microsoft.sonoma.core.BuildConfig;
import com.microsoft.sonoma.core.Sonoma;
import com.microsoft.sonoma.core.ingestion.models.Device;

import java.util.Locale;
import java.util.TimeZone;

/**
 * DeviceInfoHelper class to retrieve device information.
 */
public class DeviceInfoHelper {

    /**
     * OS name.
     */
    private static final String OS_NAME = "Android";

    /**
     * Gets device information.
     *
     * @param context The context of the application.
     * @return {@link Device}
     * @throws DeviceInfoException If device information cannot be retrieved
     */
    public static Device getDeviceInfo(Context context) throws DeviceInfoException {
        Device device = new Device();

        /* Application version. */
        PackageInfo packageInfo;
        try {
            PackageManager packageManager = context.getPackageManager();
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            device.setAppVersion(packageInfo.versionName);
            device.setAppBuild(String.valueOf(packageInfo.versionCode));
        } catch (Exception e) {
            SonomaLog.error(Sonoma.LOG_TAG, "Cannot retrieve package info", e);
            throw new DeviceInfoException("Cannot retrieve package info", e);
        }

        /* Application namespace. */
        device.setAppNamespace(context.getPackageName());

        /* Carrier info. */
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            device.setCarrierCountry(telephonyManager.getNetworkCountryIso());
            device.setCarrierName(telephonyManager.getNetworkOperatorName());
        } catch (Exception e) {
            SonomaLog.error(Sonoma.LOG_TAG, "Cannot retrieve carrier info", e);
        }

        /* Locale. */
        device.setLocale(Locale.getDefault().toString());

        /* Hardware info. */
        device.setModel(Build.MODEL);
        device.setOemName(Build.MANUFACTURER);

        /* OS version. */
        device.setOsApiLevel(Build.VERSION.SDK_INT);
        device.setOsName(OS_NAME);
        device.setOsVersion(Build.VERSION.RELEASE);
        device.setOsBuild(Build.ID);

        /* Screen size. */
        try {
            device.setScreenSize(getScreenSize(context));
        } catch (Exception e) {
            SonomaLog.error(Sonoma.LOG_TAG, "Cannot retrieve screen size", e);
        }

        /* SDK version. */
        device.setSdkVersion(BuildConfig.VERSION_NAME);

        /* Timezone offset in minutes (including DST). */
        device.setTimeZoneOffset(TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60 / 1000);
        return device;
    }

    /**
     * Gets a size of a device for base orientation.
     *
     * @param context The context of the application.
     * @return A string with {@code <width>x<height>} format.
     */
    @SuppressLint("SwitchIntDef")
    @SuppressWarnings("SuspiciousNameCombination")
    private static String getScreenSize(Context context) {

        /* Guess resolution based on the natural device orientation */
        int screenWidth;
        int screenHeight;
        Display defaultDisplay = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        Point size = new Point();
        defaultDisplay.getSize(size);
        switch (defaultDisplay.getRotation()) {
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                screenHeight = size.x;
                screenWidth = size.y;
                break;
            default:
                screenWidth = size.x;
                screenHeight = size.y;
        }

         /* Serialize screen resolution */
        return screenWidth + "x" + screenHeight;
    }

    /**
     * Thrown when {@link DeviceInfoHelper} cannot retrieve device information from devices
     */
    public static class DeviceInfoException extends Exception {

        @SuppressWarnings("SameParameterValue")
        public DeviceInfoException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
}