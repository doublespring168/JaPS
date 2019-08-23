package com.kingq.server;

import com.kingq.client.sub.Subscriber;
import com.kingq.server.command.Command;
import com.kingq.server.command.CommandManager;
import com.kingq.server.command.impl.*;
import com.kingq.server.command.impl.sub.SubCommand;
import com.kingq.server.config.Config;
import com.kingq.server.logging.KingQLogger;
import com.kingq.server.logging.out.LoggingOutputStream;
import jline.console.ConsoleReader;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by spring on 25.03.2017.
 */
public class KingQ {

    private static final Pattern ARGS_SPLIT = Pattern.compile(" ");

    private static KingQ instance;

    private Logger logger;

    private ConsoleReader consoleReader;

    private CommandManager commandManager;

    private Config config;

    private KingQServer kingQServer;

    private boolean running;

    public KingQ(Config config) {

        this.config = config;

        instance = this;
    }

    public static KingQ getInstance() {

        return instance;
    }

    public static Logger getLogger() {

        return instance.logger;
    }

    public static CommandManager getCommandManager() {

        return instance.commandManager;
    }

    public static Config getConfig() {

        return instance.config;
    }

    public static KingQServer getServer() {

        return instance.kingQServer;
    }

    public void init() {

        System.setProperty("library.jansi.version", "KingQ");

        AnsiConsole.systemInstall();

        try {
            consoleReader = new ConsoleReader();
            consoleReader.setExpandEvents(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Setup logging system
        logger = new KingQLogger(consoleReader);
        System.setErr(new PrintStream(new LoggingOutputStream(logger, Level.SEVERE), true));
        System.setOut(new PrintStream(new LoggingOutputStream(logger, Level.INFO), true));

        // Register command
        commandManager = new CommandManager();
        commandManager.addCommand(new HelpCommand("help", new String[]{"h"}, "Show this output"));
        commandManager.addCommand(new SubCommand("sub", new String[]{"s", "subscribe"}, "Subscribe a channel and view it's data passing"));
        commandManager.addCommand(new UnsubCommand("unsub", new String[]{}, "Unsubscribe previous subscribed channels"));
        commandManager.addCommand(new EndCommand("end", new String[]{"stop"}, "Shutdown the server"));
        commandManager.addCommand(new SetCommand("set", new String[]{"add"}, "Sets a key and value"));
        commandManager.addCommand(new GetCommand("get", new String[]{}, "Gets a value from a key"));
        commandManager.addCommand(new DeleteCommand("delete", new String[]{"remove", "del", "rm"}, "Gets a value from a key"));
        commandManager.addCommand(new SnapshotCommand("snapshot", new String[]{}, "Takes a memory snapshot"));

        // Autocomplete commands
        consoleReader.addCompleter(new StringsCompleter(commandManager.getCommands().stream().map(Command::getName).collect(Collectors.toList())));

        // Autocomplete files
        consoleReader.addCompleter(new FileNameCompleter());

        logger.info("Initialized");
    }

    public void start() throws Exception {

        logger.info("Starting KingQ server");

        running = true;

        kingQServer = new KingQServer(config);

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
                    logger.log(Level.INFO, "Executing command: {0}", line);

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

        // Disconnect all self subscriptions
        for (Subscriber subscriber : SubCommand.SUBSCRIBERS) {
            subscriber.disconnect(true);
        }

        // Try to close faithfully
        kingQServer.stop();

        // Explicitly exit
        System.exit(0);
    }
}
