package edu.uob;
import java.util.*;

public class CommandParser {
    private final Set<String> decorativeWords;

    public CommandParser(Set<String> decorativeWords) {
        this.decorativeWords = decorativeWords;
    }

    public String normalizeCommand(String command) {
        command = command.toLowerCase();

        // 用 LinkedList 代替 ArrayList
        Deque<String> wordsDeque = new LinkedList<>();
        try (Scanner scanner = new Scanner(command)) {
            while (scanner.hasNext()) {
                wordsDeque.add(scanner.next());
            }
        }

        // 移除修饰词
        wordsDeque.removeAll(decorativeWords);

        // 使用 StringBuilder 进行拼接，避免使用 `String.join()`
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = wordsDeque.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next());
            if (iterator.hasNext()) {
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    public Set<String> extractCommandWords(String command) {
        StringBuilder commandPartBuilder = new StringBuilder(command);
        int colonIndex = commandPartBuilder.lastIndexOf(":");
        String commandPart;
        if (colonIndex >= 0) {
            commandPart = commandPartBuilder.substring(colonIndex + 1).trim();
        } else {
            commandPart = "";
        }

        Set<String> commandWords = new LinkedHashSet<>();
        try (Scanner scanner = new Scanner(commandPart)) {
            while (scanner.hasNext()) {
                commandWords.add(scanner.next());
            }
        }
        return commandWords;
    }
}
