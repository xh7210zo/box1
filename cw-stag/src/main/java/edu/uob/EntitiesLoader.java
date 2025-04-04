package edu.uob;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;
import com.alexmerz.graphviz.objects.Edge;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class EntitiesLoader {

    private final Map<String, Room> rooms;
    private Room startingRoom;

    //store all the valid entities
    private final Set<String> gameEntities;

    public EntitiesLoader() {
        this.rooms = new LinkedHashMap<>();
        this.gameEntities = new HashSet<>();
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public GameEntity getEntityByName(String entityName) {

        for (Room room : rooms.values()) {
            for (Artefact artefact : room.getArtefacts()) {
                if (artefact.getName().equalsIgnoreCase(entityName)) {
                    return artefact;
                }
            }
            for (Furniture furniture : room.getFurniture()) {
                if (furniture.getName().equalsIgnoreCase(entityName)) {
                    return furniture;
                }
            }
            for (Character character : room.getCharacters()) {
                if (character.getName().equalsIgnoreCase(entityName)) {
                    return character;
                }
            }
        }
        return null;
    }

    public Room getStartingRoom() {
        return startingRoom;
    }

    public Set<String> getGameEntities() {
        return gameEntities;
    }

    public void loadEntities(File entitiesFile) {
        Parser parser = new Parser();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(entitiesFile));
            parser.parse(bufferedReader);
            List<Graph> graphs = parser.getGraphs();
            Graph mainGraph = graphs.get(0);

            for (Graph locationSubgraph : mainGraph.getSubgraphs()) {
                this.processLocation(locationSubgraph);
            }

            this.processPaths(mainGraph);

            // ensure get the valid startingRoom
            if (startingRoom == null) {
                for (Room room : rooms.values()) {
                    if (!room.getName().startsWith("cluster")) {
                        startingRoom = room;
                        break;
                    }
                }
                if (startingRoom == null) {
                    throw new IllegalStateException("[EntitiesLoader] Error: No valid starting room found! Check your .dot file.");
                }
            }

            // add entity to gameEntities and print
            for (Room room : rooms.values()) {
                this.addRoomEntitiesToGameEntities(room);
            }

        } catch (IOException | ParseException e) {
            throw new RuntimeException("[EntitiesLoader] Error loading entities file", e);
        }
    }

    private void addRoomEntitiesToGameEntities(Room room) {

        // add artefacts、furniture and characters
        for (Artefact artefact : room.getArtefacts()) {
            gameEntities.add(artefact.getName());

        }
        for (Furniture furniture : room.getFurniture()) {
            gameEntities.add(furniture.getName());
        }
        for (Character character : room.getCharacters()) {
            gameEntities.add(character.getName());
        }
    }

    private void processEntities(Graph entitySubgraph, Room room, String entityType) {
        if (!entityType.equals("artefacts") && !entityType.equals("furniture") && !entityType.equals("characters")) {
            return;
        }

        List<Node> entityNodes = entitySubgraph.getNodes(false);

        for (Node entityNode : entityNodes) {
            String entityName = entityNode.getId().getId();
            String entityDescription = entityNode.getAttribute("description").toLowerCase();

            switch (entityType) {
                case "artefacts":
                    Artefact artefact = new Artefact(entityName, entityDescription);
                    room.addArtefact(artefact);
                    break;

                case "furniture":
                    Furniture furniture = new Furniture(entityName, entityDescription);
                    room.addFurniture(furniture);
                    break;

                case "characters":
                    Character character = new Character(entityName, entityDescription);
                    room.addCharacter(character);
                    break;

                default:
                    break;
            }
        }
    }

    private void processClusterSubgraph(Graph clusterSubgraph) {
        List<Node> nodes = clusterSubgraph.getNodes(false);
        if (nodes.isEmpty()) {
            return;
        }

        // use the next node of the cluster as the name of room
        Node firstNode = nodes.get(0);
        String roomName = firstNode.getId().getId();
        String roomDescription = firstNode.getAttribute("description");

        // ensure not to cover the existing room
        if (!rooms.containsKey(roomName)) {
            Room room = new Room(roomName, roomDescription);
            rooms.put(roomName, room);

            // set the first room to startingRoom
            if (startingRoom == null) {
                startingRoom = room;
            }

            // handle entities（artefacts、furniture、characters）
            for (Graph subgraph : clusterSubgraph.getSubgraphs()) {
                this.processEntities(subgraph, room, subgraph.getId().getId());
            }
        }
    }

    private void processLocation(Graph locationSubgraph) {

        for (Graph subgraph : locationSubgraph.getSubgraphs()) {
            String entityType = subgraph.getId().getId();

            if (entityType.startsWith("cluster")) {
                this.processClusterSubgraph(subgraph);
            }
        }
    }

    private void processPaths(Graph mainGraph) {

        // only handle paths defined in subgraph paths
        for (Graph subgraph : mainGraph.getSubgraphs()) {
            if (subgraph.getId().getId().equals("paths")) {

                // get all the connections in subgraph paths
                for (Edge edge : subgraph.getEdges()) {
                    String fromRoomName = edge.getSource().getNode().getId().getId();
                    String toRoomName = edge.getTarget().getNode().getId().getId();

                    Room fromRoom = rooms.get(fromRoomName);
                    Room toRoom = rooms.get(toRoomName);

                    if (fromRoom != null && toRoom != null && !fromRoom.getExits().containsKey(toRoomName)) {
                        fromRoom.addExit(toRoomName, toRoom);
                    }
                }
            }
        }
    }
}