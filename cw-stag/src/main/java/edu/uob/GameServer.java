package edu.uob;

import javax.swing.text.html.parser.Entity;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;



public final class GameServer {

    private static final char END_OF_TRANSMISSION = 4;
    private Player currentPlayer;
    private Map<String, Room> rooms; // 存储所有房间
    private Room storeroom;
    private Map<String, GameAction> actions; // 存储所有的动作

    public static void main(String[] args) throws IOException {
        StringBuilder entitiesPath = new StringBuilder();
        entitiesPath.append("config");
        entitiesPath.append(File.separator);
        entitiesPath.append("basic-entities.dot");

        StringBuilder actionsPath = new StringBuilder();
        actionsPath.append("config");
        actionsPath.append(File.separator);
        actionsPath.append("basic-actions.xml");

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
        EntitiesLoader entitiesLoader = new EntitiesLoader();
        entitiesLoader.loadEntities(entitiesFile);
        this.rooms = entitiesLoader.getRooms(); // 获取加载的房间列表
        this.storeroom = entitiesLoader.getStoreroom(); // 获取 storeroom

        // 获取起始房间
        Room startingRoom = entitiesLoader.getStartingRoom(); // 从EntitiesLoader中获取起始房间
        if (startingRoom == null) {
            throw new IllegalStateException("[GameServer] Error: No valid starting room found! Please check your .dot file.");
        }

        this.currentPlayer = new Player("Player1", startingRoom);

        ActionsLoader actionsLoader = new ActionsLoader();
        actionsLoader.loadActions(actionsFile);
        this.actions = actionsLoader.getActions(); // 获取加载的动作列表


    }


    // 处理动作命令
    public String handleAction(String actionCommand) {
        // 找到匹配的动作
        GameAction action = actions.get(actionCommand.toLowerCase());
        if (action == null) {
            return "无法识别的动作";
        }

        // 执行动作，返回动作的叙述
        return action.getNarration();
    }


    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * This method handles all incoming game commands and carries out the corresponding actions.</p>
    *
    * @param command The incoming command to be processed
    */
    public String handleCommand(String command) {
        // TODO implement your server logic here
        // 先去除前后空白，再根据冒号分割命令
        String[] words = command.toLowerCase().trim().split(":");

        // 如果命令没有动作部分，返回无效命令
        if (words.length == 0 || words[words.length - 1].trim().isEmpty()) return "Invalid command.";

        // 获取动作部分，忽略冒号之前的部分
        String input = words[words.length - 1].trim(); // 获取冒号后的部分
        String[] word = input.split(" ");
        String action = word[0];


        switch (action) {
            case "look":
                return this.handleLook(); // 使用 this 调用方法
            case "inventory":
            case "inv":
                return this.currentPlayer.listInventory(); // 调用 currentPlayer 的方法，当前的 this 可以省略
            case "get":
                return this.handleGet(this.currentPlayer, word); // 使用 this 调用 handleGet 方法
            case "drop":
                return this.handleDrop(word); // 使用 this 调用 handleDrop 方法
            case "goto":
                return this.handleGoto(word); // 使用 this 调用 handleGoto 方法
            default:
                return this.handleAction(action); // 使用 this 调用 handleAction 方法
        }
    }

