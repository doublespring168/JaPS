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

import de.jackwhite20.japs.server.command.Command;
import de.jackwhite20.japs.server.command.CommandManager;
import de.jackwhite20.japs.server.command.impl.EndCommand;
import de.jackwhite20.japs.server.command.impl.HelpCommand;
import de.jackwhite20.japs.server.command.impl.UnsubCommand;
import de.jackwhite20.japs.server.command.impl.sub.SubCommand;
import de.jackwhite20.japs.server.config.Config;
import de.jackwhite20.japs.server.logging.JaPSLogger;
import de.jackwhite20.japs.server.logging.out.LoggingOutputStream;
import jline.console.ConsoleReader;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class JaPS {

    private static final Pattern ARGS_SPLIT = Pattern.compile(" ");

    private static JaPS instance;

    private Logger logger;

    private ConsoleReader consoleReader;

    private CommandManager commandManager;

    private Config config;

    private JaPSServer jaPSServer;

    private boolean running;

    public JaPS(Config config) {

        this.config = config;

        instance = this;
    }

    public void init() {

        System.setProperty("library.jansi.version", "JaPS");

        AnsiConsole.systemInstall();

        try {
            consoleReader = new ConsoleReader();
            consoleReader.setExpandEvents(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Setup logging system
        logger = new JaPSLogger(consoleReader);
        System.setErr(new PrintStream(new LoggingOutputStream(logger, Level.SEVERE), true));
        System.setOut(new PrintStream(new LoggingOutputStream(logger, Level.INFO), true));

        // Register command
        commandManager = new CommandManager();
        commandManager.addCommand(new HelpCommand("help", new String[]{"h"}, "Show this output"));
        commandManager.addCommand(new SubCommand("sub", new String[]{"s", "subscribe"}, "Subscribe a channel and view it's data passing"));
        commandManager.addCommand(new UnsubCommand("unsub", new String[]{}, "Unsubscribe previous subscribed channels"));
        commandManager.addCommand(new EndCommand("end", new String[]{"stop"}, "Shutdown the server"));

        logger.info("Initialized");
    }

    public void start() throws Exception {

        logger.info("Starting JaPS server");

        running = true;

        jaPSServer = new JaPSServer(config);

        String line;
        while (running) {
            line = consoleReader.readLine("> ");

            if (!line.isEmpty()) {
                String[] split = ARGS_SPLIT.split(line);

                if (split.length == 0) {
                    continue;
                }

                String commandName = split[0].toLowerCase();

                // Try to get the command with the name
                Command command = commandManager.findCommand(commandName);

                if (command != null) {
                    logger.log(Level.INFO, "Executing command: " + command.getName());

                    String[] cmdArgs = Arrays.copyOfRange(split, 1, split.length);
                    command.execute(cmdArgs);
                } else {
                    logger.log(Level.INFO, "Command not found!");
                }
            }
        }
    }

    public void stop() {

        running = false;

        // Try to close faithfully
        jaPSServer.stop();

        // Explicitly exit
        System.exit(0);
    }

    public static JaPS getInstance() {

        return instance;
    }

    public static Logger getLogger() {

        return instance.logger;
    }

    public static CommandManager getCommandManager() {

        return instance.commandManager;
    }
}
