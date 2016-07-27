package avalanche.errors;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import avalanche.core.AbstractAvalancheFeature;
import avalanche.core.Constants;
import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.HttpURLConnectionBuilder;
import avalanche.core.utils.Util;
import avalanche.errors.ingestion.models.ErrorLog;
import avalanche.errors.ingestion.models.json.ErrorLogFactory;
import avalanche.errors.model.CrashMetaData;
import avalanche.errors.model.CrashReport;
import avalanche.errors.model.CrashesUserInput;

import static android.text.TextUtils.isEmpty;
import static avalanche.core.utils.StorageHelper.InternalStorage;
import static avalanche.core.utils.StorageHelper.PreferencesStorage;


public class Crashes extends AbstractAvalancheFeature {

    /**
     * Constant marking event of the error group.
     */
    public static final String ERROR_GROUP = "group_error";

    private static final String ALWAYS_SEND_KEY = "always_send_crash_reports";

    private static final int STACK_TRACES_FOUND_NONE = 0;
    private static final int STACK_TRACES_FOUND_NEW = 1;
    private static final int STACK_TRACES_FOUND_CONFIRMED = 2;
    private static Crashes sharedInstance = null;
    private CrashesListener mListener;
    private String mEndpointUrl;
    private WeakReference<Context> mContextWeakReference;
    private boolean mLazyExecution;
    private boolean mIsSubmitting = false;
    private long mInitializeTimestamp;
    private boolean mDidCrashInLastSession = false;

    protected Crashes() {
    }

    public static Crashes getInstance() {
        if (sharedInstance == null) {
            sharedInstance = new Crashes();
        }
        return sharedInstance;
    }

