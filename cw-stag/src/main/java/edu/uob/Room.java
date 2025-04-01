package edu.uob;

import java.util.*;

public class Room extends GameEntity {
    private Map<String, Room> exits; // 存储房间出口
    private Set<Artefact> artefacts; // 存储房间内的物品
    private Set<Furniture> furniture; // 存储房间内的家具
    private Set<Character> characters; // 存储房间内的角色

    public Room(String name, String description) {
        super(name, description);
        this.exits = new HashMap<>();
        this.artefacts = new HashSet<>();
        this.furniture = new HashSet<>();
        this.characters = new HashSet<>();
    }

    // 添加房间出口
    public void addExit(String direction, Room room) {
        exits.put(direction.toLowerCase(), room);
    }

    // 获取房间的出口
    public Room getExit(String direction) {
        return exits.get(direction.toLowerCase());
    }

    // 获取所有的出口
    public Map<String, Room> getExits() {
        return exits;
    }

    // 添加物品
    public void addArtefact(Artefact artefact) {
        artefacts.add(artefact);
    }

    // 获取房间内的所有物品
    public Set<Artefact> getArtefacts() {
        return artefacts;
    }

    // 添加家具
    public void addFurniture(Furniture f) {
        furniture.add(f);
    }

    // 获取房间内的所有家具
    public Set<Furniture> getFurniture() {
        return furniture;
    }

    // 添加角色
    public void addCharacter(Character c) {
        characters.add(c);
    }

    // 获取房间内的所有角色
    public Set<Character> getCharacters() {
        return characters;
    }

    // 获取所有的实体（物品、家具、角色）
    public Set<GameEntity> getEntities() {
        Set<GameEntity> entities = new HashSet<>();
        entities.addAll(artefacts);
        entities.addAll(furniture);
        entities.addAll(characters);
        return entities;
    }

    // 获取所有连接的房间
    public Set<Room> getConnectedRooms() {
        return new HashSet<>(exits.values());
    }
    // 打印房间描述
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are in: ").append(getName()).append("\n");
        sb.append(getDescription()).append("\n");

        if (!artefacts.isEmpty()) {
            sb.append("You see: ");
            artefacts.forEach(a -> sb.append(a.getName()).append(", "));
            sb.setLength(sb.length() - 2); // 去掉最后的逗号
            sb.append("\n");
        }

        if (!furniture.isEmpty()) {
            sb.append("There is furniture: ");
            furniture.forEach(f -> sb.append(f.getName()).append(", "));
            sb.setLength(sb.length() - 2);
            sb.append("\n");
        }

        if (!characters.isEmpty()) {
            sb.append("Characters present: ");
            characters.forEach(c -> sb.append(c.getName()).append(", "));
            sb.setLength(sb.length() - 2);
            sb.append("\n");
        }

        if (!exits.isEmpty()) {
            sb.append("You can go to: ");
            exits.keySet().forEach(exit -> sb.append(exit).append(", "));
            sb.setLength(sb.length() - 2);
            sb.append("\n");
        }

        return sb.toString();
    }
}
