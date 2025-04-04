package edu.uob;
import java.util.*;

public class GameActionHandler {
    private final Map<String, GameAction> actions;
    private final Player currentPlayer;
    private final EntitiesLoader entitiesLoader;

    public GameActionHandler(Map<String, GameAction> actions, Player currentPlayer, EntitiesLoader entitiesLoader) {
        this.actions = actions;
        this.currentPlayer = currentPlayer;
        this.entitiesLoader = entitiesLoader;
    }

    public String handleGameAction(String actionVerb, Iterator<String> subjectIterator) {
        if (!actions.containsKey(actionVerb)) {
            return "Invalid action.";
        }

        // get single GameAction
        GameAction action = actions.get(actionVerb);

        // change Iterator to Set to check
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


        // check if the subject exists in the inventory or room
        for (String subject : action.getSubjects()) {
            if (!currentRoom.hasEntity(subject) && currentPlayer.hasItem(subject) && !entitiesLoader.getRooms().containsKey(subject)) {
                StringBuilder sb = new StringBuilder();
                sb.append("You don't see ").append(subject).append(" here.");
                return sb.toString();
            }
        }

        // check if the consumed entity is valid
        for (String entity : action.getConsumed()) {
            if (!entity.equalsIgnoreCase("health") && !currentRoom.hasEntity(entity) &&
                    currentPlayer.hasItem(entity) && !entitiesLoader.getRooms().containsKey(entity)) {
                StringBuilder sb = new StringBuilder();
                sb.append("You don't see ").append(entity).append(" here.");
                return sb.toString();
            }
        }

        // check if the produced entity is valid
        for (String entity : action.getProduced()) {

            if (!entity.equalsIgnoreCase("health") && !currentRoom.hasEntity(entity) &&
                    entitiesLoader.getEntityByName(entity) == null && !entitiesLoader.getRooms().containsKey(entity)) {

                StringBuilder sb = new StringBuilder();
                sb.append("You cannot create ").append(entity).append(" here.");
                return sb.toString();
            }
        }

        // handle health change
        if (action.getConsumed().contains("health")) {
            currentPlayer.decreaseHealth(1);
        }
        if (action.getProduced().contains("health")) {
            currentPlayer.increaseHealth(1);
        }

        // handle the consumed entity
        for (String entity : action.getConsumed()) {
            if (entity.equalsIgnoreCase("health")) continue;

            // if the entity is a room
            if (entitiesLoader.getRooms().containsKey(entity)) {
                currentRoom.removeExit(entity);
            } else {
                // check inventory of the player
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

        //  handle the produced entity
        for (String entity : action.getProduced()) {
            if (entity.equalsIgnoreCase("health")) continue;

            if (entitiesLoader.getRooms().containsKey(entity)) {
                Room newRoom = entitiesLoader.getRooms().get(entity);
                currentRoom.addExit(entity, newRoom);
            } else {
                GameEntity newEntity = entitiesLoader.getEntityByName(entity);
                if (newEntity != null) {
                    currentRoom.addEntity(newEntity);
                }
            }
        }
        return action.getNarration();
    }
}