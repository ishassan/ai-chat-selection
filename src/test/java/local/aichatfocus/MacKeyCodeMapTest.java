package local.aichatfocus;

import java.awt.event.KeyEvent;
import java.util.Arrays;

public final class MacKeyCodeMapTest {
  private MacKeyCodeMapTest() {}

  public static void main(String[] args) {
    assertMapping("S", KeyEvent.VK_S, 1);
    assertMapping("slash", KeyEvent.VK_SLASH, 44);
    assertMapping("backtick", KeyEvent.VK_BACK_QUOTE, 50);
    assertMapping("F13", KeyEvent.VK_F13, 105);
    assertMapping("F14", KeyEvent.VK_F14, 107);
    assertMapping("F15", KeyEvent.VK_F15, 113);
    assertMapping("F16", KeyEvent.VK_F16, 106);
    assertMapping("F17", KeyEvent.VK_F17, 64);
    assertMapping("F18", KeyEvent.VK_F18, 79);
    assertMapping("F19", KeyEvent.VK_F19, 80);
    assertMapping("F20", KeyEvent.VK_F20, 90);
    assertMapping("ISO less-than key", KeyEvent.VK_LESS, 10);
    assertMapping("Context menu", KeyEvent.VK_CONTEXT_MENU, 110);
    assertMapping("Help", KeyEvent.VK_HELP, 114);
    int[] macNumpadDigits = {82, 83, 84, 85, 86, 87, 88, 89, 91, 92};
    for (int digit = 0; digit <= 9; digit++) {
      assertMapping("keypad " + digit, KeyEvent.VK_NUMPAD0 + digit, macNumpadDigits[digit]);
    }
    assertMapping("keypad decimal", KeyEvent.VK_DECIMAL, 65);
    assertMapping("keypad multiply", KeyEvent.VK_MULTIPLY, 67);
    assertMapping("keypad plus", KeyEvent.VK_ADD, 69);
    assertMapping("keypad clear", KeyEvent.VK_CLEAR, 71);
    assertMapping("keypad divide", KeyEvent.VK_DIVIDE, 75);
    assertMapping("keypad minus", KeyEvent.VK_SUBTRACT, 78);
    assertMapping("keypad equals", KeyEvent.VK_EQUALS, 24, 81);
    assertMapping("Return and keypad Enter", KeyEvent.VK_ENTER, 36, 76);

    for (int macKeyCode = 0; macKeyCode <= 48; macKeyCode++) {
      int awtKeyCode = MacKeyCodeMap.toAwtKeyCode(macKeyCode);
      if (awtKeyCode == KeyEvent.VK_UNDEFINED
          || !contains(MacKeyCodeMap.fromAwtKeyCode(awtKeyCode), macKeyCode)) {
        throw new AssertionError("Main keyboard key does not round-trip: " + macKeyCode);
      }
    }

    for (int macKeyCode : new int[]{65, 67, 69, 71, 75, 76, 78, 81, 82, 83, 84, 85, 86, 87, 88, 89, 91, 92}) {
      if (MacKeyCodeMap.keyLocationForMacCode(macKeyCode) != KeyEvent.KEY_LOCATION_NUMPAD) {
        throw new AssertionError("Expected keypad location for macOS key code " + macKeyCode);
      }
    }
    if (MacKeyCodeMap.keyLocationForMacCode(105) != KeyEvent.KEY_LOCATION_STANDARD) {
      throw new AssertionError("F13 must use the standard key location");
    }

    System.out.println("Mac key-code mapping checks passed");
  }

  private static void assertMapping(String label, int awtKeyCode, int... expectedMacCodes) {
    int[] actualMacCodes = MacKeyCodeMap.fromAwtKeyCode(awtKeyCode);
    if (!Arrays.equals(expectedMacCodes, actualMacCodes)) {
      throw new AssertionError(label + " expected " + Arrays.toString(expectedMacCodes)
          + " but got " + Arrays.toString(actualMacCodes));
    }
    for (int macKeyCode : expectedMacCodes) {
      if (MacKeyCodeMap.toAwtKeyCode(macKeyCode) != awtKeyCode) {
        throw new AssertionError(label + " does not map back from macOS key code " + macKeyCode);
      }
    }
  }

  private static boolean contains(int[] values, int target) {
    for (int value : values) {
      if (value == target) return true;
    }
    return false;
  }
}
