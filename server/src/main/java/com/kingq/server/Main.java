package com.kingq.server;

import com.google.gson.Gson;
import com.kingq.server.config.Config;
import com.kingq.shared.config.ClusterServer;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by spring on 05.05.2017.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        Config config = null;

        if (args.length > 0) {
            Options options = new Options();
            options.addOption("h", true, "Address to bind to");
            options.addOption("p", true, "Port to bind to");
            options.addOption("b", true, "The backlog");
            options.addOption("t", true, "Worker thread count");
            options.addOption("d", false, "If debug is enabled or not");
            options.addOption("c", true, "Add server as a cluster");
            options.addOption("ci", true, "Sets the cache check interval");
            options.addOption("si", true, "Sets the snapshot interval");

            CommandLineParser commandLineParser = new BasicParser();
            CommandLine commandLine = commandLineParser.parse(options, args);

            if (commandLine.hasOption("h") && commandLine.hasOption("p") && commandLine.hasOption("b") && commandLine.hasOption("t")) {

                List<ClusterServer> clusterServers = new ArrayList<>();

                if (commandLine.hasOption("c")) {
                    for (String c : commandLine.getOptionValues("c")) {
                        String[] splitted = c.split(":");
                        clusterServers.add(new ClusterServer(splitted[0], Integer.parseInt(splitted[1])));
                    }
                }

                config = new Config(commandLine.getOptionValue("h"),
                        Integer.parseInt(commandLine.getOptionValue("p")),
                        Integer.parseInt(commandLine.getOptionValue("b")),
                        commandLine.hasOption("d"),
                        Integer.parseInt(commandLine.getOptionValue("t")),
                        clusterServers,
                        (commandLine.hasOption("ci")) ? Integer.parseInt(commandLine.getOptionValue("ci")) : 300,
                        (commandLine.hasOption("si")) ? Integer.parseInt(commandLine.getOptionValue("si")) : -1);
            } else {
                System.out.println("Usage: java -jar kingQ-server.jar -h <Host> -p <Port> -b <Backlog> -t <Threads> [-c IP:Port IP:Port] [-d]");
                System.out.println("Example (with debugging enabled): java -jar kingQ-server.jar -h localhost -p 1337 -b 100 -t 4 -d");
                System.out.println("Example (with debugging enabled and cluster setup): java -jar kingQ-server.jar -h localhost -p 1337 -b 100 -t 4 -c localhost:1338 -d");
                System.exit(-1);
            }
        } else {
            File configFile = new File("config.json");
            if (!configFile.exists()) {
                try {
                    Files.copy(KingQ.class.getClassLoader().getResourceAsStream("config.json"), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.err.println("Unable to load default config!");
                    System.exit(-1);
                }
            }

            try {
                config = new Gson().fromJson(Files.lines(configFile.toPath()).map(String::toString).collect(Collectors.joining(" ")), Config.class);
            } catch (IOException e) {
                System.err.println("Unable to load 'config.json' in current directory!");
                System.exit(-1);
            }
        }

        if (config == null) {
            System.err.println("Failed to create a Config!");
            System.err.println("Please check the program parameters or the 'config.json' file!");
        } else {
            System.err.println("Using Config: " + config);

            KingQ kingQ = new KingQ(config);
            kingQ.init();
            kingQ.start();
            kingQ.stop();
        }
    }
}
