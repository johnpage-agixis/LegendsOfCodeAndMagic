package com.codingame.gameengine.runner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.codingame.gameengine.runner.dto.GameResult;
import com.google.common.io.Files;

public class CommandLineInterface {
    static String P1_PICTURE 	= "http://localhost:8888/p1.jpg";
    static String P2_PICTURE 	= "http://localhost:8888/p2.jpg";

    public static void main(String[] args) {
        try {
            Options options = new Options();

            // Define required options
            options.addOption("h", false, "Print the help")
                    .addOption("p1", true, "Required. Player 1 command line.")
                    .addOption("p2", true, "Required. Player 2 command line.")
                    .addOption("s", false, "Server mode")
                    .addOption("l", true, "File output for logs")
                    .addOption("d", true, "Referee initial data");

            CommandLine cmd = new DefaultParser().parse(options, args);

            if (cmd.hasOption("h") || !cmd.hasOption("p1") || !cmd.hasOption("p2")) {
                new HelpFormatter().printHelp( "-p1 <player1 command line> -p2 <player2 command line> -l <log_folder>", options);
                System.exit(0);
            }

            // Launch Game
            MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();

            //Choose league level
            Properties gameParameters = new Properties();
            // set game parameters here
            //        gameRunner.setSeed(1279960l);
            //        gameParameters.setProperty("draftChoicesSeed", "-5113144502819146988");
            //        gameParameters.setProperty("shufflePlayer0Seed", "127");
            //        gameParameters.setProperty("shufflePlayer1Seed", "333");
            //        gameParameters.setProperty("predefinedDraftIds", "91 92 93,94 95 96,97 98 99,100 101 102,103 104 105,106 107 108,109 110 111,112 113 114,115 116 117,118 119 120,121 122 123,124 125 126,127 128 129,130 131 132,133 134 135,136 137 138,139 140 141,142 143 144,145 146 147,148 149 150,151 152 153,154 155 156,157 158 159,160 160 160,160 160 160,160 160 160,160 160 160,160 160 160,160 160 160,160 160 160");
            gameRunner.setGameParameters(gameParameters);


            //Add players
            gameRunner.addAgent(cmd.getOptionValue("p1"), "Player1", P1_PICTURE);
            gameRunner.addAgent(cmd.getOptionValue("p2"), "Player2", P2_PICTURE);

            if (cmd.hasOption("d")) {
                String[] parse = cmd.getOptionValue("d").split("=", 0);
                Long seed = Long.parseLong(parse[1]);
                gameRunner.setSeed(seed);
            } else {
                gameRunner.setSeed(System.currentTimeMillis());
            }

            GameResult result = gameRunner.gameResult;

            if (cmd.hasOption("s")) {
                gameRunner.start();
            }

            Method initialize = GameRunner.class.getDeclaredMethod("initialize", Properties.class);
            initialize.setAccessible(true);
            initialize.invoke(gameRunner, new Properties());

            Method runAgents = GameRunner.class.getDeclaredMethod("runAgents");
            runAgents.setAccessible(true);
            runAgents.invoke(gameRunner);

            if (cmd.hasOption("l")) {
                Method getJSONResult = GameRunner.class.getDeclaredMethod("getJSONResult");
                getJSONResult.setAccessible(true);

                Files.asCharSink(Paths.get(cmd.getOptionValue("l")).toFile(), Charset.defaultCharset())
                        .write((String) getJSONResult.invoke(gameRunner));
            }

            for (int i = 0; i < 2; ++i) {
                System.out.println(result.scores.get(i));
            }

            for (String line : result.uinput) {
                System.out.println(line);
            }

            // We have to clean players process properly
            Field getPlayers = GameRunner.class.getDeclaredField("players");
            getPlayers.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Agent> players = (List<Agent>) getPlayers.get(gameRunner);

            if (players != null) {
                for (Agent player : players) {
                    Field getProcess = CommandLinePlayerAgent.class.getDeclaredField("process");
                    getProcess.setAccessible(true);
                    Process process = (Process) getProcess.get(player);

                    process.destroy();
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

} 