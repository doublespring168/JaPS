package com.kingq.server.logging;

import jline.console.ConsoleReader;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

public class KingQLogger extends Logger {

    private final Formatter formatter = new ConsoleFormatter();

    private final LogDispatcher dispatcher = new LogDispatcher(this);

    public KingQLogger(ConsoleReader consoleReader) {

        super("KingQ", null);
        setLevel(Level.INFO);

        try {
            File logDir = new File("logs");
            if (!logDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                logDir.mkdir();
            }

            FileHandler fileHandler = new FileHandler("logs" + File.separator + "kingq.log", 1400000, 4, false);
            fileHandler.setFormatter(formatter);
            addHandler(fileHandler);

            ConsoleHandler consoleHandler = new ConsoleHandler(consoleReader);
            consoleHandler.setFormatter(formatter);
            addHandler(consoleHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }

        dispatcher.start();
    }

    @Override
    public void log(LogRecord record) {

        dispatcher.queue(record);
    }

    void doLog(LogRecord record) {

        super.log(record);
    }
}