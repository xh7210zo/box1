package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

class ExampleSTAGTests {

  private GameServer server;

  // Create a new server _before_ every @Test
  @BeforeEach
  void setup() {
      File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
      File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
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


      String response = sendCommandToServer("please simon: please LOOK");

      response = response.toLowerCase();

      // 打印响应内容，帮助调试
      assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
      assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
      assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
      assertTrue(response.contains("wooden trapdoor"), "Did not see description of furniture in response to look");
      assertTrue(response.contains("forest"), "Did not see available paths in response to look");

    }



    // Test that we can pick something up and that it appears in our inventory
  @Test
  void testGet()
  {
      String response;
      sendCommandToServer("simon:potion get");
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
      sendCommandToServer("simon:key get");
      response = sendCommandToServer("simon: iNv");
      response = response.toLowerCase();
      assertTrue(response.contains("key"), "Did not see the key in the inventory after an attempt was made to get it");
      response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertFalse(response.contains("key"), "key is still present in the room after an attempt was made to get it");
      sendCommandToServer("simon: goto cabin");
      response = sendCommandToServer("simon: iNv");
      response = response.toLowerCase();
      assertTrue(response.contains("key"), "Did not see the key in the inventory after an attempt was made to get it");
      response = sendCommandToServer("simon: open trapdoor with key");
      assertTrue(response.contains("You unlock the door"), "Expected narration not found in response");
      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("cellar"), "Cellar not found after unlocking trapdoor");
      sendCommandToServer("simon:axe get");
      sendCommandToServer("simon: goto foRest");
      response = sendCommandToServer("simon: iNv");
      response = response.toLowerCase();
      assertTrue(response.contains("axe"), "Did not see the axe in the inventory after an attempt was made to get it");
      response = sendCommandToServer("simon: use axe to chop tree");
      assertTrue(response.contains("cut down the tree"), "Tree chopping narration missing");

      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("log"), "Log not found after chopping tree");

//      // （drink potion）
      sendCommandToServer("simon: goto cabin");
      sendCommandToServer("simon: potion get");
      response = sendCommandToServer("simon: health");
      assertTrue(response.contains("3"), "Health should start at 3");
//
      sendCommandToServer("simon: drink potion");
      response = sendCommandToServer("simon: health");
      assertTrue(response.contains("3"), "Health should remain at max (3) after drinking potion");
//
      // fight elf）
      sendCommandToServer("simon: goto cellar");
      sendCommandToServer("simon: look");
      response = sendCommandToServer("simon: fight elf");
      assertTrue(response.contains("you lose some health"), "Expected fight narration missing");
//
      response = sendCommandToServer("simon: health");
      assertTrue(response.contains("2"), "Health should decrease after fighting the elf");
//
      sendCommandToServer("simon: fight elf");
      response = sendCommandToServer("simon: health");
      assertTrue(response.contains("1"), "Health should be 1 after second fight");
//
      sendCommandToServer("simon: fight elf");
      response = sendCommandToServer("simon: health");
      assertTrue(response.contains("3"), "Health should reset to 3 after death");
//
      response = sendCommandToServer("simon: iNv");
      assertFalse(response.contains("potion"), "Inventory should be empty after death");

      // （pay elf）
      sendCommandToServer("simon: goto market");
      sendCommandToServer("simon: look");
      sendCommandToServer("simon: coin get");
      sendCommandToServer("simon: goto cellar");
      response = sendCommandToServer("simon: pay elf coin");
      assertTrue(response.contains("shovel"), "Shovel should be received after paying elf");

//
      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("shovel"), "Shovel should exist");
      assertFalse(response.contains("coin"), "Coin should be consumed after payment");

      // （bridge log over river）
      sendCommandToServer("simon: goto cabin");
     sendCommandToServer("simon: get axe");
      sendCommandToServer("simon: goto forest");
     sendCommandToServer("simon: chop tree axe");
      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("log"), "Log should be present after chopping tree");

      sendCommandToServer("simon: log get");
      sendCommandToServer("simon: goto riverbank");
      response = sendCommandToServer("simon: bridge log over river");
      assertTrue(response.contains("reach the other side"), "Expected bridge narration missing");

      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("clearing"), "Clearing should be visible after bridging river");

      sendCommandToServer("simon: goto forest");
      sendCommandToServer("simon: look");
      sendCommandToServer("simon: goto cabin");
      sendCommandToServer("simon: look");
      sendCommandToServer("simon: goto cellar");
      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("shovel"), "Shovel should exist");
      sendCommandToServer("simon: shovel get");

     // （dig ground with shovel）
      sendCommandToServer("simon: goto cabin");
      sendCommandToServer("simon: goto forest");
      sendCommandToServer("simon: goto riverbank");
      sendCommandToServer("simon: goto clearing");
      sendCommandToServer("simon: look");
      response = sendCommandToServer("simon: dig ground with shovel");
      assertTrue(response.contains("pot of gold"), "Gold should be found after digging");

      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("hole"), "Hole should be visible after digging");
      sendCommandToServer("simon: goto riverbank");
      sendCommandToServer("simon: horn get");
      sendCommandToServer("simon: goto forest");
      // （blow horn）
      sendCommandToServer("simon: goto cabin");
      response = sendCommandToServer("simon: blow horn");
      assertTrue(response.contains("lumberjack appears"), "Lumberjack should appear after blowing horn");

      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("cutter"), "Lumberjack should be in the room after blowing horn");

  }

  // Add more unit tests or integration tests here.



}