    /**
     * Searches .stacktrace files and returns them as array.
     */
    private static String[] searchForStackTraces() {
        if (Constants.FILES_PATH != null) {
            AvalancheLog.debug("Looking for exceptions in: " + Constants.FILES_PATH);

            // Filter for ".stacktrace" files
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".stacktrace");
                }
            };
            return InternalStorage.getFilenames(Constants.FILES_PATH, filter);
        } else {
            AvalancheLog.debug("Can't search for exception as file path is null.");
            return null;
        }
    }

    private static List<String> getConfirmedFilenames() {
        return Arrays.asList(PreferencesStorage.getString("ConfirmedFilenames", "").split("\\|"));
    }

    private static String getAlertTitle(Context context) {
        String appTitle = Util.getAppName(context);

        String message = context.getString(R.string.avalanche_crash_dialog_title);
        return String.format(message, appTitle);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        super.onActivityResumed(activity);
        if (mContextWeakReference == null && Util.isMainActivity(activity)) {
            // Opinionated approach -> per default we will want to activate the crash reporting with the very first of your activities.
            register(activity);
        }
    }

    /**
     * Registers the crash manager and handles existing crash logs.
     * Avalanche app identifier is read from configuration values in manifest.
     *
     * @param context The context to use. Usually your Activity object. If
     *                context is not an instance of Activity (or a subclass of it),
     *                crashes will be sent automatically.
     * @return The configured crash manager for method chaining.
     */
    public Crashes register(Context context) {
        return register(context, null);
    }

    /**
     * Registers the crash manager and handles existing crash logs.
     * Avalanche app identifier is read from configuration values in manifest.
     *
     * @param context  The context to use. Usually your Activity object. If
     *                 context is not an instance of Activity (or a subclass of it),
     *                 crashes will be sent automatically.
     * @param listener Implement this for callback functions.
     * @return The configured crash manager for method chaining.
     */
    public Crashes register(Context context, CrashesListener listener) {
        return register(context, Constants.BASE_URL, listener);
    }

    /**
     * Registers the crash manager and handles existing crash logs.
     * Avalanche app identifier is read from configuration values in manifest.
     *
     * @param context     The context to use. Usually your Activity object. If
     *                    context is not an instance of Activity (or a subclass of it),
     *                    crashes will be sent automatically.
     * @param endpointUrl URL of the Avalanche endpoint to use.
     * @param listener    Implement this for callback functions.
     * @return The configured crash manager for method chaining.
     */
    public Crashes register(Context context, String endpointUrl, CrashesListener listener) {
        return register(context, endpointUrl, listener, false);
    }

    /**
     * Registers the crash manager and handles existing crash logs.
     * Avalanche app identifier is read from configuration values in manifest.
     *
     * @param context       The context to use. Usually your Activity object. If
     *                      context is not an instance of Activity (or a subclass of it),
     *                      crashes will be sent automatically.
     * @param endpointUrl   URL of the Avalanche endpoint to use.
     * @param listener      Implement this for callback functions.
     * @param lazyExecution Whether the manager should execute lazily, e.g. not check for crashes right away.
     * @return this crashes module.
     */
    public Crashes register(Context context, String endpointUrl, CrashesListener listener, boolean lazyExecution) {
        mContextWeakReference = new WeakReference<>(context);
        mListener = listener;
        mEndpointUrl = endpointUrl;
        mLazyExecution = lazyExecution;

        initialize();

        return this;
    }

    private void initialize() {
        Context context = mContextWeakReference.get();

        if (context != null) {
            if (mInitializeTimestamp == 0) {
                mInitializeTimestamp = System.currentTimeMillis();
            }

            Constants.loadFromContext(context);

            if (!mLazyExecution) {
                execute();
            }
        }
    }

    /**
     * Allows you to execute the crash manager later on-demand.
     */
    public void execute() {
        Context context = mContextWeakReference.get();
        if (context == null) {
            return;
        }

        int foundOrSend = hasStackTraces();

        if (foundOrSend == STACK_TRACES_FOUND_NEW) {
            mDidCrashInLastSession = true;
            Boolean autoSend = !(context instanceof Activity);
            autoSend |= PreferencesStorage.getBoolean(ALWAYS_SEND_KEY, false);

            if (mListener != null) {
                autoSend |= mListener.shouldAutoUploadCrashes();

                mListener.onNewCrashesFound();
            }

            if (!autoSend) {
                showDialog(mContextWeakReference);
            } else {
                sendCrashes();
            }
        } else if (foundOrSend == STACK_TRACES_FOUND_CONFIRMED) {
            if (mListener != null) {
                mListener.onConfirmedCrashesFound();
            }

            sendCrashes();
        } else {
            registerExceptionHandler();
        }
    }

    private void registerExceptionHandler() {
        if (!isEmpty(Constants.APP_VERSION) && !isEmpty(Constants.APP_PACKAGE)) {
            // Get current handler
            Thread.UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
            if (currentHandler != null) {
                AvalancheLog.debug("Current handler class = " + currentHandler.getClass().getName());
            }

            // Update listener if already registered, otherwise set new handler
            if (currentHandler instanceof ExceptionHandler) {
                ((ExceptionHandler) currentHandler).setListener(mListener);
            } else {
                Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this, currentHandler, mListener, isIgnoreDefaultHandler()));
            }
        } else {
            AvalancheLog.warn("Exception handler not set because app version or package is null.");
        }
    }

    private void sendCrashes() {
        sendCrashes(null);
    }

    private void sendCrashes(final CrashMetaData crashMetaData) {
        saveConfirmedStackTraces();
        registerExceptionHandler();

        Context context = mContextWeakReference.get();
        if (context != null && !Util.isConnectedToNetwork(context)) {
            // Not connected to network, not trying to submit stack traces
            return;
        }

        if (!mIsSubmitting) {
            mIsSubmitting = true;

            new Thread() {
                @Override
                public void run() {
                    submitStackTraces(crashMetaData);
                    mIsSubmitting = false;
                }
            }.start();
        }
    }

    /**
     * Submits all stack traces in the files dir to Avalanche.
     *
     * @param crashMetaData The crashMetaData, provided by the user.
     */
    public void submitStackTraces(CrashMetaData crashMetaData) {
        String[] list = searchForStackTraces();
        Boolean successful = false;

        if ((list != null) && (list.length > 0)) {
            AvalancheLog.debug("Found " + list.length + " stacktrace(s).");

            for (String stackTrace : list) {
                HttpURLConnection urlConnection = null;
                try {
                    // Read contents of stack trace
                    String filename = Constants.FILES_PATH + "/" + stackTrace;
                    String stacktrace = InternalStorage.read(filename);
                    if (stacktrace.length() > 0) {
                        // Transmit stack trace with POST request

                        AvalancheLog.debug("Transmitting crash data: \n" + stacktrace);

                        // Retrieve user ID and contact information if given
                        String userID = InternalStorage.read(filename.replace(".stacktrace", ".user"));
                        String contact = InternalStorage.read(filename.replace(".stacktrace", ".contact"));

                        if (crashMetaData != null) {
                            final String crashMetaDataUserID = crashMetaData.getUserID();
                            if (!isEmpty(crashMetaDataUserID)) {
                                userID = crashMetaDataUserID;
                            }
                            final String crashMetaDataContact = crashMetaData.getUserEmail();
                            if (!isEmpty(crashMetaDataContact)) {
                                contact = crashMetaDataContact;
                            }
                        }

                        // Append application log to user provided description if present, if not, just send application log
                        final String applicationLog = InternalStorage.read(filename.replace(".stacktrace", ".description"));
                        String description = crashMetaData != null ? crashMetaData.getUserDescription() : "";
                        if (!isEmpty(applicationLog)) {
                            if (!isEmpty(description)) {
                                description = String.format("%s\n\nLog:\n%s", description, applicationLog);
                            } else {
                                description = String.format("Log:\n%s", applicationLog);
                            }
                        }

                        Map<String, String> parameters = new HashMap<>();

                        parameters.put("raw", stacktrace);
                        parameters.put("userID", userID);
                        parameters.put("contact", contact);
                        parameters.put("description", description);
                        parameters.put("sdk", Constants.SDK_NAME);
                        parameters.put("sdk_version", BuildConfig.VERSION_NAME);

                        urlConnection = new HttpURLConnectionBuilder(getURLString())
                                .setRequestMethod("POST")
                                .writeFormFields(parameters)
                                .build();

                        int responseCode = urlConnection.getResponseCode();

                        successful = (responseCode == HttpURLConnection.HTTP_ACCEPTED || responseCode == HttpURLConnection.HTTP_CREATED);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (successful) {
                        AvalancheLog.debug("Transmission succeeded");
                        deleteStackTrace(stackTrace);

                        if (mListener != null) {
                            mListener.onCrashesSent();
                            deleteRetryCounter(stackTrace, mListener.getMaxRetryAttempts());
                        }
                    } else {
                        AvalancheLog.debug("Transmission failed, will retry on next register() call");
                        if (mListener != null) {
                            mListener.onCrashesNotSent();
                            updateRetryCounter(stackTrace, mListener.getMaxRetryAttempts());
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows a dialog to ask the user whether he wants to send crash reports to
     * Avalanche or delete them.
     */
    private void showDialog(final WeakReference<Context> weakContext) {
        Context context = null;
        if (weakContext != null) {
            context = weakContext.get();
        }

        if (context == null) {
            return;
        }

        if (mListener != null && mListener.onHandleAlertView()) {
            return;
        }

        final boolean ignoreDefaultHandler = isIgnoreDefaultHandler();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String alertTitle = getAlertTitle(context);
        builder.setTitle(alertTitle);
        builder.setMessage(R.string.avalanche_crash_dialog_message);

        builder.setNegativeButton(R.string.avalanche_crash_dialog_negative_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                handleUserInput(CrashesUserInput.CrashManagerUserInputDontSend, null, mListener, weakContext, ignoreDefaultHandler);
            }
        });

        builder.setNeutralButton(R.string.avalanche_crash_dialog_neutral_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                handleUserInput(CrashesUserInput.CrashManagerUserInputAlwaysSend, null, mListener, weakContext, ignoreDefaultHandler);
            }
        });

        builder.setPositiveButton(R.string.avalanche_crash_dialog_positive_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                handleUserInput(CrashesUserInput.CrashManagerUserInputSend, null, mListener,
                        weakContext, ignoreDefaultHandler);
            }
        });

        builder.create().show();
    }

    /**
     * Provides an interface to pass user input from a custom alert to a crash report
     *
     * @param userInput            Defines the users action whether to send, always send, or not to send the crash report.
     * @param userProvidedMetaData The content of this optional CrashMetaData instance will be attached to the crash report
     *                             and allows to ask the user for e.g. additional comments or info.
     * @param listener             an optional crash manager listener to use.
     * @param weakContext          The context to use. Usually your Activity object.
     * @param ignoreDefaultHandler whether to ignore the default exception handler.
     * @return true if the input is a valid option and successfully triggered further processing of the crash report.
     * @see CrashesUserInput
     * @see CrashMetaData
     * @see CrashesListener
     */
    public boolean handleUserInput(final CrashesUserInput userInput,
                                   final CrashMetaData userProvidedMetaData, final CrashesListener listener,
                                   final WeakReference<Context> weakContext, final boolean ignoreDefaultHandler) {
        switch (userInput) {
            case CrashManagerUserInputDontSend:
                if (listener != null) {
                    listener.onUserDeniedCrashes();
                }

                deleteStackTraces();
                registerExceptionHandler();
                return true;
            case CrashManagerUserInputAlwaysSend:
                PreferencesStorage.putBoolean(ALWAYS_SEND_KEY, true);
                sendCrashes(userProvidedMetaData);
                return true;
            case CrashManagerUserInputSend:
                sendCrashes(userProvidedMetaData);
                return true;
            default:
                return false;
        }
    }

    long getInitializeTimestamp() {
        return mInitializeTimestamp;
    }

    /**
     * Checks if there are any saved stack traces in the files dir.
     *
     * @return STACK_TRACES_FOUND_NONE if there are no stack traces,
     * STACK_TRACES_FOUND_NEW if there are any new stack traces,
     * STACK_TRACES_FOUND_CONFIRMED if there only are confirmed stack traces.
     */
    public int hasStackTraces() {
        String[] filenames = searchForStackTraces();
        List<String> confirmedFilenames = null;
        int result = STACK_TRACES_FOUND_NONE;
        if ((filenames != null) && (filenames.length > 0)) {
            try {
                confirmedFilenames = getConfirmedFilenames();

            } catch (Exception e) {
                // Just in case, we catch all exceptions here
            }

            if (confirmedFilenames != null) {
                result = STACK_TRACES_FOUND_CONFIRMED;

                for (String filename : filenames) {
                    if (!confirmedFilenames.contains(filename)) {
                        result = STACK_TRACES_FOUND_NEW;
                        break;
                    }
                }
            } else {
                result = STACK_TRACES_FOUND_NEW;
            }
        }

        return result;
    }

    public boolean didCrashInLastSession() {
        return mDidCrashInLastSession;
    }

    public CrashReport getLastCrashDetails() {
        if (Constants.FILES_PATH == null || !didCrashInLastSession()) {
            return null;
        }

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".stacktrace");
            }
        };

        File lastModifiedFile = InternalStorage.lastModifiedFile(Constants.FILES_PATH, filter);
        CrashReport result = null;

        if (lastModifiedFile != null && lastModifiedFile.exists()) {
            try {
                result = CrashReport.fromFile(lastModifiedFile);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    private void saveConfirmedStackTraces() {
        try {
            String[] filenames = searchForStackTraces();
            PreferencesStorage.putString("ConfirmedFilenames", Util.joinArray(filenames, "|"));
        } catch (Exception e) {
            // Just in case, we catch all exceptions here
        }
    }

    public void deleteStackTraces() {
        String[] list = searchForStackTraces();

        if ((list != null) && (list.length > 0)) {
            AvalancheLog.debug("Found " + list.length + " stacktrace(s).");

            for (String stackTrace : list) {
                try {
                    AvalancheLog.debug("Delete stacktrace " + stackTrace + ".");
                    deleteStackTrace(stackTrace);
                    InternalStorage.delete(Constants.FILES_PATH + "/" + stackTrace);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Deletes the given filename and all corresponding files (same name,
     * different extension).
     */
    private void deleteStackTrace(String filename) {
        filename = Constants.FILES_PATH + "/" + filename;
        InternalStorage.delete(filename);

        String user = filename.replace(".stacktrace", ".user");
        InternalStorage.delete(user);

        String contact = filename.replace(".stacktrace", ".contact");
        InternalStorage.delete(contact);

        String description = filename.replace(".stacktrace", ".description");
        InternalStorage.delete(description);
    }

    /**
     * Update the retry attempts count for this crash stacktrace.
     */
    private void updateRetryCounter(String filename, int maxRetryAttempts) {
        if (maxRetryAttempts == -1) {
            return;
        }

        int retryCounter = PreferencesStorage.getInt("RETRY_COUNT: " + filename);
        if (retryCounter >= maxRetryAttempts) {
            deleteStackTrace(filename);
            deleteRetryCounter(filename, maxRetryAttempts);
        } else {
            PreferencesStorage.putInt("RETRY_COUNT: " + filename, retryCounter + 1);
        }
    }

    /**
     * Delete the retry counter if stacktrace is uploaded or retry limit is
     * reached.
     */
    private void deleteRetryCounter(String filename, int maxRetryAttempts) {
        PreferencesStorage.remove("RETRY_COUNT: " + filename);
    }

    private boolean isIgnoreDefaultHandler() {
        return mListener != null && mListener.ignoreDefaultHandler();
    }

    private String getURLString() {
        return mEndpointUrl + "api/2/apps/560eb1f4ada02d23d8c843682b867fca/crashes/";
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        HashMap<String, LogFactory> factories = new HashMap<>();
        factories.put(ErrorLog.TYPE, new ErrorLogFactory());
        return factories;
    }

    @Override
    protected String getGroupName() {
        return ERROR_GROUP;
    }
}
