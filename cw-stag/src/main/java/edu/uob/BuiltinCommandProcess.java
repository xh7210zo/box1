package edu.uob;

import java.util.*;

public class BuiltinCommandProcess {

    private final Player currentPlayer;
    private final GameServer gameServer;

    public BuiltinCommandProcess(Player currentPlayer, GameServer gameServer) {
        this.currentPlayer = currentPlayer;
        this.gameServer = gameServer;
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
                return "what to get?";
            }
        } else if (action.equals("drop")) {
            if (subjectIterator.hasNext()) {
                return this.handleDrop(subjectIterator);
            } else {
                return "what to drop?";
            }
        } else if (action.equals("goto")) {
            if (subjectIterator.hasNext()) {
                return this.handleGoto(subjectIterator);
            } else {
                return "where to go?";
            }
        } else if (action.equals("health")) {
            return this.handleHealth();
        } else {
            return "Unknown built-in command.";
        }
    }

    private String handleHealth() {

        //get current health of the player
        int currentHealth = currentPlayer.getHealth();
        StringBuilder s = new StringBuilder();
        s.append("Your current health is: ").append(currentHealth);
        return s.toString();
    }

    private String handleLook() {

        // get current room of player and the description
        Room currentRoom = currentPlayer.getCurrentRoom();
        String roomDescription = currentRoom.getDescription();

        // get all the entities
        Set<GameEntity> entities = currentRoom.getEntities();
        StringBuilder entityList = new StringBuilder();
        for (GameEntity entity : entities) {
            entityList.append(entity.getName()).append(": ").append(entity.getDescription()).append("\n");
        }

        // get artefacts、furniture 和 characters
        StringBuilder artefactsList = new StringBuilder();
        if (!currentRoom.getArtefacts().isEmpty()) {
            artefactsList.append("Artefacts:\n");
            for (Artefact artefact : currentRoom.getArtefacts()) {
                artefactsList.append("    ").append(artefact.getName()).append(": ").append(artefact.getDescription()).append("\n");
            }
        } else {
            artefactsList.append("No artefacts in this room.\n");
        }

        StringBuilder furnitureList = new StringBuilder();
        if (!currentRoom.getFurniture().isEmpty()) {
            furnitureList.append("Furniture:\n");
            for (Furniture furniture : currentRoom.getFurniture()) {
                furnitureList.append("    ").append(furniture.getName()).append(": ").append(furniture.getDescription()).append("\n");
            }
        } else {
            furnitureList.append("No furniture in this room.\n");
        }

        StringBuilder charactersList = new StringBuilder();
        if (!currentRoom.getCharacters().isEmpty()) {
            charactersList.append("Characters:\n");
            for (Character character : currentRoom.getCharacters()) {
                charactersList.append("    ").append(character.getName()).append(": ").append(character.getDescription()).append("\n");
            }
        } else {
            charactersList.append("No characters in this room.\n");
        }

        // get all the exits in the room
        Set<Room> connectedRooms = currentRoom.getConnectedRooms();
        StringBuilder connectedRoomList = new StringBuilder();
        for (Room room : connectedRooms) {
            connectedRoomList.append(room.getName()).append("\n");
        }

        // get all the players in the room other than self
        StringBuilder playersInRoomList = new StringBuilder();
        boolean hasOtherPlayers = false;

        Set<String> allPlayers = gameServer.getAllPlayers();
        for (String playerName : allPlayers) {
            if (!playerName.equals(currentPlayer.getName())) {
                Player player = gameServer.getPlayer(playerName);
                if (player.getCurrentRoom().equals(currentRoom)) {
                    if (!hasOtherPlayers) {
                        playersInRoomList.append("Other players in the room:\n");
                        hasOtherPlayers = true;
                    }
                    playersInRoomList.append(player.getName()).append("\n");
                }
            }
        }

        if (!hasOtherPlayers) {
            playersInRoomList.append("No other players in the room.\n");
        }

        StringBuilder result = new StringBuilder();
        result.append("You are in: ").append(currentRoom.getName()).append("\n")
                .append(roomDescription).append("\n")
                .append("Entities in this room:\n")
                .append(entityList)
                .append(artefactsList)
                .append(furnitureList)
                .append(charactersList)
                .append("Paths to other rooms:\n")
                .append(connectedRoomList)
                .append(playersInRoomList);

        return result.toString();
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

        StringBuilder s = new StringBuilder();
        s.append("You have picked up the ").append(itemToGet.getName()).append(".");
        return s.toString();
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

        StringBuilder s = new StringBuilder();
        s.append("You dropped: ").append(itemToDrop.getName());
        return s.toString();
    }

    private String handleGoto(Iterator<String> wordIterator) {
        if (!wordIterator.hasNext()) {
            return "Go where?";
        }

        //use iterator to get name of room
        String roomName = wordIterator.next();

        Room currentRoom = currentPlayer.getCurrentRoom();
        Room targetRoom = currentRoom.getExit(roomName);
        //if the room doesn't exist
        if (targetRoom == null) {
            return "You can't go there.";
        }

        currentPlayer.moveTo(targetRoom);

        StringBuilder s = new StringBuilder();
        s.append("You moved to: ").append(targetRoom.getName()).append("\n");
        s.append(targetRoom.describe());
        return s.toString();
    }
}