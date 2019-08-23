package com.kingq.server.logging;

import jline.console.ConsoleReader;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Created by spring on 05.05.2017.
 */
public class ConsoleHandler extends Handler {

    private ConsoleReader console;

    public ConsoleHandler(ConsoleReader console) {

        this.console = console;
    }

    @Override
    public void publish(LogRecord record) {

        if (isLoggable(record)) {
            print(getFormatter().format(record));
        }
    }

    public void print(String s) {

        try {
            console.print(Ansi.ansi().eraseLine(Ansi.Erase.ALL).toString() + ConsoleReader.RESET_LINE + s + Ansi.ansi().reset().toString());
            console.drawLine();
            console.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
