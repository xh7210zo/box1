package edu.uob;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Player extends Character {
    private Room currentRoom;
    private Set<Artefact> inventory; // 用 Set 存储物品，防止重复

    public Player(String name, Room startingRoom) {
        super(name, "A brave adventurer");
        if (startingRoom == null) {
            throw new IllegalArgumentException("Starting room cannot be null!");
        }
        this.currentRoom = startingRoom;
        this.inventory = new HashSet<>();
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
        return inventory.stream().anyMatch(i -> i.getName().equalsIgnoreCase(name));
    }

    // ✅ 实现 listInventory() 方法
    public String listInventory() {
        if (inventory.isEmpty()) {
            return "You are carrying nothing.";
        }

        StringBuilder sb = new StringBuilder("You are carrying: ");
        Iterator<Artefact> it = inventory.iterator();
        while (it.hasNext()) {
            sb.append(it.next().getName()).append(", ");
        }
        sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
        return sb.toString();
    }

    // ✅ 返回迭代器，而不是直接返回集合
    public Iterator<Artefact> getInventoryIterator() {
        return inventory.iterator();
    }
}
