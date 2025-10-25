package jp.moyashi.phoneos.standalone;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.LoggerService;

import java.awt.AWTEvent;
import java.awt.EventQueue;

/**
 * AWTイベントキューをラップして、イベント投入からディスパッチまでの
 * 遅延や処理時間をログに記録するクラス。
 */
class InstrumentedEventQueue extends EventQueue {

    private static final long DISPATCH_WARN_THRESHOLD_MS = 5000L; // 5秒
    private static final long ENQUEUE_WARN_THRESHOLD_MS = 2000L;  // 2秒

    private final Kernel kernel;
    private final LoggerService logger;

    InstrumentedEventQueue(Kernel kernel) {
        this.kernel = kernel;
        this.logger = kernel != null ? kernel.getLogger() : null;
    }

    @Override
    protected void dispatchEvent(AWTEvent event) {
        long startNs = System.nanoTime();
        long eventWhenMs = -1L;
        if (event instanceof java.awt.event.InputEvent) {
            eventWhenMs = ((java.awt.event.InputEvent) event).getWhen();
        } else if (event instanceof java.awt.event.ActionEvent) {
            eventWhenMs = ((java.awt.event.ActionEvent) event).getWhen();
        }
        long enqueueLatencyMs = -1L;
        if (eventWhenMs > 0) {
            enqueueLatencyMs = System.currentTimeMillis() - eventWhenMs;
        }

        if (enqueueLatencyMs >= ENQUEUE_WARN_THRESHOLD_MS) {
            logWarn("AWTQueue latency before dispatch",
                    event, enqueueLatencyMs);
            // 重大な遅延時にスレッドダンプ
            if (enqueueLatencyMs >= 10000L && logger != null) { // 10秒以上
                logger.error("AWTEventQueue", "CRITICAL DELAY DETECTED - Thread dump:");
                Thread currentThread = Thread.currentThread();
                logger.error("AWTEventQueue", "Current thread: " + currentThread.getName() + " state=" + currentThread.getState());
            }
        }

        try {
            super.dispatchEvent(event);
        } finally {
            long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
            if (durationMs >= DISPATCH_WARN_THRESHOLD_MS) {
                logWarn("AWTQueue dispatch slow",
                        event, durationMs);
            }
        }
    }

    private void logWarn(String prefix, AWTEvent event, long ms) {
        if (logger == null) {
            return;
        }
        String message = String.format(
                "%s type=%s id=%d latency=%dms source=%s",
                prefix,
                event.getClass().getSimpleName(),
                event.getID(),
                ms,
                event.getSource() == null ? "null" : event.getSource().getClass().getSimpleName()
        );
        logger.warn("AWTEventQueue", message);
    }
}
