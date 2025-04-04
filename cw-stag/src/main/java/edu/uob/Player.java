package edu.uob;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Player extends Character {
    private Room currentRoom;
    private final Set<Artefact> inventory;
    private int health;
    private final EntitiesLoader entitiesLoader;

    public Player(String name, Room startingRoom, EntitiesLoader entitiesLoader) {
        super(name, "A brave adventurer");
        if (startingRoom == null) {
            throw new IllegalArgumentException("Starting room cannot be null!");
        }
        this.currentRoom = startingRoom;
        this.inventory = new HashSet<>();
        this.health = 3;
        this.entitiesLoader = entitiesLoader;
    }

    public Room getCurrentRoom() {
        if (currentRoom == null) {
            throw new IllegalStateException("Player's current room is not set!");
        }
        return currentRoom;
    }

    public void moveTo(Room newRoom) {
        if (newRoom == null) {
            throw new IllegalArgumentException("New room cannot be null!");
        }
        this.currentRoom = newRoom;
    }

    public void addItem(Artefact item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null!");
        }
        inventory.add(item);
    }

    public void removeItem(Artefact item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null!");
        }
        inventory.remove(item);
    }

    public boolean hasItem(String name) {
        for (Artefact item : inventory) {
            if (item.getName().equalsIgnoreCase(name)) {
                return false;
            }
        }
        //if dont find the matched item
        return true;
    }

    public int getHealth() {
        return health;
    }

    //the maximum of health is 3
    public void increaseHealth(int amount) {
        health = Math.min(3, health + amount);
    }

    public void decreaseHealth(int amount) {
        health = Math.max(0, health - amount);
        if (health == 0) {
            this.handlePlayerDeath();
        }
    }

    private void handlePlayerDeath() {

        //return item to room
        for (Artefact item : inventory) {
            currentRoom.addArtefact(item);
        }
        //clear inventory and return to starting room
        inventory.clear();
        currentRoom = this.getStartingRoom();
        health = 3;
    }

    private Room getStartingRoom() {
        return entitiesLoader.getStartingRoom();
    }

    public String listInventory() {
        if (inventory.isEmpty()) {
            return "You are carrying nothing.";
        }

        StringBuilder sb = new StringBuilder("You are carrying: ");
        for (Artefact artefact : inventory) {
            sb.append(artefact.getName()).append(", ");
        }
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    public Iterator<Artefact> getInventoryIterator() {
        return inventory.iterator();
    }
}