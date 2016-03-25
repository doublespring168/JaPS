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
import java.nio.file.StandardCopyOption;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        setUpLogging();

        LOGGER.info("Starting JaPS server");

        Config config = null;

        if(args.length > 0) {
            if(args.length == 2) {
                config = new Config(args[0], Integer.parseInt(args[1]), 50);
            } else if(args.length == 3) {
                config = new Config(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            } else {
                System.out.println("Usage: java -jar japs-server.jar <Host> <Port> [Backlog]");
                System.exit(-1);
            }
        } else {
            File configFile = new File("config.json");
            if(!configFile.exists()) {
                try {
                    Files.copy(Main.class.getClassLoader().getResourceAsStream("config.json"), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Unable to load default config!", e);
                    System.exit(-1);
                }
            }

            try {
                config = new Gson().fromJson(Files.lines(configFile.toPath()).map(String::toString).collect(Collectors.joining(" ")), Config.class);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Unable to load 'config.json' in current directory!");
                System.exit(-1);
            }
        }

        if(config == null) {
            LOGGER.log(Level.SEVERE, "Failed to create a Config!");
            LOGGER.log(Level.SEVERE, "Please check the program parameters or the 'config.json' file!");
        } else {
            LOGGER.log(Level.INFO, "Using Config: {0}", config);

            new JaPSServer(config);
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
