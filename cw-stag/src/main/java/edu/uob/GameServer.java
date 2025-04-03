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

    private static final char END_OF_TRANSMISSION = 4;
    private  EntitiesLoader entitiesLoader;  // 添加成员变量
    private  Map<String, GameAction> actions;  // 添加 actions 变量
    private  Player currentPlayer;
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

        // 获取起始房间
        Room startingRoom = entitiesLoader.getStartingRoom(); // 从EntitiesLoader中获取起始房间
        if (startingRoom == null) {
            throw new IllegalStateException("[GameServer] Error: No valid starting room found! Please check your .dot file.");
        }

        this.currentPlayer = new Player("Player1", startingRoom, entitiesLoader);


        // 添加 actionsLoader
        ActionsLoader actionsLoader = new ActionsLoader();
        actionsLoader.loadActions(actionsFile);
        this.actions = actionsLoader.getActions(); // 获取加载的动作列表

    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * This method handles all incoming game commands and carries out the corresponding actions.</p>
    *
    * @param command The incoming command to be processed
    */
    public String handleCommand(String command) {
        // 1. 预处理命令（去除修饰词，转换小写）
        CommandParser commandParser = new CommandParser(decorativeWords);
        String normalizedCommand = commandParser.normalizeCommand(command);

        // 2. 拆分命令，获取有效的关键词
        Set<String> commandWords = commandParser.extractCommandWords(normalizedCommand);

        // 3. 解析命令
        String actionVerb = this.findActionVerb(commandWords);

        // 4. 提取除了 actionVerb 以外的其它词
        commandWords.remove(actionVerb);

        Iterator<String> subjectIterator = this.findSubjects(commandWords).iterator();

        if (actionVerb == null || (!subjectIterator.hasNext() && !isBuiltinCommand(actionVerb))) {
            return "Invalid command: Missing necessary action or subject.";
        }

        // 5. 处理内置命令
        BuiltinCommandHandler builtinCommandHandler = new BuiltinCommandHandler(currentPlayer);
        if (isBuiltinCommand(actionVerb)) {
            return builtinCommandHandler.handleBuiltinCommand(actionVerb, commandWords);
        }

        // 6. 处理游戏动作
        GameActionHandler actionHandler = new GameActionHandler(actions, currentPlayer, entitiesLoader);
        return actionHandler.handleGameAction(actionVerb, subjectIterator);
    }

    public boolean isBuiltinCommand(String action) {
        // 直接判断是否是内置命令
        return action.equals("look") || action.equals("inventory") || action.equals("inv") || action.equals("get")
                || action.equals("drop") || action.equals("goto")|| action.equals("health");
    }

    public Player getCurrentPlayer() {
        return this.currentPlayer;  // 返回当前玩家对象
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
        // 先检查是否是内置命令
        for (String word : commandWords) {
            if (isBuiltinCommand(word)) {
                return word; // 直接返回内置命令
            }
        }

        // 再检查是否是 XML 里的游戏动作
        for (String word : commandWords) {
            if (actions.containsKey(word)) {
                return word;
            }
        }

        return null;  // 没找到匹配的动作，返回 null
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

                // 使用 StringBuilder 来构建返回的内容
                StringBuilder sb = new StringBuilder();
                sb.append(result);
                sb.append("\n").append(END_OF_TRANSMISSION).append("\n");

                writer.write(sb.toString());  // 发送给客户端
                writer.flush();
            }
        }
    }
}