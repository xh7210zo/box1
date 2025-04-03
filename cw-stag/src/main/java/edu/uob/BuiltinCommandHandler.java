package edu.uob;

import java.util.*;

public class BuiltinCommandHandler {
    private final Player currentPlayer;

    public BuiltinCommandHandler(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public String handleBuiltinCommand(String action, Set<String> subjects) {
        if (subjects.size() > 1) {
            return "Too many entities specified.";
        }

        Iterator<String> subjectIterator = subjects.iterator();

        if (action.equals("look")) {
            return this.handleLook();
        } else if (action.equals("inventory") || action.equals("inv")) {
            return this.currentPlayer.listInventory();
        } else if (action.equals("get")) {
            if (subjectIterator.hasNext()) {
                return this.handleGet(this.currentPlayer, subjectIterator);
            } else {
                return "Specify what to get.";
            }
        } else if (action.equals("drop")) {
            if (subjectIterator.hasNext()) {
                return this.handleDrop(subjectIterator);
            } else {
                return "Specify what to drop.";
            }
        } else if (action.equals("goto")) {
            if (subjectIterator.hasNext()) {
                return this.handleGoto(subjectIterator);
            } else {
                return "Specify where to go.";
            }
        } else if (action.equals("health")) {
            return this.handleHealth();
        } else {
            return "Unknown built-in command.";
        }
    }

    private String handleHealth() {
        int currentHealth = currentPlayer.getHealth();  // 获取玩家当前健康值
        StringBuilder sb = new StringBuilder();
        sb.append("Your current health is: ").append(currentHealth);
        return sb.toString();
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

        // 使用 StringBuilder 拼接所有信息
        StringBuilder result = new StringBuilder();
        result.append("You are in: ").append(currentRoom.getName()).append("\n")
                .append(roomDescription).append("\n")
                .append("Entities in this room:\n")
                .append(entityList)
                .append(artefactsList)
                .append(furnitureList)
                .append(charactersList)
                .append("Paths to other rooms:\n")
                .append(connectedRoomList);

        return result.toString();  // 返回拼接后的结果
    }

    public String handleGet(Player currentPlayer, Iterator<String> wordIterator) {
        if (!wordIterator.hasNext()) {
            return "What do you want to get?";
        }

        String itemName = wordIterator.next().toLowerCase();
        Artefact itemToGet = null;
        Room currentRoom = currentPlayer.getCurrentRoom();

        for (Artefact artefact : currentRoom.getArtefacts()) {
            if (artefact.getName().equalsIgnoreCase(itemName)) {
                itemToGet = artefact;
                break;
            }
        }

        if (itemToGet == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("There is no ").append(itemName).append(" here!");
            return sb.toString();
        }

        currentPlayer.addItem(itemToGet);
        currentRoom.removeArtefact(itemToGet);

        StringBuilder sb = new StringBuilder();
        sb.append("You have picked up the ").append(itemToGet.getName()).append(".");
        return sb.toString();
    }

    private String handleDrop(Iterator<String> wordIterator) {
        if (!wordIterator.hasNext()) {
            return "Drop what?";
        }

        String itemName = wordIterator.next();

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

        if (itemToDrop == null) {
            return "You don't have that item.";
        }

        currentPlayer.removeItem(itemToDrop);
        currentPlayer.getCurrentRoom().addArtefact(itemToDrop);

        StringBuilder sb = new StringBuilder();
        sb.append("You dropped: ").append(itemToDrop.getName());
        return sb.toString();
    }

    private String handleGoto(Iterator<String> wordIterator) {
        if (!wordIterator.hasNext()) {
            return "Go where?";  // 玩家没有指定目标房间
        }

        String roomName = wordIterator.next();  // 通过迭代器获取目标房间名称

        Room currentRoom = currentPlayer.getCurrentRoom();
        Room targetRoom = currentRoom.getExit(roomName);
        if (targetRoom == null) {
            return "You can't go there.";  // 如果目标房间不存在
        }

        currentPlayer.moveTo(targetRoom);

        // 使用 StringBuilder 来构建返回字符串
        StringBuilder sb = new StringBuilder();
        sb.append("You moved to: ").append(targetRoom.getName()).append("\n");
        sb.append(targetRoom.describe());  // 添加目标房间的描述
        return sb.toString();
    }
}
