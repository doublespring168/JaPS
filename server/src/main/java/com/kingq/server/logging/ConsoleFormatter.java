package com.kingq.server.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Created by spring on 25.03.2017.
 */
public class ConsoleFormatter extends Formatter {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    public String format(LogRecord record) {

        StringBuilder builder = new StringBuilder();
        builder.append("[").append(simpleDateFormat.format(new Date(record.getMillis()))).append("] ");
        builder.append("[").append(record.getLevel()).append("] ");
        builder.append(formatMessage(record));
        //noinspection ThrowableResultOfMethodCallIgnored
        Throwable throwable = record.getThrown();
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            String exceptionString = sw.toString();
            builder.append(exceptionString);
        }
        builder.append("\n");

        return builder.toString();
    }
}
