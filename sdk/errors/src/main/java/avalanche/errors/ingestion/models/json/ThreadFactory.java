package avalanche.errors.ingestion.models.json;

import java.util.ArrayList;
import java.util.List;

import avalanche.core.ingestion.models.json.ModelFactory;
import avalanche.errors.ingestion.models.Thread;

public class ThreadFactory implements ModelFactory<Thread> {

    private static final ThreadFactory sInstance = new ThreadFactory();

    private ThreadFactory() {
    }

    public static ThreadFactory getInstance() {
        return sInstance;
    }

    @Override
    public Thread create() {
        return new Thread();
    }

    @Override
    public List<Thread> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
