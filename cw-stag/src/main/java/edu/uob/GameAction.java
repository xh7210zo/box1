package edu.uob;

import java.util.List;

public class GameAction {
    private List<String> triggers; // 触发短语
    private List<String> subjects; // 动作的主体
    private List<String> consumed; // 消耗品
    private List<String> produced; // 产生物品
    private String narration; // 叙述

    // 构造函数
    public GameAction(List<String> triggers, List<String> subjects, List<String> consumed, List<String> produced, String narration) {
        // 调试信息：打印传入的参数
        System.out.println("[GameAction] Creating new GameAction with the following parameters:");
        System.out.println("[GameAction] Triggers: " + triggers);
        System.out.println("[GameAction] Subjects: " + subjects);
        System.out.println("[GameAction] Consumed: " + consumed);
        System.out.println("[GameAction] Produced: " + produced);
        System.out.println("[GameAction] Narration: " + narration);

        this.triggers = triggers;
        this.subjects = subjects;
        this.consumed = consumed;
        this.produced = produced;
        this.narration = narration;
    }

    // Getters 和 Setters
    public List<String> getTriggers() {
        System.out.println("[GameAction] getTriggers() called. Returning: " + triggers);
        return triggers;
    }

    public List<String> getSubjects() {
        System.out.println("[GameAction] getSubjects() called. Returning: " + subjects);
        return subjects;
    }

    public List<String> getConsumed() {
        System.out.println("[GameAction] getConsumed() called. Returning: " + consumed);
        return consumed;
    }

    public List<String> getProduced() {
        System.out.println("[GameAction] getProduced() called. Returning: " + produced);
        return produced;
    }

    public String getNarration() {
        System.out.println("[GameAction] getNarration() called. Returning: " + narration);
        return narration;
    }
}
