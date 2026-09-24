package local.aichatfocus;

import java.awt.event.KeyEvent;

/** Maps Java key codes to macOS hardware key codes used by NSEvent. */
final class MacKeyCodeMap {
  private static final int[] LETTERS = {0, 11, 8, 2, 14, 3, 5, 4, 34, 38, 40, 37, 46,
      45, 31, 35, 12, 15, 1, 17, 32, 9, 13, 7, 16, 6};
  private static final int[] DIGITS = {29, 18, 19, 20, 21, 23, 22, 26, 28, 25};
  private static final int[] NUMPAD_DIGITS = {82, 83, 84, 85, 86, 87, 88, 89, 91, 92};
  private static final int[] MAC_KEY_CODES = {KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_F,
      KeyEvent.VK_H, KeyEvent.VK_G, KeyEvent.VK_Z, KeyEvent.VK_X, KeyEvent.VK_C,
      KeyEvent.VK_V, KeyEvent.VK_LESS, KeyEvent.VK_B, KeyEvent.VK_Q,
      KeyEvent.VK_W, KeyEvent.VK_E, KeyEvent.VK_R, KeyEvent.VK_Y, KeyEvent.VK_T,
      KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_6,
      KeyEvent.VK_5, KeyEvent.VK_EQUALS, KeyEvent.VK_9, KeyEvent.VK_7,
      KeyEvent.VK_MINUS, KeyEvent.VK_8, KeyEvent.VK_0, KeyEvent.VK_CLOSE_BRACKET,
      KeyEvent.VK_O, KeyEvent.VK_U, KeyEvent.VK_OPEN_BRACKET, KeyEvent.VK_I,
      KeyEvent.VK_P, KeyEvent.VK_ENTER, KeyEvent.VK_L, KeyEvent.VK_J,
      KeyEvent.VK_QUOTE, KeyEvent.VK_K, KeyEvent.VK_SEMICOLON, KeyEvent.VK_BACK_SLASH,
      KeyEvent.VK_COMMA, KeyEvent.VK_SLASH, KeyEvent.VK_N, KeyEvent.VK_M,
      KeyEvent.VK_PERIOD};

  private MacKeyCodeMap() {}

