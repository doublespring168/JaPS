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
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Created by JackWhite20 on 05.05.2016.
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
