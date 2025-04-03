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
    private static Room startingRoom;
    private final Set<String> gameEntities;  // 新增的 gameEntities 集合，用于存储所有有效的实体名称

    public EntitiesLoader() {
        this.rooms = new LinkedHashMap<>();
        Room storeroom = new Room("storeroom", "Storage for unplaced entities");
        rooms.put("storeroom", storeroom);
        this.gameEntities = new HashSet<>();  // 初始化 gameEntities 集合
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public GameEntity getEntityByName(String entityName) {
        // 遍历所有房间中的 artefacts
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
    public static Room getStartingRoom() {
        return startingRoom;
    }

    public Set<String> getGameEntities() {
        return gameEntities;  // 提供访问 gameEntities 集合的方法
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

            for (Room room : rooms.values()) {
                // 这里将物品、家具和角色的名称添加到 gameEntities 中
                this.addRoomEntitiesToGameEntities(room);
            }

        } catch (IOException | ParseException e) {
            throw new RuntimeException("[EntitiesLoader] Error loading entities file", e);
        }
    }

    // 新增方法，将房间中的物品、家具和角色名称添加到 gameEntities 中
    private void addRoomEntitiesToGameEntities(Room room) {
        // 添加物品（artefacts）
        for (Artefact artefact : room.getArtefacts()) {
            gameEntities.add(artefact.getName());  // 添加物品名称
        }

        // 添加家具（furniture）
        for (Furniture furniture : room.getFurniture()) {
            gameEntities.add(furniture.getName());  // 添加家具名称
        }

        // 添加角色（characters）
        for (Character character : room.getCharacters()) {
            gameEntities.add(character.getName());  // 添加角色名称
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

    private void processClusterSubgraph(Graph clusterSubgraph) {
        List<Node> nodes = clusterSubgraph.getNodes(false);
        if (nodes.isEmpty()) {
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

        // **设置第一个房间为 startingRoom**
        if (startingRoom == null) {
            startingRoom = room;
        }

        // 处理房间中的子元素（artefacts、furniture、characters）
        for (Graph subgraph : clusterSubgraph.getSubgraphs()) {
            this.processEntities(subgraph, room, subgraph.getId().getId());
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

    private void processPaths(Graph mainGraph) {

        // 只处理 subgraph paths 中定义的路径
        for (Graph subgraph : mainGraph.getSubgraphs()) {
            if (subgraph.getId().getId().equals("paths")) {

                // 获取 subgraph paths 中的所有连接
                for (Edge edge : subgraph.getEdges()) {
                    String fromRoomName = edge.getSource().getNode().getId().getId();
                    String toRoomName = edge.getTarget().getNode().getId().getId();

                    Room fromRoom = rooms.get(fromRoomName);
                    Room toRoom = rooms.get(toRoomName);

                    if (fromRoom != null && toRoom != null&&!fromRoom.getExits().containsKey(toRoomName)) {
                        fromRoom.addExit(toRoomName, toRoom);
                        }


                }
            }
        }
    }
}
