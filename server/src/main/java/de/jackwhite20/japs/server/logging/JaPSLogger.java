/*
 * Copyright (c) 2016 "JackWhite20"
 *
 * This file is part of JaPS.
 *
 * JaPS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.jackwhite20.japs.server.logging;

import jline.console.ConsoleReader;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

public class JaPSLogger extends Logger {

    private final Formatter formatter = new ConsoleFormatter();

    private final LogDispatcher dispatcher = new LogDispatcher(this);

    public JaPSLogger(ConsoleReader consoleReader) {

        super("JaPS", null);
        setLevel(Level.INFO);

        try {
            File logDir = new File("logs");
            if (!logDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                logDir.mkdir();
            }

            FileHandler fileHandler = new FileHandler("logs" + File.separator + "japs.log", 1400000, 4, false);
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