  static int[] fromAwtKeyCode(int keyCode) {
    if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
      return one(LETTERS[keyCode - KeyEvent.VK_A]);
    }
    if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9) {
      return one(DIGITS[keyCode - KeyEvent.VK_0]);
    }
    if (keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9) {
      return one(NUMPAD_DIGITS[keyCode - KeyEvent.VK_NUMPAD0]);
    }

    if (keyCode == KeyEvent.VK_EQUALS) {
      return new int[]{24, 81};
    }
    if (keyCode == KeyEvent.VK_ENTER) {
      return new int[]{36, 76};
    }

    int macKeyCode = switch (keyCode) {
      case KeyEvent.VK_MINUS -> 27;
      case KeyEvent.VK_OPEN_BRACKET -> 33;
      case KeyEvent.VK_CLOSE_BRACKET -> 30;
      case KeyEvent.VK_BACK_SLASH -> 42;
      case KeyEvent.VK_SEMICOLON -> 41;
      case KeyEvent.VK_QUOTE -> 39;
      case KeyEvent.VK_COMMA -> 43;
      case KeyEvent.VK_PERIOD -> 47;
      case KeyEvent.VK_SLASH -> 44;
      case KeyEvent.VK_BACK_QUOTE -> 50;
      case KeyEvent.VK_TAB -> 48;
      case KeyEvent.VK_SPACE -> 49;
      case KeyEvent.VK_BACK_SPACE -> 51;
      case KeyEvent.VK_ESCAPE -> 53;
      case KeyEvent.VK_DELETE -> 117;
      case KeyEvent.VK_HOME -> 115;
      case KeyEvent.VK_END -> 119;
      case KeyEvent.VK_PAGE_UP -> 116;
      case KeyEvent.VK_PAGE_DOWN -> 121;
      case KeyEvent.VK_LEFT -> 123;
      case KeyEvent.VK_RIGHT -> 124;
      case KeyEvent.VK_DOWN -> 125;
      case KeyEvent.VK_UP -> 126;
      case KeyEvent.VK_F1 -> 122;
      case KeyEvent.VK_F2 -> 120;
      case KeyEvent.VK_F3 -> 99;
      case KeyEvent.VK_F4 -> 118;
      case KeyEvent.VK_F5 -> 96;
      case KeyEvent.VK_F6 -> 97;
      case KeyEvent.VK_F7 -> 98;
      case KeyEvent.VK_F8 -> 100;
      case KeyEvent.VK_F9 -> 101;
      case KeyEvent.VK_F10 -> 109;
      case KeyEvent.VK_F11 -> 103;
      case KeyEvent.VK_F12 -> 111;
      case KeyEvent.VK_DECIMAL -> 65;
      case KeyEvent.VK_MULTIPLY -> 67;
      case KeyEvent.VK_ADD -> 69;
      case KeyEvent.VK_CLEAR -> 71;
      case KeyEvent.VK_DIVIDE -> 75;
      case KeyEvent.VK_SUBTRACT -> 78;
      case KeyEvent.VK_F17 -> 64;
      case KeyEvent.VK_F18 -> 79;
      case KeyEvent.VK_F19 -> 80;
      case KeyEvent.VK_F20 -> 90;
      case KeyEvent.VK_F13 -> 105;
      case KeyEvent.VK_F16 -> 106;
      case KeyEvent.VK_F14 -> 107;
      case KeyEvent.VK_F15 -> 113;
      case KeyEvent.VK_CONTEXT_MENU -> 110;
      case KeyEvent.VK_HELP -> 114;
      case KeyEvent.VK_LESS -> 10;
      default -> -1;
    };
    return one(macKeyCode);
  }

  static int toAwtKeyCode(int macKeyCode) {
    if (macKeyCode >= 0 && macKeyCode < MAC_KEY_CODES.length) {
      return MAC_KEY_CODES[macKeyCode];
    }

    return switch (macKeyCode) {
      case 48 -> KeyEvent.VK_TAB;
      case 49 -> KeyEvent.VK_SPACE;
      case 50 -> KeyEvent.VK_BACK_QUOTE;
      case 51 -> KeyEvent.VK_BACK_SPACE;
      case 53 -> KeyEvent.VK_ESCAPE;
      case 64 -> KeyEvent.VK_F17;
      case 65 -> KeyEvent.VK_DECIMAL;
      case 67 -> KeyEvent.VK_MULTIPLY;
      case 69 -> KeyEvent.VK_ADD;
      case 71 -> KeyEvent.VK_CLEAR;
      case 75 -> KeyEvent.VK_DIVIDE;
      case 76 -> KeyEvent.VK_ENTER;
      case 78 -> KeyEvent.VK_SUBTRACT;
      case 79 -> KeyEvent.VK_F18;
      case 80 -> KeyEvent.VK_F19;
      case 81 -> KeyEvent.VK_EQUALS;
      case 82 -> KeyEvent.VK_NUMPAD0;
      case 83 -> KeyEvent.VK_NUMPAD1;
      case 84 -> KeyEvent.VK_NUMPAD2;
      case 85 -> KeyEvent.VK_NUMPAD3;
      case 86 -> KeyEvent.VK_NUMPAD4;
      case 87 -> KeyEvent.VK_NUMPAD5;
      case 88 -> KeyEvent.VK_NUMPAD6;
      case 89 -> KeyEvent.VK_NUMPAD7;
      case 90 -> KeyEvent.VK_F20;
      case 91 -> KeyEvent.VK_NUMPAD8;
      case 92 -> KeyEvent.VK_NUMPAD9;
      case 96 -> KeyEvent.VK_F5;
      case 97 -> KeyEvent.VK_F6;
      case 98 -> KeyEvent.VK_F7;
      case 99 -> KeyEvent.VK_F3;
      case 100 -> KeyEvent.VK_F8;
      case 101 -> KeyEvent.VK_F9;
      case 103 -> KeyEvent.VK_F11;
      case 105 -> KeyEvent.VK_F13;
      case 106 -> KeyEvent.VK_F16;
      case 107 -> KeyEvent.VK_F14;
      case 109 -> KeyEvent.VK_F10;
      case 111 -> KeyEvent.VK_F12;
      case 113 -> KeyEvent.VK_F15;
      case 110 -> KeyEvent.VK_CONTEXT_MENU;
      case 114 -> KeyEvent.VK_HELP;
      case 115 -> KeyEvent.VK_HOME;
      case 116 -> KeyEvent.VK_PAGE_UP;
      case 117 -> KeyEvent.VK_DELETE;
      case 118 -> KeyEvent.VK_F4;
      case 119 -> KeyEvent.VK_END;
      case 120 -> KeyEvent.VK_F2;
      case 121 -> KeyEvent.VK_PAGE_DOWN;
      case 122 -> KeyEvent.VK_F1;
      case 123 -> KeyEvent.VK_LEFT;
      case 124 -> KeyEvent.VK_RIGHT;
      case 125 -> KeyEvent.VK_DOWN;
      case 126 -> KeyEvent.VK_UP;
      default -> KeyEvent.VK_UNDEFINED;
    };
  }

  static int keyLocationForMacCode(int macKeyCode) {
    return switch (macKeyCode) {
      case 65, 67, 69, 71, 75, 76, 78, 81, 82, 83, 84, 85, 86, 87, 88, 89, 91, 92 ->
          KeyEvent.KEY_LOCATION_NUMPAD;
      default -> KeyEvent.KEY_LOCATION_STANDARD;
    };
  }

  private static int[] one(int keyCode) {
    return keyCode < 0 ? new int[0] : new int[]{keyCode};
  }
}
