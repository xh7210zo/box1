package edu.uob;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class ActionsLoader {

    // 用于存储解析后的动作
    private Map<String, GameAction> actions;

    public ActionsLoader() {
        this.actions = new HashMap<>();
    }

    public Map<String, GameAction> getActions() {
        return actions;
    }

    /**
     * 从 XML 文件加载动作
     *
     * @param actionsFile XML 文件
     */
    public void loadActions(File actionsFile) {
        try {
            // 创建 DocumentBuilderFactory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // 解析 XML 文件
            Document doc = builder.parse(actionsFile);

            // 获取所有的 action 元素
            NodeList actionNodes = doc.getElementsByTagName("action");
            System.out.println("[ActionsLoader] Found " + actionNodes.getLength() + " actions in the file.");

            // 遍历所有的 action 节点
            for (int i = 0; i < actionNodes.getLength(); i++) {
                org.w3c.dom.Node actionNode = actionNodes.item(i);
                if (actionNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element actionElement = (Element) actionNode;

                    System.out.println("[ActionsLoader] Processing action " + (i + 1) + " of " + actionNodes.getLength());

                    // 获取 trigger 关键字
                    NodeList triggerNodes = actionElement.getElementsByTagName("trigger");
                    List<String> triggers = new ArrayList<>();
                    for (int j = 0; j < triggerNodes.getLength(); j++) {
                        String trigger = triggerNodes.item(j).getTextContent();
                        triggers.add(trigger);
                        System.out.println("[ActionsLoader] Found trigger: " + trigger);
                    }

                    // 获取 subject 节点
                    NodeList subjectNodes = actionElement.getElementsByTagName("subject");
                    List<String> subjects = new ArrayList<>();
                    for (int j = 0; j < subjectNodes.getLength(); j++) {
                        String subject = subjectNodes.item(j).getTextContent();
                        subjects.add(subject);
                        System.out.println("[ActionsLoader] Found subject: " + subject);
                    }

                    // 获取 consumed 节点
                    NodeList consumedNodes = actionElement.getElementsByTagName("consumed");
                    List<String> consumed = new ArrayList<>();
                    for (int j = 0; j < consumedNodes.getLength(); j++) {
                        String item = consumedNodes.item(j).getTextContent();
                        consumed.add(item);
                        System.out.println("[ActionsLoader] Found consumed item: " + item);
                    }

                    // 获取 produced 节点
                    NodeList producedNodes = actionElement.getElementsByTagName("produced");
                    List<String> produced = new ArrayList<>();
                    for (int j = 0; j < producedNodes.getLength(); j++) {
                        String item = producedNodes.item(j).getTextContent();
                        produced.add(item);
                        System.out.println("[ActionsLoader] Found produced item: " + item);
                    }

                    // 获取 narration
                    String narration = actionElement.getElementsByTagName("narration").item(0).getTextContent();
                    System.out.println("[ActionsLoader] Found narration: " + narration);

                    // 创建 GameAction 对象并添加到 actions 中
                    GameAction action = new GameAction(triggers, subjects, consumed, produced, narration);
                    for (String trigger : triggers) {
                        actions.put(trigger, action);
                        System.out.println("[ActionsLoader] Added action for trigger: " + trigger);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading actions file", e);
        }
    }
}
