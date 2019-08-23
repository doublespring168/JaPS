package com.kingq.server.logging;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.LogRecord;

public class LogDispatcher extends Thread {

    private final KingQLogger logger;

    private final BlockingQueue<LogRecord> queue = new LinkedBlockingQueue<>();

    public LogDispatcher(KingQLogger logger) {

        super("KingQ Logger Thread");
        this.logger = logger;
    }

    @Override
    public void run() {

        while (!isInterrupted()) {
            LogRecord record;
            try {
                record = queue.take();
            } catch (InterruptedException ex) {
                continue;
            }

            logger.doLog(record);
        }

        queue.forEach(logger::doLog);
    }

    public void queue(LogRecord record) {

        if (!isInterrupted()) {
            queue.add(record);
        }
    }
}