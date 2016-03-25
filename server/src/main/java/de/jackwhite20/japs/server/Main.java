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

package de.jackwhite20.japs.server;

import com.google.gson.Gson;
import de.jackwhite20.japs.server.config.Config;
import de.jackwhite20.japs.server.logging.ConsoleFormatter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        setUpLogging();

        LOGGER.info("Starting JaPS server");

        // TODO: 25.03.2016 First handle args and after that if config does not exists create a default one
        File config = new File("config.json");
        if(config.exists()) {
            try {
                Config configClass = new Gson().fromJson(Files.lines(config.toPath()).map(String::toString).collect(Collectors.joining(" ")), Config.class);

                LOGGER.log(Level.INFO, "Using Config: {0}", configClass);

                new JaPSServer(configClass);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error while loading config: ", e);
            }
        } else {
            LOGGER.log(Level.INFO, "Unable to find Config file {0} in current directory, using program args", config.getName());
            if(args.length == 2) {
                LOGGER.log(Level.INFO, "Host: {0}", args[0]);
                LOGGER.log(Level.INFO, "Port: {0}", args[1]);

                new JaPSServer(args[0], Integer.parseInt(args[1]), 50);
            } else if(args.length == 3) {
                LOGGER.log(Level.INFO, "Host: {0}", args[0]);
                LOGGER.log(Level.INFO, "Port: {0}", args[1]);
                LOGGER.log(Level.INFO, "Backlog: {0}", args[2]);

                new JaPSServer(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            } else {
                System.out.println("Usage: java -jar japs-server.jar <Host> <Port>");
            }
        }
    }

    private static void setUpLogging() {

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new ConsoleFormatter());

        Logger globalLogger = Logger.getLogger("");
        globalLogger.setLevel(Level.OFF);
        globalLogger.setUseParentHandlers(false);
        for (Handler handler : globalLogger.getHandlers()) {
            globalLogger.removeHandler(handler);
        }

        Logger japsLogger = Logger.getLogger("de.jackwhite20");
        japsLogger.setLevel(Level.INFO);
        japsLogger.addHandler(consoleHandler);
    }
}
