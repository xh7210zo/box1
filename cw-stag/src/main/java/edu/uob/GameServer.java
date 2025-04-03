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
    private EntitiesLoader entitiesLoader;  // 添加成员变量
    private ActionsLoader actionsLoader;   // 添加 actionsLoader
    private Map<String, GameAction> actions;  // 添加 actions 变量
    private Player currentPlayer;
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

        this.currentPlayer = new Player("Player1", startingRoom);

        this.actionsLoader = new ActionsLoader();
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
        System.out.println("Received command: " + command);
        String normalizedCommand = normalizeCommand(command);
        System.out.println("Normalized Command : " + normalizedCommand);

        // 2. 拆分命令，获取有效的关键词
        String[] words = normalizedCommand.substring(normalizedCommand.lastIndexOf(":") + 1).trim().split("\\s+");
        if (words.length == 0) {
            System.out.println("Invalid command: No action or subjects found.");
            return "Invalid command.";
        }

        // 3. 解析命令
        Set<String> commandWords = new HashSet<>(Arrays.asList(words)); // 转换为 Set 便于查找
        System.out.println("Command words split into set: " + commandWords);

        // 先检查是否是内置命令
        String actionVerb;
        actionVerb = findActionVerb(commandWords);

        System.out.println("Action verb found: " + actionVerb);

        // 4. 提取除了 actionVerb 以外的其它词
        List<String> things = new ArrayList<>(commandWords);
        things.remove(actionVerb); // 从 things 中移除动作词 actionVerb
        System.out.println("Things found (other than action verb): " + things);

        List<String> subjects = findSubjects(commandWords);
        System.out.println("Subjects found: " + subjects);

        if (actionVerb == null || (subjects.isEmpty() && !isBuiltinCommand(actionVerb))) {
            System.out.println("Invalid command: Missing necessary action or subject.");
            return "Invalid command: Missing necessary action or subject.";
        }

        // 4. 处理内置命令
        if (isBuiltinCommand(actionVerb)) {
            System.out.println("Detected built-in command: " + actionVerb);
            return handleBuiltinCommand(actionVerb, things);
        }

        // 5. 处理游戏动作
        System.out.println("Handling game action: " + actionVerb);
        return handleGameAction(actionVerb, subjects);
    }

    public boolean isBuiltinCommand(String action) {
        // 直接判断是否是内置命令
        return action.equals("look") || action.equals("inventory") || action.equals("inv") || action.equals("get")
                || action.equals("drop") || action.equals("goto")|| action.equals("health");
    }


    public String normalizeCommand(String command) {
        // 将命令转为小写并分割成单词
        command = command.toLowerCase();

        // 去除修饰词
        List<String> words = new ArrayList<>(Arrays.asList(command.split("\\s+")));
        words.removeAll(decorativeWords);

        return String.join(" ", words);
    }

    private List<String> findSubjects(Set<String> commandWords) {
        List<String> subjects = new ArrayList<>();

        for (String word : commandWords) {
            if (entitiesLoader.getGameEntities().contains(word)) {
                subjects.add(word);
            }
        }
        System.out.println("在findsubjects里的subjects: " + subjects);
        return subjects;
    }


    private String findActionVerb(Set<String> commandWords) {
        // 先检查是否是内置命令
        for (String word : commandWords) {
            if (isBuiltinCommand(word)) {
                System.out.println("在findActionVerb里找到 内置命令: " + word);
                return word; // 直接返回内置命令
            }
        }

        // 再检查是否是 XML 里的游戏动作
        for (String word : commandWords) {
            if (actions.containsKey(word)) {
                System.out.println("在findActionVerb里找到 游戏动作: " + word);
                return word;
            }
        }

        System.out.println("在findActionVerb里未找到 actionVerb");
        return null;  // 没找到匹配的动作，返回 null
    }

    public String handleBuiltinCommand(String action, List<String> subjects) {
        // 打印传入的命令和参数
        System.out.println("Handling built-in command: " + action);
        System.out.println("Subjects passed: " + subjects);

        if (subjects.size() > 1) return "Too many entities specified.";

        switch (action) {
            case "look":
                System.out.println("Executing 'look' command.");
                return this.handleLook();

            case "inventory":
            case "inv":
                System.out.println("Executing 'inventory' command.");
                return this.currentPlayer.listInventory();

            case "get":
                System.out.println("Executing 'get' command.");
                if (subjects.isEmpty()) {
                    return "Specify what to get.";
                } else {
                    // 打印获取的物品
                    System.out.println("Getting: " + subjects.get(0));
                    return this.handleGet(this.currentPlayer, new String[]{"get",subjects.get(0)});
                }

            case "drop":
                System.out.println("Executing 'drop' command.");
                if (subjects.isEmpty()) {
                    return "Specify what to drop.";
                } else {
                    // 打印要丢弃的物品
                    System.out.println("Dropping: " + subjects.get(0));
                    return this.handleDrop(new String[]{"drop",subjects.get(0)});
                }

            case "goto":
                System.out.println("Executing 'goto' command.");
                if (subjects.isEmpty()) {
                    return "Specify where to go.";
                } else {
                    // 打印目标房间/位置
                    System.out.println("Going to: " + subjects.get(0));
                    return this.handleGoto(new String[]{"goto",subjects.get(0)});
                }
            case "health":  // ➕ 新增健康值检查命令
                System.out.println("Executing 'health' command.");
                return this.handleHealth();

            default:
                System.out.println("Unknown built-in command: " + action);
                return "Unknown built-in command.";
        }
    }




    private String handleGameAction(String actionVerb, List<String> subjects) {
        if (!actions.containsKey(actionVerb)) {
            return "Invalid action.";
        }

        // 获取单个 GameAction
        GameAction action = actions.get(actionVerb);
        System.out.println("[DEBUG] Handling action: " + actionVerb);

        // 输出动作的触发者和消耗的实体
        System.out.println("[DEBUG] Subjects: " + subjects);
        System.out.println("[DEBUG] Consumed entities: " + action.getConsumed());

        if (subjects.containsAll(action.getSubjects())) {
            System.out.println("[DEBUG] Action subjects matched, executing action...");
            return executeGameAction(action);
        }

        System.out.println("[DEBUG] Subjects did not match.");
        return "You can't do that right now.";
    }

    private String executeGameAction(GameAction action) {
        Room currentRoom = currentPlayer.getCurrentRoom();
        System.out.println("[DEBUG] Executing game action in room: " + currentRoom.getName());

        // **检查 Action 作用的对象 (Subjects) 是否在当前房间/玩家身上**
        for (String subject : action.getSubjects()) {
            if (!currentRoom.hasEntity(subject) && !currentPlayer.hasItem(subject) && !entitiesLoader.getRooms().containsKey(subject)) {
                System.out.println("[DEBUG] Subject not found in current room, inventory, or as a room: " + subject);
                return "You don't see " + subject + " here.";
            }
        }

        // **检查要消耗的实体 (Consumed) 是否存在**
        for (String entity : action.getConsumed()) {
            if (!entity.equalsIgnoreCase("health") && !currentRoom.hasEntity(entity) &&
                    !currentPlayer.hasItem(entity) && !entitiesLoader.getRooms().containsKey(entity)) {
                System.out.println("[DEBUG] Entity not found in current room, inventory, or as a room: " + entity);
                return "You don't see " + entity + " here.";
            }
        }

        // **检查要生成的实体 (Produced) 是否有效**
        for (String entity : action.getProduced()) {
            if (!entity.equalsIgnoreCase("health") && !currentRoom.hasEntity(entity) &&
                    entitiesLoader.getEntityByName(entity) == null && !entitiesLoader.getRooms().containsKey(entity)) {
                System.out.println("[DEBUG] Entity cannot be produced here: " + entity);
                return "You cannot create " + entity + " here.";
            }
        }

        // **处理健康变化**
        if (action.getConsumed().contains("health")) {
            System.out.println("[DEBUG] Health is being consumed...");
            currentPlayer.decreaseHealth(1);
        }
        if (action.getProduced().contains("health")) {
            System.out.println("[DEBUG] Health is being restored...");
            currentPlayer.increaseHealth(1);
        }

        // **处理消耗的实体**
        for (String entity : action.getConsumed()) {
            if (entity.equalsIgnoreCase("health")) continue; // 🛑 已处理 health，跳过

            System.out.println("[DEBUG] Checking consumed entity: " + entity);

            if (entitiesLoader.getRooms().containsKey(entity)) {
                // **如果消耗的是一个房间**
                System.out.println("[DEBUG] Removing room exit: " + entity);
                currentRoom.removeExit(entity); // 从当前房间的出口中移除
            } else {
                // 先检查玩家背包
                Artefact artefactToRemove = null;
                Iterator<Artefact> it = currentPlayer.getInventoryIterator();
                while (it.hasNext()) {
                    Artefact artefact = it.next();
                    if (artefact.getName().equalsIgnoreCase(entity)) {
                        artefactToRemove = artefact;
                        break;
                    }
                }

                if (artefactToRemove != null) {
                    System.out.println("[DEBUG] Found artefact in inventory: " + artefactToRemove.getName());
                    currentPlayer.removeItem(artefactToRemove);
                } else if (currentRoom.hasEntity(entity)) {
                    System.out.println("[DEBUG] Found entity in room: " + entity);
                    currentRoom.removeEntityByName(entity);
                }
            }
        }

        // **处理生成的实体**
        for (String entity : action.getProduced()) {
            if (entity.equalsIgnoreCase("health")) continue; // 🛑 已处理 health，跳过

            System.out.println("[DEBUG] Producing entity: " + entity);

            if (entitiesLoader.getRooms().containsKey(entity)) {
                // **如果生成的是一个房间**
                Room newRoom = entitiesLoader.getRooms().get(entity);
                currentRoom.addExit(entity, newRoom); // 将新房间添加为当前房间的出口
                System.out.println("[DEBUG] Created new exit to room: " + newRoom.getName());
            } else {
                // 否则尝试通过实体加载器获取物品
                GameEntity newEntity = entitiesLoader.getEntityByName(entity);
                if (newEntity != null) {
                    System.out.println("[DEBUG] Adding entity to room: " + newEntity.getName());
                    currentRoom.addEntity(newEntity);
                }
            }
        }

        System.out.println("[DEBUG] Action narration: " + action.getNarration());
        return action.getNarration(); // 返回描述信息
    }





    private String handleHealth() {
        int currentHealth = currentPlayer.getHealth();  // 获取玩家当前健康值
        return "Your current health is: " + currentHealth;
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
        System.out.println("[DEBUG] Current room: " + currentRoom.getName());
        System.out.println("[DEBUG] Entities in room: " + currentRoom.getEntities());
        // 组合所有信息，返回给玩家
        return "You are in: " + currentRoom.getName() + "\n" +
                roomDescription + "\n" +
                "Entities in this room:\n" +
                entityList +
                artefactsList +
                furnitureList +
                charactersList +
                "Paths to other rooms:\n" +
                connectedRoomList;
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
