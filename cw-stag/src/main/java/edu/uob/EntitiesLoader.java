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

public class EntitiesLoader {
    private Map<String, Room> rooms;
    private Room storeroom;
    private Room startingRoom;

    public EntitiesLoader() {
        this.rooms = new LinkedHashMap<>();
        this.storeroom = new Room("storeroom", "Storage for unplaced entities");
        rooms.put("storeroom", storeroom);
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Room getStoreroom() {
        return storeroom;
    }

    public Room getStartingRoom() {
        return startingRoom;
    }

    public void loadEntities(File entitiesFile) {
        Parser parser = new Parser();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(entitiesFile));
            System.out.println("[EntitiesLoader] Parsing entities file...");
            parser.parse(bufferedReader);
            List<Graph> graphs = parser.getGraphs();
            Graph mainGraph = graphs.get(0);

            for (Graph locationSubgraph : mainGraph.getSubgraphs()) {
                processLocation(locationSubgraph);
            }

            processPaths(mainGraph);

            if (startingRoom == null) {
                for (Room room : rooms.values()) {
                    if (!room.getName().startsWith("cluster")) {
                        startingRoom = room;
                        System.out.println("[EntitiesLoader] Setting starting room: " + startingRoom.getName());
                        break;
                    }
                }
                if (startingRoom == null) {
                    throw new IllegalStateException("[EntitiesLoader] Error: No valid starting room found! Check your .dot file.");
                }
            }

            System.out.println("[EntitiesLoader] Debug: List of rooms and their contents:");
            for (Room room : rooms.values()) {
                System.out.println("[EntitiesLoader] Room: " + room.getName());
                System.out.println("[EntitiesLoader] Description: " + room.getDescription());

                if (!room.getArtefacts().isEmpty()) {
                    System.out.println("[EntitiesLoader] Artefacts:");
                    for (Artefact artefact : room.getArtefacts()) {
                        System.out.println("    " + artefact.getName() + ": " + artefact.getDescription());
                    }
                } else {
                    System.out.println("[EntitiesLoader] No artefacts in this room.");
                }

                if (!room.getFurniture().isEmpty()) {
                    System.out.println("[EntitiesLoader] Furniture:");
                    for (Furniture furniture : room.getFurniture()) {
                        System.out.println("    " + furniture.getName() + ": " + furniture.getDescription());
                    }
                } else {
                    System.out.println("[EntitiesLoader] No furniture in this room.");
                }

                if (!room.getCharacters().isEmpty()) {
                    System.out.println("[EntitiesLoader] Characters:");
                    for (Character character : room.getCharacters()) {
                        System.out.println("    " + character.getName() + ": " + character.getDescription());
                    }
                } else {
                    System.out.println("[EntitiesLoader] No characters in this room.");
                }

                if (!room.getExits().isEmpty()) {
                    System.out.println("[EntitiesLoader] Exits:");
                    for (Map.Entry<String, Room> exit : room.getExits().entrySet()) {
                        System.out.println("    Exit to: " + exit.getKey());
                    }
                } else {
                    System.out.println("[EntitiesLoader] No exits in this room.");
                }

                System.out.println("[EntitiesLoader] ---");
            }

        } catch (IOException | ParseException e) {
            throw new RuntimeException("[EntitiesLoader] Error loading entities file", e);
        }
    }

    private void processLocation(Graph locationSubgraph) {
        System.out.println("[EntitiesLoader] Processing subgraph: " + locationSubgraph.getId().getId());

        for (Graph subgraph : locationSubgraph.getSubgraphs()) {
            String entityType = subgraph.getId().getId();

            if (entityType.startsWith("cluster")) {
                processClusterSubgraph(subgraph);
            }
        }
    }

    private void processClusterSubgraph(Graph clusterSubgraph) {
        List<Node> nodes = clusterSubgraph.getNodes(false);
        if (nodes.isEmpty()) {
            System.out.println("[EntitiesLoader] Warning: Cluster " + clusterSubgraph.getId().getId() + " has no nodes!");
            return;
        }

        // 获取 cluster 下的第一个节点作为房间名称
        Node firstNode = nodes.get(0);
        String roomName = firstNode.getId().getId();
        String roomDescription = firstNode.getAttribute("description");

        // 特殊处理 storeroom
        if (clusterSubgraph.getId().getId().equals("cluster999")) {
            roomName = "storeroom";
        }

        Room room = new Room(roomName, roomDescription);
        rooms.put(roomName, room);
        System.out.println("[EntitiesLoader] Created room: " + roomName + " - " + roomDescription);

        // **设置第一个房间为 startingRoom**
        if (startingRoom == null) {
            startingRoom = room;
            System.out.println("[EntitiesLoader] Setting starting room: " + startingRoom.getName());
        }

        // 处理房间中的子元素（artefacts、furniture、characters）
        for (Graph subgraph : clusterSubgraph.getSubgraphs()) {
            processEntities(subgraph, room, subgraph.getId().getId());
        }
    }

    private void processEntities(Graph entitySubgraph, Room room, String entityType) {
        System.out.println("[EntitiesLoader] Processing entity type: " + entityType);

        if (!entityType.equals("artefacts") && !entityType.equals("furniture") && !entityType.equals("characters")) {
            return;
        }

        List<Node> entityNodes = entitySubgraph.getNodes(false);

        if (entityNodes.isEmpty()) {
            System.out.println("[EntitiesLoader] Warning: No entities found for entity type: " + entityType);
        }

        for (Node entityNode : entityNodes) {
            String entityName = entityNode.getId().getId();
            String entityDescription = entityNode.getAttribute("description").toLowerCase();

            System.out.println("[EntitiesLoader] Processing entity: " + entityName);
            System.out.println("[EntitiesLoader] Entity Description: " + entityDescription);

            switch (entityType) {
                case "artefacts":
                    Artefact artefact = new Artefact(entityName, entityDescription);
                    room.addArtefact(artefact);
                    System.out.println("[EntitiesLoader] Added artefact: " + entityName + " to location: " + room.getName());
                    break;

                case "furniture":
                    Furniture furniture = new Furniture(entityName, entityDescription);
                    room.addFurniture(furniture);
                    System.out.println("[EntitiesLoader] Added furniture: " + entityName + " to location: " + room.getName());
                    break;

                case "characters":
                    Character character = new Character(entityName, entityDescription);
                    room.addCharacter(character);
                    System.out.println("[EntitiesLoader] Added character: " + entityName + " to location: " + room.getName());
                    break;

                default:
                    System.out.println("[EntitiesLoader] Warning: Unrecognized entity type: " + entityType + " for entity: " + entityName);
                    break;
            }
        }
    }

    private void processPaths(Graph mainGraph) {
        System.out.println("[EntitiesLoader] Parsing edges for room connections...");

        // 只处理 subgraph paths 中定义的路径
        for (Graph subgraph : mainGraph.getSubgraphs()) {
            if (subgraph.getId().getId().equals("paths")) {
                System.out.println("[EntitiesLoader] Parsing subgraph paths...");

                // 获取 subgraph paths 中的所有连接
                for (Edge edge : subgraph.getEdges()) {
                    String fromRoomName = edge.getSource().getNode().getId().getId();
                    String toRoomName = edge.getTarget().getNode().getId().getId();

                    Room fromRoom = rooms.get(fromRoomName);
                    Room toRoom = rooms.get(toRoomName);

                    if (fromRoom != null && toRoom != null) {
                        // 确保仅添加 subgraph paths 中定义的路径
                        if (!fromRoom.getExits().containsKey(toRoomName)) {
                            fromRoom.addExit(toRoomName, toRoom);
                            System.out.println("[EntitiesLoader] Added exit from " + fromRoomName + " to " + toRoomName);
                        }

                    } else {
                        System.out.println("[EntitiesLoader] Warning: Invalid exit from " + fromRoomName + " to " + toRoomName);
                    }
                }
            }
        }
    }



}
