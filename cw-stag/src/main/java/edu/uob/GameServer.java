package edu.uob;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public final class GameServer {

    //initialize variables
    private static final char END_OF_TRANSMISSION = 4;
    private final EntitiesLoader entitiesLoader;
    private final Map<String, GameAction> actions;
    private final Map<String, Player> players = new HashMap<>();
    Set<String> decorativeWords = new HashSet<>(Arrays.asList("please", "the", "using", "with", "to"));

    public static void main(String[] args) throws IOException {
        StringBuilder entitiesPath = new StringBuilder();
        entitiesPath.append("config");
        entitiesPath.append(File.separator);
        entitiesPath.append("extended-entities.dot");

        StringBuilder actionsPath = new StringBuilder();
        actionsPath.append("config");
        actionsPath.append(File.separator);
        actionsPath.append("extended-actions.xml");

        File entitiesFile = new File(entitiesPath.toString()).getAbsoluteFile();
        File actionsFile = new File(actionsPath.toString()).getAbsoluteFile();

        GameServer server = new GameServer(entitiesFile, actionsFile);
        server.blockingListenOn(8888);
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Instanciates a new server instance, specifying a game with some configuration files
    *
    * @param entitiesFile The game configuration file containing all game entities to use in your game
    * @param actionsFile The game configuration file containing all game actions to use in your game
    */
    public GameServer(File entitiesFile, File actionsFile) {
        // TODO implement your server logic here
        this.entitiesLoader = new EntitiesLoader();
        entitiesLoader.loadEntities(entitiesFile);

        // get startingRoom in EntitiesLoader
        Room startingRoom = entitiesLoader.getStartingRoom();
        if (startingRoom == null) {
            throw new IllegalStateException("[GameServer] Error: No valid starting room found! Please check your .dot file.");
        }

        String initialPlayerName = "simon";
        Player initialPlayer = new Player(initialPlayerName, startingRoom, entitiesLoader);
        players.put(initialPlayerName, initialPlayer);

        // add actionsLoader and get action list
        ActionsLoader actionsLoader = new ActionsLoader();
        actionsLoader.loadActions(actionsFile);
        this.actions = actionsLoader.getActions();
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * This method handles all incoming game commands and carries out the corresponding actions.</p>
    *
    * @param command The incoming command to be processed
    */
    public String handleCommand(String command) {
        String playerName = this.extractPlayerName(command);
        if (!isValidPlayerName(playerName)) {
            return "Invalid player name.";
        }

        // Retrieve or create the player
        Player player = players.get(playerName);
        if (player == null) {
            player = this.createNewPlayer(playerName);
            players.put(playerName, player);
        }

        // 1. Remove modifiers and convert to lowercase
        CommandParser commandParser = new CommandParser(decorativeWords);
        String normalizedCommand = commandParser.normalizeCommand(command);

        // 2. Split the command to get valid keywords
        Set<String> commandWords = commandParser.extractCommandWords(normalizedCommand);

        // 3. Parse command to find the action verb
        String actionVerb = this.findActionVerb(commandWords);

        // 4. Get words other than the action verb (subjects)
        commandWords.remove(actionVerb);
        Iterator<String> subjectIterator = this.findSubjects(commandWords).iterator();

        if (actionVerb == null || (!subjectIterator.hasNext() && !isBuiltinCommand(actionVerb))) {
            return "Invalid command: Missing necessary action or subject.";
        }

        // 5. Handle built-in commands
        BuiltinCommandProcess builtinCommandHandler = new BuiltinCommandProcess(player,this);
        if (isBuiltinCommand(actionVerb)) {
            return builtinCommandHandler.handleBuiltinCommand(actionVerb, commandWords);
        }

        // 6. Handle game actions with partial subject matching
        GameActionProcess actionHandler = new GameActionProcess(actions, player, entitiesLoader);
        return actionHandler.handleGameAction(actionVerb, subjectIterator);
    }

    private String extractPlayerName(String command) {
        int colonIndex = command.indexOf(":");
        if (colonIndex == -1) {
            return null;  // No colon means no player name
        }
        return command.substring(0, colonIndex).trim();
    }

    private boolean isValidPlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        for (char c : playerName.toCharArray()) {
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == ' ' || c == '\'' || c == '-')) {
                return false;
            }
        }
        return true;
    }

    private Player createNewPlayer(String playerName) {
        Room startingRoom = entitiesLoader.getStartingRoom();
        return new Player(playerName, startingRoom, entitiesLoader);
    }

    public boolean isBuiltinCommand(String action) {
        // check if it is built-in command
        return action.equals("look") || action.equals("inventory") || action.equals("inv") || action.equals("get")
                || action.equals("drop") || action.equals("goto")|| action.equals("health");
    }

    private Set<String> findSubjects(Set<String> commandWords) {
        Set<String> subjects = new HashSet<>();

        for (String word : commandWords) {
            if (entitiesLoader.getGameEntities().contains(word)) {
                subjects.add(word);
            }
        }
        return subjects;
    }

    private String findActionVerb(Set<String> commandWords) {
        // check if its built-in command
        for (String word : commandWords) {
            if (isBuiltinCommand(word)) {
                return word;
            }
        }

        // check if its action in .xml file
        for (String word : commandWords) {
            if (actions.containsKey(word)) {
                return word;
            }
        }
        return null;
    }

    public Set<String> getAllPlayers() {
        return players.keySet();
    }

    public Player getPlayer(String playerName) {
        return players.get(playerName);
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Starts a *blocking* socket server listening for new connections.
    *
    * @param portNumber The port to listen on.
    * @throws IOException If any IO related operation fails.
    */
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            while (!Thread.interrupted()) {
                try {
                    this.blockingHandleConnection(s);
                } catch (IOException e) {
                    System.out.println("Connection closed");
                }
            }
        }
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Handles an incoming connection from the socket server.
    *
    * @param serverSocket The client socket to read/write from.
    * @throws IOException If any IO related operation fails.
    */
    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            System.out.println("Connection established");

            String incomingCommand = reader.readLine();
            if (incomingCommand != null) {
                String result = this.handleCommand(incomingCommand);

                StringBuilder sb = new StringBuilder();
                sb.append(result);
                sb.append("\n").append(END_OF_TRANSMISSION).append("\n");

                writer.write(sb.toString());
                writer.flush();
            }
        }
    }
}