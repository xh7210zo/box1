package edu.uob;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class GameAction {
    // 触发短语
    private final Set<String> subjects;  // 动作的主体
    private final Set<String> consumed;  // 消耗品
    private final Set<String> produced;  // 产生物品
    private final String narration;      // 叙述

    // 构造函数
    public GameAction(List<String> subjects, List<String> consumed, List<String> produced, String narration) {
        this.subjects = new HashSet<>(subjects);
        this.consumed = new HashSet<>(consumed);
        this.produced = new HashSet<>(produced);
        this.narration = narration;
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

}
