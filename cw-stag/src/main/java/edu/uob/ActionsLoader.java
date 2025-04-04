package edu.uob;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class ActionsLoader {

    // store actions
    private final Map<String, GameAction> actions;

    public ActionsLoader() {
        this.actions = new HashMap<>();
    }

    public Map<String, GameAction> getActions() {
        return actions;
    }

    public void loadActions(File actionsFile) {
        try {
            // create DocumentBuilderFactory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // parse .xml and get all the action elements
            Document doc = builder.parse(actionsFile);
            doc.getDocumentElement().normalize(); // 规范化 XML 结构
            NodeList actionNodes = doc.getElementsByTagName("action");

            //go through all the action nodes
            for (int i = 0; i < actionNodes.getLength(); i++) {
                Node actionNode = actionNodes.item(i);

                // ensure it's element node
                if (actionNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element actionElement = (Element) actionNode;

                    // parse triggers get key of keyphrase
                    NodeList triggerNodes = actionElement.getElementsByTagName("keyphrase");
                    Set<String> triggers = new HashSet<>();
                    for (int j = 0; j < triggerNodes.getLength(); j++) {
                        String trigger = triggerNodes.item(j).getTextContent().trim();
                        triggers.add(trigger);
                    }

                    // parse subjects、consumed and produced and get narration
                    Set<String> subjects = this.extractEntities(actionElement, "subjects");
                    Set<String> consumed = this.extractEntities(actionElement, "consumed");
                    Set<String> produced = this.extractEntities(actionElement, "produced");
                    String narration = actionElement.getElementsByTagName("narration").item(0).getTextContent().trim();

                    // create GameAction and add to actions Map
                    GameAction action = new GameAction(subjects, consumed, produced, narration);
                    for (String trigger : triggers) {
                        actions.put(trigger, action);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading actions file", e);
        }
    }

    private Set<String> extractEntities(Element actionElement, String tagName) {
        Set<String> entities = new HashSet<>();
        NodeList parentNodeList = actionElement.getElementsByTagName(tagName);

        if (parentNodeList.getLength() > 0) {
            Element parentElement = (Element) parentNodeList.item(0);
            NodeList entityNodes = parentElement.getElementsByTagName("entity");

            for (int i = 0; i < entityNodes.getLength(); i++) {
                String entity = entityNodes.item(i).getTextContent().trim();
                entities.add(entity);
            }
        }
        return entities;
    }
}