    private String handleLook() {
        // 获取当前玩家所在的房间
        Room currentRoom = currentPlayer.getCurrentRoom();

        // 获取房间的描述
        String roomDescription = currentRoom.getDescription();

        // 获取该房间的所有实体（物体或 NPC 等）
        Set<GameEntity> entities = currentRoom.getEntities();
        StringBuilder entityList = new StringBuilder();
        for (GameEntity entity : entities) {
            entityList.append(entity.getName()).append(": ").append(entity.getDescription()).append("\n");
        }

        // 获取该房间的所有 artefacts、furniture 和 characters
        StringBuilder artefactsList = new StringBuilder();
        if (!currentRoom.getArtefacts().isEmpty()) {
            artefactsList.append("Artefacts:\n");
            for (Artefact artefact : currentRoom.getArtefacts()) {
                artefactsList.append("    ").append(artefact.getName()).append(": ").append(artefact.getDescription()).append("\n");
            }
        } else {
            artefactsList.append("[EntitiesLoader] No artefacts in this room.\n");
        }

        StringBuilder furnitureList = new StringBuilder();
        if (!currentRoom.getFurniture().isEmpty()) {
            furnitureList.append("Furniture:\n");
            for (Furniture furniture : currentRoom.getFurniture()) {
                furnitureList.append("    ").append(furniture.getName()).append(": ").append(furniture.getDescription()).append("\n");
            }
        } else {
            furnitureList.append("[EntitiesLoader] No furniture in this room.\n");
        }

        StringBuilder charactersList = new StringBuilder();
        if (!currentRoom.getCharacters().isEmpty()) {
            charactersList.append("Characters:\n");
            for (Character character : currentRoom.getCharacters()) {
                charactersList.append("    ").append(character.getName()).append(": ").append(character.getDescription()).append("\n");
            }
        } else {
            charactersList.append("[EntitiesLoader] No characters in this room.\n");
        }

        // 获取当前房间的所有连接（通向其他房间的路径）
        Set<Room> connectedRooms = currentRoom.getConnectedRooms(); // 不再强制转换为 List
        StringBuilder connectedRoomList = new StringBuilder();
        for (Room room : connectedRooms) {
            connectedRoomList.append(room.getName()).append("\n");
        }

        // 组合所有信息，返回给玩家
        return "You are in: " + currentRoom.getName() + "\n" +
                roomDescription + "\n" +
                "Entities in this room:\n" +
                entityList.toString() +
                artefactsList.toString() +
                furnitureList.toString() +
                charactersList.toString() +
                "Paths to other rooms:\n" +
                connectedRoomList.toString();
    }



    public String handleGet(Player currentPlayer, String[] word) {
        if (word.length < 2) {
            return "What do you want to get?";  // 玩家没有指定物品名时
        }

        String itemName = word[1].toLowerCase();  // 获取物品名
        Artefact itemToGet = null;

        // 获取当前房间
        Room currentRoom = currentPlayer.getCurrentRoom();

        // 查找物品是否在当前房间内
        for (Artefact artefact : currentRoom.getArtefacts()) {
            if (artefact.getName().equalsIgnoreCase(itemName)) {
                itemToGet = artefact;
                break;
            }
        }

        // 如果物品未找到
        if (itemToGet == null) {
            return "There is no " + itemName + " here!";  // 提示物品不在房间
        }

        // 将物品添加到玩家的背包
        currentPlayer.addItem(itemToGet);

        // 从房间移除该物品
        currentRoom.removeArtefact(itemToGet);

        return "You have picked up the " + itemToGet.getName() + ".";  // 成功拾取物品
    }



    private String handleDrop(String[] word) {
        if (word.length < 2) return "Drop what?";
        String itemName = word[1];

        if (!currentPlayer.hasItem(itemName)) {
            return "You don't have that item.";
        }

        Iterator<Artefact> it = currentPlayer.getInventoryIterator();
        Artefact itemToDrop = null;
        while (it.hasNext()) {
            Artefact item = it.next();
            if (item.getName().equalsIgnoreCase(itemName)) {
                itemToDrop = item;
                break;
            }
        }

        if (itemToDrop == null) return "You don't have that item.";

        currentPlayer.removeItem(itemToDrop);
        currentPlayer.getCurrentRoom().addArtefact(itemToDrop);
        return "You dropped: " + itemToDrop.getName();
    }

    private String handleGoto(String[] word) {
        if (word.length < 2) return "Go where?";
        String roomName = word[1];

        Room currentRoom = currentPlayer.getCurrentRoom();
        Room targetRoom = currentRoom.getExit(roomName);
        if (targetRoom == null) {
            return "You can't go there.";
        }

        currentPlayer.moveTo(targetRoom);
        return "You moved to: " + targetRoom.getName() + "\n" + targetRoom.describe();
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
                    blockingHandleConnection(s);
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
            if(incomingCommand != null) {
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}
