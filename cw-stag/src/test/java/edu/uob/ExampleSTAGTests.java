package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.Duration;

class ExampleSTAGTests {

  private GameServer server;

  // Create a new server _before_ every @Test
  @BeforeEach
  void setup() {
      File entitiesFile = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
      File actionsFile = Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile();
      server = new GameServer(entitiesFile, actionsFile);
  }

  String sendCommandToServer(String command) {
      // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
      return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
      "Server took too long to respond (probably stuck in an infinite loop)");
  }

  // A lot of tests will probably check the game state using 'look' - so we better make sure 'look' works well !
  @Test
  void testLook() {
      // 创建玩家并初始化房间
      Room startingRoom = new Room("cabin", "A cozy log cabin.");
      Player player = new Player("simon", startingRoom);

      // 打印当前房间信息以检查是否为 null
      System.out.println("Current Room: " + player.getCurrentRoom());
      if (player.getCurrentRoom() != null) {
          System.out.println("Room Name: " + player.getCurrentRoom().getName());
      } else {
          System.out.println("Current room is null");
      }

      System.out.println("Before sending command: " + player.getCurrentRoom().getName());
      String response = sendCommandToServer("please simon: please LOOK");
      System.out.println("After sending command: " + player.getCurrentRoom().getName());

      response = response.toLowerCase();

      // 打印响应内容，帮助调试
      System.out.println("Server response to 'look' command: " + response);

      assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
      System.out.println("Checked for 'cabin' in the response.");

      assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
      System.out.println("Checked for 'log cabin' in the response.");

      assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
      System.out.println("Checked for 'magic potion' in the response.");

      assertTrue(response.contains("wooden trapdoor"), "Did not see description of furniture in response to look");
      System.out.println("Checked for 'wooden trapdoor' in the response.");

      assertTrue(response.contains("forest"), "Did not see available paths in response to look");
      System.out.println("Checked for 'forest' in the response.");
  }



    // Test that we can pick something up and that it appears in our inventory
  @Test
  void testGet()
  {
      String response;
      sendCommandToServer("simon: gEt potIon");
      response = sendCommandToServer("simon: iNv");
      response = response.toLowerCase();
      assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
      response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertFalse(response.contains("potion"), "Potion is still present in the room after an attempt was made to get it");
  }

  // Test that we can goto a different location (we won't get very far if we can't move around the game !)
  @Test
  void testGoto()
  {
      sendCommandToServer("simon: goto foRest");
      String response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
  }

  // Add more unit tests or integration tests here.

}
