package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

class ExampleSTAGTestss {
    private boolean setupExecuted;
    private GameServer server;

    // Create a new server _before_ every @Test
    @BeforeEach
    void setup() {
        System.out.println("BeforeEach setup executed");
        setupExecuted = true;
        File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
        System.out.println("Entities file path: " + entitiesFile.getAbsolutePath());
        System.out.println("Actions file path: " + actionsFile.getAbsolutePath());
        server = new GameServer(entitiesFile, actionsFile);
    }

    String sendCommandToServer(String command) {
        System.out.println("Sending command to server: " + command);  // 打印发送的命令
        String response = assertTimeoutPreemptively(Duration.ofMillis(1000), () -> {
            return server.handleCommand(command);
        }, "Server took too long to respond (probably stuck in an infinite loop)");
        System.out.println("Received response: " + response);  // 打印服务器返回的响应
        return response;
    }

    // A lot of tests will probably check the game state using 'look' - so we better make sure 'look' works well !
    @Test
    void testLook() {
        System.out.println("Starting testLook...");
        assertTrue(setupExecuted, "BeforeEach setup was not executed before LOOK.");
        String response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        System.out.println("Look command response: " + response);  // 打印look命令的响应
        assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
        assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
        assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
        assertTrue(response.contains("wooden trapdoor"), "Did not see description of furniture in response to look");
        assertTrue(response.contains("forest"), "Did not see available paths in response to look");
    }

    // Test that we can pick something up and that it appears in our inventory
    @Test
    void testGet() {
        System.out.println("Starting testGet...");
        assertTrue(setupExecuted, "BeforeEach setup was not executed before GET.");
        String response;
        sendCommandToServer("simon: get potion");
        System.out.println("Potion picked up, checking inventory...");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        System.out.println("Inventory response: " + response);  // 打印inventory响应
        assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        System.out.println("Look response after getting potion: " + response);  // 打印再次look的响应
        assertFalse(response.contains("potion"), "Potion is still present in the room after an attempt was made to get it");
    }

    // Test that we can goto a different location (we won't get very far if we can't move around the game !)
    @Test
    void testGoto() {
        System.out.println("Starting testGoto...");
        assertTrue(setupExecuted, "BeforeEach setup was not executed before GOTO.");
        sendCommandToServer("simon: goto forest");
        System.out.println("Goto command sent, checking look...");
        String response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        System.out.println("Look response after goto forest: " + response);  // 打印goto forest后的响应
        assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
    }

    // Add more unit tests or integration tests here.
}
