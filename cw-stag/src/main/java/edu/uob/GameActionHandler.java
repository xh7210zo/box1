package edu.uob;
import java.util.*;

public class GameActionHandler {
    private  Map<String, GameAction> actions;
    private  Player currentPlayer;
    private  EntitiesLoader entitiesLoader;

    public GameActionHandler(Map<String, GameAction> actions, Player currentPlayer, EntitiesLoader entitiesLoader) {
        this.actions = actions;
        this.currentPlayer = currentPlayer;
        this.entitiesLoader = entitiesLoader;
    }

    public String handleGameAction(String actionVerb, Iterator<String> subjectIterator) {
        if (!actions.containsKey(actionVerb)) {
            return "Invalid action.";
        }

        // 获取单个 GameAction
        GameAction action = actions.get(actionVerb);

        // 将 Iterator 转换为 Set 以进行 containsAll 检查
        Set<String> subjects = new HashSet<>();
        while (subjectIterator.hasNext()) {
            subjects.add(subjectIterator.next());
        }

        if (subjects.containsAll(action.getSubjects())) {
            return this.executeGameAction(action);
        }

        return "You can't do that right now.";
    }

    private String executeGameAction(GameAction action) {
        Room currentRoom = currentPlayer.getCurrentRoom();


        // **检查 Action 作用的对象 (Subjects) 是否在当前房间/玩家身上**
        for (String subject : action.getSubjects()) {
            if (!currentRoom.hasEntity(subject) && currentPlayer.hasItem(subject) && !entitiesLoader.getRooms().containsKey(subject)) {
                StringBuilder sb = new StringBuilder();
                sb.append("You don't see ").append(subject).append(" here.");
                return sb.toString();
            }
        }

        // **检查要消耗的实体 (Consumed) 是否存在**
        for (String entity : action.getConsumed()) {
            if (!entity.equalsIgnoreCase("health") && !currentRoom.hasEntity(entity) &&
                    currentPlayer.hasItem(entity) && !entitiesLoader.getRooms().containsKey(entity)) {
                StringBuilder sb = new StringBuilder();
                sb.append("You don't see ").append(entity).append(" here.");
                return sb.toString();
            }
        }

        // **检查要生成的实体 (Produced) 是否有效**
        for (String entity : action.getProduced()) {

            if (!entity.equalsIgnoreCase("health") && !currentRoom.hasEntity(entity) &&
                    entitiesLoader.getEntityByName(entity) == null && !entitiesLoader.getRooms().containsKey(entity)) {

                // 返回无法创建该实体的信息
                StringBuilder sb = new StringBuilder();
                sb.append("You cannot create ").append(entity).append(" here.");
                return sb.toString();
            }
        }


        // **处理健康变化**
        if (action.getConsumed().contains("health")) {
            currentPlayer.decreaseHealth(1);
        }
        if (action.getProduced().contains("health")) {
            currentPlayer.increaseHealth(1);
        }

        // **处理消耗的实体**
        for (String entity : action.getConsumed()) {
            if (entity.equalsIgnoreCase("health")) continue; // 🛑 已处理 health，跳过

            if (entitiesLoader.getRooms().containsKey(entity)) {
                // **如果消耗的是一个房间**
                currentRoom.removeExit(entity); // 从当前房间的出口中移除
            } else {
                // 先检查玩家背包
                Artefact artefactToRemove = null;
                Iterator<Artefact> it = currentPlayer.getInventoryIterator();
                while (it.hasNext()) {
                    Artefact artefact = it.next();
                    if (artefact.getName().equalsIgnoreCase(entity)) {
                        artefactToRemove = artefact;
                        break;
                    }
                }

                if (artefactToRemove != null) {
                    currentPlayer.removeItem(artefactToRemove);
                } else if (currentRoom.hasEntity(entity)) {
                    currentRoom.removeEntityByName(entity);
                }
            }
        }

        // **处理生成的实体**
        for (String entity : action.getProduced()) {
            if (entity.equalsIgnoreCase("health")) continue; // 🛑 已处理 health，跳过

            if (entitiesLoader.getRooms().containsKey(entity)) {
                // **如果生成的是一个房间**
                Room newRoom = entitiesLoader.getRooms().get(entity);
                currentRoom.addExit(entity, newRoom); // 将新房间添加为当前房间的出口
            } else {
                // 否则尝试通过实体加载器获取物品
                GameEntity newEntity = entitiesLoader.getEntityByName(entity);
                if (newEntity != null) {
                    currentRoom.addEntity(newEntity);
                }
            }
        }
        return action.getNarration(); // 返回描述信息
    }
}
