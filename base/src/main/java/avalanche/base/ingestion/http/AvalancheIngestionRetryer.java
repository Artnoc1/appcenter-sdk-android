package avalanche.base.ingestion.http;

import android.os.Handler;
import android.os.Looper;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import avalanche.base.ingestion.AvalancheIngestion;
import avalanche.base.ingestion.ServiceCall;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.utils.AvalancheLog;

public class AvalancheIngestionRetryer extends AvalancheIngestionDecorator {

    /**
     * Retry intervals to use, array index is to use the value for each retry. When we used all the array values, we give up and forward the last error.
     */
    private static final long[] RETRY_INTERVALS = new long[]{
            TimeUnit.SECONDS.toMillis(10),
            TimeUnit.MINUTES.toMillis(5),
            TimeUnit.MINUTES.toMillis(20)
    };

    /**
     * Retry intervals for this instance.
     */
    private final long[] mRetryIntervals;

    /**
     * Android "timer" using the main thread loop.
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Random object for interval randomness.
     */
    private final Random mRandom = new Random();

    public AvalancheIngestionRetryer(AvalancheIngestion decoratedApi) {
        this(decoratedApi, RETRY_INTERVALS);
    }

    protected AvalancheIngestionRetryer(AvalancheIngestion decoratedApi, long... retryIntervals) {
        super(decoratedApi);
        mRetryIntervals = retryIntervals;
    }

    @Override
    public ServiceCall sendAsync(UUID appKey, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException {

        /* Wrap the call with the retry logic and call delegate. */
        RetryableCall retryableCall = new RetryableCall(mDecoratedApi, appKey, installId, logContainer, serviceCallback);
        retryableCall.run();
        return retryableCall;
    }

    /**
     * Retry wrapper logic.
     */
    private class RetryableCall extends AvalancheIngestionCallDecorator {

        /**
         * Current retry counter. 0 means its the first try.
         */
        private int mRetryCount;

        RetryableCall(AvalancheIngestion decoratedApi, UUID appKey, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) {
            super(decoratedApi, appKey, installId, logContainer, serviceCallback);
        }

        @Override
        public synchronized void cancel() {
            mHandler.removeCallbacks(this);
            super.cancel();
        }

        @Override
        public void failure(Throwable t) {
            if (mRetryCount < mRetryIntervals.length && HttpUtils.isRecoverableError(t)) {
                long delay = mRetryIntervals[mRetryCount++] / 2;
                delay += mRandom.nextInt((int) delay);
                AvalancheLog.warn("Try #" + mRetryCount + " failed and will be retried in " + delay + " ms", t);
                mHandler.postDelayed(this, delay);
            } else
                mServiceCallback.failure(t);
        }
    }
}
