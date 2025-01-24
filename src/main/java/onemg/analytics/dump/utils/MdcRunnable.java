package onemg.analytics.dump.utils;

import org.apache.log4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MdcRunnable implements Runnable {
    private final Runnable delegate;
    private final Map<String, Object> contextMap;

    public MdcRunnable(Runnable delegate) {
        this.delegate = Objects.requireNonNull(delegate, "Runnable delegate cannot be null");

        // Manually copy MDC context into a map
        this.contextMap = copyMdcContext();
    }

    @Override
    public void run() {
        if (contextMap != null) {
            restoreMdcContext(contextMap); // Restore MDC context in the new thread
        }
        try {
            delegate.run(); // Run the actual task
        } finally {
            MDC.clear(); // Clear MDC to avoid leaking context
        }
    }

    // Helper method to copy the MDC context
    private Map<String, Object> copyMdcContext() {
        Map<String, Object> context = new HashMap<>();
        for (Object key : MDC.getContext().keySet()) { // Access the internal MDC context map
            context.put(String.valueOf(key), MDC.get(String.valueOf(key)));
        }
        return context;
    }

    // Helper method to restore the MDC context
    private void restoreMdcContext(Map<String, Object> contextMap) {
        contextMap.forEach(MDC::put);
    }
}