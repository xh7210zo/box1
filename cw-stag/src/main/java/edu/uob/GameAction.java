package edu.uob;

import java.util.Set;
import java.util.HashSet;

public class GameAction {
    // 触发短语
    private Set<String> subjects;  // 动作的主体
    private Set<String> consumed;  // 消耗品
    private  Set<String> produced;  // 产生物品
    private  String narration;      // 叙述

    // 构造函数
    public GameAction(Set<String> subjects, Set<String> consumed, Set<String> produced, String narration) {
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
