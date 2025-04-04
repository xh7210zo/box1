package edu.uob;

import java.util.Set;
import java.util.HashSet;

public class GameAction {

    private final Set<String> subjects;
    private final Set<String> consumed;
    private final Set<String> produced;
    private final String narration;

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