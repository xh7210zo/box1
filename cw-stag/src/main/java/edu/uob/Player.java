package edu.uob;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Player extends Character {
    private Room currentRoom;
    private final Set<Artefact> inventory; // 用 Set 存储物品，防止重复
    private int health; // 健康属性

    public Player(String name, Room startingRoom) {
        super(name, "A brave adventurer");
        if (startingRoom == null) {
            throw new IllegalArgumentException("Starting room cannot be null!");
        }
        this.currentRoom = startingRoom;
        this.inventory = new HashSet<>();
        this.health = 3; // 初始化健康为 3
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
        return inventory.stream().noneMatch(i -> i.getName().equalsIgnoreCase(name));
    }
    // 获取当前健康值
    public int getHealth() {
        return health;
    }

    // 增加健康
    public void increaseHealth(int amount) {
        health = Math.min(3, health + amount); // 健康值最多为 3
    }

    // 减少健康
    public void decreaseHealth(int amount) {
        health = Math.max(0, health - amount); // 健康值不能小于 0
        if (health == 0) {
            handlePlayerDeath();
        }
    }

    // 处理死亡
    private void handlePlayerDeath() {
        for (Artefact item : inventory) {
            currentRoom.addArtefact(item); // 将物品放回当前房间
        }
        inventory.clear(); // 清空背包
        currentRoom = getStartingRoom(); // 将玩家传送回起始房间
        health = 3; // 恢复最大健康值
    }

    // 返回起始房间
    private Room getStartingRoom() {
        // 假设我们有一个固定的起始房间
        return EntitiesLoader.getStartingRoom();
    }
    // ✅ 实现 listInventory() 方法
    public String listInventory() {
        if (inventory.isEmpty()) {
            return "You are carrying nothing.";
        }

        StringBuilder sb = new StringBuilder("You are carrying: ");
        for (Artefact artefact : inventory) {
            sb.append(artefact.getName()).append(", ");
        }
        sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
        return sb.toString();
    }

    // ✅ 返回迭代器，而不是直接返回集合
    public Iterator<Artefact> getInventoryIterator() {
        return inventory.iterator();
    }
}
