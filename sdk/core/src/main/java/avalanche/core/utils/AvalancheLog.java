package avalanche.core.utils;


import android.support.annotation.IntRange;
import android.util.Log;

import static android.util.Log.ASSERT;
import static android.util.Log.VERBOSE;

/**
 * <h3>Description</h3>
 * <p/>
 * Wrapper class for logging in the SDK as well as
 * setting the desired log level for end users.
 * Log levels correspond to those of android.util.Log.
 *
 * @see Log
 */
public class AvalancheLog {
    private static final String AVALANCHE_TAG = "Avalanche";

    private static int sLogLevel = Log.ASSERT;

    /**
     * Get the log level to find out how much data the AvalancheSDK spews into LogCat. The Default will be
     * LOG_LEVEL.ASSERT so nothing shows up in LogCat.
     *
     * @return the log level
     */
    @IntRange(from = VERBOSE, to = ASSERT)
    public static int getLogLevel() {
        return sLogLevel;
    }

    /**
     * Set the log level to determine the amount of info the AvalancheSDK spews info into LogCat.
     *
     * @param avalancheLogLevel The log level for AvalancheSDK logging
     */
    @IntRange(from = VERBOSE, to = ASSERT)
    public static void setLogLevel(int avalancheLogLevel) {
        sLogLevel = avalancheLogLevel;
    }


    /**
     * Log a message with level VERBOSE with the default tag
     *
     * @param message the log message
     */
    public static void verbose(String message) {
        verbose(null, message);
    }

    /**
     * Log a message with level VERBOSE
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    public static void verbose(String tag, String message) {
        tag = sanitizeTag(tag);
        if (sLogLevel <= Log.VERBOSE) {
            Log.v(tag, message);
        }
    }

    /**
     * Log a message with level VERBOSE with the default tag
     *
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    @SuppressWarnings("SameParameterValue")
    public static void verbose(String message, Throwable throwable) {
        verbose(null, message, throwable);
    }

    /**
     * Log a message with level VERBOSE
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void verbose(String tag, String message, Throwable throwable) {
        tag = sanitizeTag(tag);
        if (sLogLevel <= Log.VERBOSE) {
            Log.v(tag, message, throwable);
        }
    }

    /**
     * Log a message with level DEBUG with the default tag
     *
     * @param message the log message
     */
    public static void debug(String message) {
        debug(null, message);
    }

    /**
     * Log a message with level DEBUG
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    public static void debug(String tag, String message) {
        tag = sanitizeTag(tag);
        if (sLogLevel <= Log.DEBUG) {
            Log.d(tag, message);
        }
    }

    /**
     * Log a message with level DEBUG with the default tag
     *
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    @SuppressWarnings("SameParameterValue")
    public static void debug(String message, Throwable throwable) {
        debug(null, message, throwable);
    }

    /**
     * Log a message with level DEBUG
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void debug(String tag, String message, Throwable throwable) {
        tag = sanitizeTag(tag);
        if (sLogLevel <= Log.DEBUG) {
            Log.d(tag, message, throwable);
        }
    }

    /**
     * Log a message with level INFO with the default tag
     *
     * @param message the log message
     */
    public static void info(String message) {
        info(null, message);
    }

    /**
     * Log a message with level INFO
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    public static void info(String tag, String message) {
        tag = sanitizeTag(tag);
        if (sLogLevel <= Log.INFO) {
            Log.i(tag, message);
        }
    }

    /**
     * Log a message with level INFO with the default tag
     *
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    @SuppressWarnings("SameParameterValue")
    public static void info(String message, Throwable throwable) {
        info(null, message, throwable);
    }

    /**
     * Log a message with level INFO
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void info(String tag, String message, Throwable throwable) {
        tag = sanitizeTag(tag);
        if (sLogLevel <= Log.INFO) {
            Log.i(tag, message, throwable);
        }
    }

    /**
     * Log a message with level WARN with the default tag
     *
     * @param message the log message
     */
    public static void warn(String message) {
        warn(null, message);
    }

    /**
     * Log a message with level WARN
     *
     * @param tag     the TAG
     * @param message the log message
     */
    public static void warn(String tag, String message) {
        tag = sanitizeTag(tag);
        if (sLogLevel <= Log.WARN) {
            Log.w(tag, message);
        }
    }

    /**
     * Log a message with level WARN with the default tag
     *
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void warn(String message, Throwable throwable) {
        warn(null, message, throwable);
    }

    /**
     * Log a message with level WARN
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void warn(String tag, String message, Throwable throwable) {
        tag = sanitizeTag(tag);
        if (sLogLevel <= Log.WARN) {
            Log.w(tag, message, throwable);
        }
    }

    /**
     * Log a message with level ERROR with the default tag
     *
     * @param message the log message
     */
    public static void error(String message) {
        error(null, message);
    }

    /**
     * Log a message with level ERROR
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    public static void error(String tag, String message) {
        tag = sanitizeTag(tag);
        if (sLogLevel <= Log.ERROR) {
            Log.e(tag, message);
        }
    }

    /**
     * Log a message with level ERROR with the default tag
     *
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void error(String message, Throwable throwable) {
        error(null, message, throwable);
    }

    /**
     * Log a message with level ERROR
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void error(String tag, String message, Throwable throwable) {
        tag = sanitizeTag(tag);
        if (sLogLevel <= Log.ERROR) {
            Log.e(tag, message, throwable);
        }
    }

    /**
     * Sanitize a TAG string
     *
     * @param tag the log tag for your message for the logging
     * @return a sanitized TAG, defaults to 'Avalanche' in case the log tag for your message is null, empty or longer than
     * 23 characters.
     */
    private static String sanitizeTag(String tag) {
        if ((tag == null) || (tag.length() == 0) || (tag.length() > 23)) {
            tag = AVALANCHE_TAG;
        }

        return tag;
    }

}
