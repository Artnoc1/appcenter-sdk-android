package avalanche.errors.model;

/**
 * Crash Manager user input
 *
 */
public enum CrashesUserInput {
    /**
     * User chose not to send the crash report
     */
    CrashManagerUserInputDontSend(0),
    /**
     * User wants the crash report to be sent
     */
    CrashManagerUserInputSend(1),
    /**
     * User chose to always send crash reports
     */
    CrashManagerUserInputAlwaysSend(2);

    private final int mValue;

    CrashesUserInput(int value) {
        this.mValue = value;
    }

    public int getValue() {
        return mValue;
    }
}
