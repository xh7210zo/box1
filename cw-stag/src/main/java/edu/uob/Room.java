package edu.uob;

import java.util.*;

public class Room extends GameEntity {
    private final Map<String, Room> exits;
    private final Set<Artefact> artefacts;
    private final Set<Furniture> furniture;
    private final Set<Character> characters;

    public Room(String name, String description) {
        super(name, description);
        this.exits = new HashMap<>();
        this.artefacts = new HashSet<>();
        this.furniture = new HashSet<>();
        this.characters = new HashSet<>();
    }

    public void addExit(String direction, Room room) {
        exits.put(direction.toLowerCase(), room);
    }

    public void removeExit(String direction) {
        exits.remove(direction.toLowerCase());
    }

    public Room getExit(String direction) {
        return exits.get(direction.toLowerCase());
    }

    public Map<String, Room> getExits() {
        return exits;
    }

    public void addArtefact(Artefact artefact) {
        artefacts.add(artefact);
    }

    public Set<Artefact> getArtefacts() {
        return artefacts;
    }

    public void removeArtefact(Artefact artefact) {
        artefacts.remove(artefact);
    }

    public void addFurniture(Furniture f) {
        furniture.add(f);
    }

    public Set<Furniture> getFurniture() {
        return furniture;
    }

    public void addCharacter(Character c) {
        characters.add(c);
    }

    public Set<Character> getCharacters() {
        return characters;
    }

    public Set<GameEntity> getEntities() {
        Set<GameEntity> entities = new HashSet<>();
        entities.addAll(artefacts);
        entities.addAll(furniture);
        entities.addAll(characters);
        return entities;
    }

    public Set<Room> getConnectedRooms() {
        return new HashSet<>(exits.values());
    }

    public boolean hasEntity(String entityName) {
        // check if the entity is an artefact in the room
        for (Artefact artefact : artefacts) {
            if (artefact.getName().equalsIgnoreCase(entityName)) {
                return true;
            }
        }

        // check if the entity is furniture in the room
        for (Furniture furniture : furniture) {
            if (furniture.getName().equalsIgnoreCase(entityName)) {
                return true;
            }
        }

        // check if the entity is a character in the room
        for (Character character : characters) {
            if (character.getName().equalsIgnoreCase(entityName)) {
                return true;
            }
        }
        return false;
    }

    public void removeEntityByName(String entityName) {

        for (Artefact artefact : new HashSet<>(artefacts)) {
            if (artefact.getName().equalsIgnoreCase(entityName)) {
                artefacts.remove(artefact);
                return;
            }
        }

        for (Furniture furniture : new HashSet<>(this.furniture)) {
            if (furniture.getName().equalsIgnoreCase(entityName)) {
                this.furniture.remove(furniture);
                return;
            }
        }

        for (Character character : new HashSet<>(this.characters)) {
            if (character.getName().equalsIgnoreCase(entityName)) {
                this.characters.remove(character);
                return;
            }
        }
    }

    public void addEntity(GameEntity entity) {
        if (entity instanceof Artefact) {
            artefacts.add((Artefact) entity);
        } else if (entity instanceof Furniture) {
            furniture.add((Furniture) entity);
        } else if (entity instanceof Character) {
            characters.add((Character) entity);
        } else {
            throw new IllegalArgumentException("Unsupported entity type.");
        }
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are in: ").append(getName()).append("\n");
        sb.append(getDescription()).append("\n");

        // print artefacts and remove the ',' at the end
        if (!artefacts.isEmpty()) {
            sb.append("Artefacts: ");
            for (Artefact a : artefacts) {
                sb.append(a.getName()).append(", ");
            }
            if (sb.charAt(sb.length() - 2) == ',') {
                sb.setLength(sb.length() - 2);
            }
            sb.append("\n");
        } else {
            sb.append("No artefacts in this room.\n");
        }

        // print furniture
        if (!furniture.isEmpty()) {
            sb.append("Furniture: ");
            for (Furniture f : furniture) {
                sb.append(f.getName()).append(", ");
            }
            if (sb.charAt(sb.length() - 2) == ',') {
                sb.setLength(sb.length() - 2);
            }
            sb.append("\n");
        } else {
            sb.append("No furniture in this room.\n");
        }

        // print characters
        if (!characters.isEmpty()) {
            sb.append("Characters: ");
            for (Character c : characters) {
                sb.append(c.getName()).append(", ");
            }
            if (sb.charAt(sb.length() - 2) == ',') {
                sb.setLength(sb.length() - 2);
            }
            sb.append("\n");
        } else {
            sb.append("No characters in this room.\n");
        }

        // print exits
        if (!exits.isEmpty()) {
            sb.append("Exits: ");
            for (String exit : exits.keySet()) {
                sb.append(exit).append(", ");
            }
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