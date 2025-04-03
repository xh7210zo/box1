package edu.uob;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class ActionsLoader {

    // 用于存储解析后的动作
    private final Map<String, GameAction> actions;

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
            doc.getDocumentElement().normalize(); // 规范化 XML 结构

            // 获取所有的 action 元素
            NodeList actionNodes = doc.getElementsByTagName("action");

            // 遍历所有的 action 节点
            for (int i = 0; i < actionNodes.getLength(); i++) {
                Node actionNode = actionNodes.item(i);

                // 确保是元素节点
                if (actionNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element actionElement = (Element) actionNode;

                    // 解析 triggers (获取 <keyphrase> 里的值)
                    NodeList triggerNodes = actionElement.getElementsByTagName("keyphrase");
                    List<String> triggers = new ArrayList<>();
                    for (int j = 0; j < triggerNodes.getLength(); j++) {
                        String trigger = triggerNodes.item(j).getTextContent().trim();
                        triggers.add(trigger);
                    }

                    // 解析 subjects (获取 <entity> 里的值)
                    List<String> subjects = this.extractEntities(actionElement, "subjects");

                    // 解析 consumed (获取 <entity> 里的值)
                    List<String> consumed = this.extractEntities(actionElement, "consumed");

                    // 解析 produced (获取 <entity> 里的值)
                    List<String> produced = this.extractEntities(actionElement, "produced");

                    // 获取 narration
                    String narration = actionElement.getElementsByTagName("narration").item(0).getTextContent().trim();

                    // 创建 GameAction 对象并添加到 actions Map
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

    /**
     * 从 XML 文件中解析指定节点的 <entity> 值
     *
     * @param actionElement action 的 Element 节点
     * @param tagName       需要提取的标签 (如 "subjects", "consumed", "produced")
     * @return 该标签下的所有 entity 值的列表
     */
    private List<String> extractEntities(Element actionElement, String tagName) {
        List<String> entities = new ArrayList<>();
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
