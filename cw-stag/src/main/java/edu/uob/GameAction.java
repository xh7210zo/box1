package edu.uob;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class GameAction {
    private Set<String> triggers;  // 触发短语
    private Set<String> subjects;  // 动作的主体
    private Set<String> consumed;  // 消耗品
    private Set<String> produced;  // 产生物品
    private String narration;      // 叙述

    // 构造函数
    public GameAction(List<String> triggers, List<String> subjects, List<String> consumed, List<String> produced, String narration) {
        this.triggers = new HashSet<>(triggers);
        this.subjects = new HashSet<>(subjects);
        this.consumed = new HashSet<>(consumed);
        this.produced = new HashSet<>(produced);
        this.narration = narration;
    }

    public Set<String> getTriggers() {
        return triggers;
    }

    public Set<String> getSubjects() {
        return subjects;
    }

    public Set<String> getConsumed() {
        return consumed;
    }

    public Set<String> getProduced() {
        return produced;
    }

    public String getNarration() {
        return narration;
    }

    // 判断当前动作是否匹配给定的动词和主题
    public boolean matches(String actionVerb, List<String> commandSubjects) {
        Set<String> commandSet = new HashSet<>(commandSubjects);

        // 1️⃣ 检查动作关键字是否匹配
        if (!triggers.contains(actionVerb.toLowerCase())) {
            return false;
        }

        // 2️⃣ 确保所有必需的主题都包含在命令中
        return commandSet.containsAll(subjects);
    }
}
