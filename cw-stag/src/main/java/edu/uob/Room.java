package edu.uob;

import java.util.*;

public class Room extends GameEntity {
    private final Map<String, Room> exits; // 存储房间出口
    private final Set<Artefact> artefacts; // 存储房间内的物品
    private final Set<Furniture> furniture; // 存储房间内的家具
    private final Set<Character> characters; // 存储房间内的角色

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
    // 移除房间出口的方法
    public void removeExit(String direction) {
        exits.remove(direction.toLowerCase());
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

    public void removeArtefact(Artefact artefact) {
        artefacts.remove(artefact);
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

    public boolean hasEntity(String entityName) {
        // 检查房间内的所有物品是否包含该实体名称
        for (Artefact artefact : artefacts) {
            if (artefact.getName().equalsIgnoreCase(entityName)) {
                return true;
            }
        }

        // 检查房间内的所有家具是否包含该实体名称
        for (Furniture furniture : furniture) {
            if (furniture.getName().equalsIgnoreCase(entityName)) {
                return true;
            }
        }

        // 检查房间内的所有角色是否包含该实体名称
        for (Character character : characters) {
            if (character.getName().equalsIgnoreCase(entityName)) {
                return true;
            }
        }

        return false;  // 如果没有找到实体，返回 false
    }
    public void removeEntityByName(String entityName) {
        // 遍历物品集合
        for (Artefact artefact : new HashSet<>(artefacts)) {
            if (artefact.getName().equalsIgnoreCase(entityName)) {
                artefacts.remove(artefact);
                return; // 找到并移除物品
            }
        }

        // 遍历家具集合
        for (Furniture furniture : new HashSet<>(this.furniture)) {
            if (furniture.getName().equalsIgnoreCase(entityName)) {
                this.furniture.remove(furniture);
                return; // 找到并移除家具
            }
        }

        // 遍历角色集合
        for (Character character : new HashSet<>(this.characters)) {
            if (character.getName().equalsIgnoreCase(entityName)) {
                this.characters.remove(character);
                return; // 找到并移除角色
            }
        }

    }
    public void addEntity(GameEntity entity) {
        if (entity instanceof Artefact) {
            artefacts.add((Artefact) entity);  // 添加物品
        } else if (entity instanceof Furniture) {
            furniture.add((Furniture) entity);  // 添加家具
        } else if (entity instanceof Character) {
            characters.add((Character) entity);  // 添加角色
        } else {
            throw new IllegalArgumentException("Unsupported entity type.");
        }
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are in: ").append(getName()).append("\n");
        sb.append(getDescription()).append("\n");

        // 打印 artefacts (物品)
        if (!artefacts.isEmpty()) {
            sb.append("Artefacts: ");
            for (Artefact a : artefacts) {
                sb.append(a.getName()).append(", ");
            }
            // 去掉最后的逗号
            if (sb.charAt(sb.length() - 2) == ',') {
                sb.setLength(sb.length() - 2);
            }
            sb.append("\n");
        } else {
            sb.append("No artefacts in this room.\n");
        }

        // 打印 furniture (家具)
        if (!furniture.isEmpty()) {
            sb.append("Furniture: ");
            for (Furniture f : furniture) {
                sb.append(f.getName()).append(", ");
            }
            // 去掉最后的逗号
            if (sb.charAt(sb.length() - 2) == ',') {
                sb.setLength(sb.length() - 2);
            }
            sb.append("\n");
        } else {
            sb.append("No furniture in this room.\n");
        }

        // 打印 characters (角色)
        if (!characters.isEmpty()) {
            sb.append("Characters: ");
            for (Character c : characters) {
                sb.append(c.getName()).append(", ");
            }
            // 去掉最后的逗号
            if (sb.charAt(sb.length() - 2) == ',') {
                sb.setLength(sb.length() - 2);
            }
            sb.append("\n");
        } else {
            sb.append("No characters in this room.\n");
        }

        // 打印 exits (出口)
        if (!exits.isEmpty()) {
            sb.append("Exits: ");
            for (String exit : exits.keySet()) {
                sb.append(exit).append(", ");
            }
            // 去掉最后的逗号
            if (sb.charAt(sb.length() - 2) == ',') {
                sb.setLength(sb.length() - 2);
            }
            sb.append("\n");
        } else {
            sb.append("No exits from this room.\n");
        }

        return sb.toString();
    }
}
