import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shape
 */
public class Shape {

  static final String ROCKET = "CbCuCbCu:Sr------:--CrSrCr:CwCwCwCw"; // 0xFE1F
  static final String LOGO = "RuCw--Cw:----Ru--"; // 0x004B
  static final String HALF_RECT = "RuRu----"; // 0x0003
  static final String FULL_CIRC = "CuCuCuCu"; // 0x000F

  static final int[] FLAT_1 = { 0x1, 0x2, 0x4, 0x8 };
  static final int[] FLAT_2 = { 0x3, 0x5, 0x6, 0x9, 0xa, 0xc };
  static final int[] FLAT_3 = { 0x7, 0xb, 0xd, 0xe };
  static final int[] FLAT_4 = { 0xf };

  static final int[] PIN_1 = { 0x10000, 0x20000, 0x40000, 0x80000 };
  static final int[] PIN_4 = { 0xf0000 };

  public static final int PIN_MASK = 0x00010000;
  public static final int SOLID_MASK = 0x00000001;
  public static final int CRYSTAL_MASK = 0x00010001;
  public static final int LAYER_MASK = 0x000f000f;

  public static final int NUM_LAYERS = 4;
  public static final int NUM_QUADS = 4;
  public static final int NUM_SPOTS = NUM_LAYERS * NUM_QUADS;
  private static final char CIRC = 'C';
  private static final char RECT = 'R';
  private static final char STAR = 'S';
  private static final char WIND = 'W';
  private static final char CRYS = 'c';
  private static final char PIN = 'P';
  private static final String SEP = ":";

  private String code = "";
  private int intValue = 0;

  public Shape(String code) throws Exception {
    if (!verifyCode(code))
      throw new Exception("Invalid shape code");
    this.code = code;
    this.intValue = parseCode(code);
  }

  public Shape(int value) {
    this.code = makeCode(value);
    this.intValue = value;
  }

  public String toString() {
    return String.format("value: %08x, code: %s", intValue, code);
  }

  private boolean verifyCode(String code) {
    String RE = "((?:[CRSWc][urygcbmw])|(?:P-)|(?:--)){4}";
    String[] values = code.split(SEP);
    if (values.length < 1 || values.length > 4)
      return false;
    for (String val : values) {
      if (!val.matches(RE))
        return false;
    }
    return true;
  }

  private int parseCode(String code) {
    String[] spots = new String[NUM_SPOTS];
    String[] values = code.split(SEP);
    Pattern p = Pattern.compile(".{2}");
    Matcher m;
    int num = 0;
    for (String val : values) {
      m = p.matcher(val);
      while (m.find()) {
        spots[num++] = m.group();
      }
    }

    int v1 = 0, v2 = 0;
    String s;
    for (int i = NUM_SPOTS - 1; i >= 0; --i) {
      v1 <<= 1;
      v2 <<= 1;
      s = spots[i];
      if (s == null)
        continue;
      switch (s.charAt(0)) {
      case 'C':
      case 'R':
      case 'S':
      case 'W':
        v1 += 1;
        break;
      case 'P':
        v2 += 1;
        break;
      case 'c':
        v1 += 1;
        v2 += 1;
        break;
      }
    }
    // System.out.printf("%08x\n", intValue);
    return (v2 << 16) + v1;
  }

  private String toBin(int value) {
    String num = Integer.toString(v1(value), 2);
    String pad = "0".repeat(16 - num.length());
    return pad + num;
  }

  private String makeCode(int value) {
    String COLORS = "rgbw";
    String EMPTY = "--";

    String bin1 = toBin(v1(value));
    String bin2 = toBin(v2(value));
    String num, val = "";
    char color;
    String result = "";
    for (int i = 0; i < NUM_SPOTS; ++i) {
      num = "" + bin2.charAt(NUM_SPOTS - i - 1) + bin1.charAt(NUM_SPOTS - i - 1);
      color = COLORS.charAt(i / 4);
      switch (num) {
      case "00":
        val = EMPTY;
        break;
      case "01":
        val = "" + RECT + color;
        break;
      case "10":
        val = "" + PIN + '-';
        break;
      case "11":
        val = "" + CRYS + color;
        break;
      }
      if (i == 4 || i == 8 || i == 12) {
        result += SEP;
      }
      result += val;
    }

    return result;
  }

  public int intValue() {
    return intValue;
  }

  public static int v1(int value) {
    return value & 0xffff;
  }

  public static int v2(int value) {
    return value >>> 16;
  }

  /**
   * Returns an array of shapes, layers from bottom to top
   * 
   * @param code
   * @return
   */
  public static int[] toLayers(int value) {
    int[] result = new int[NUM_LAYERS];
    int v1 = v1(value);
    int v2 = v2(value);
    for (int i = 0; i < NUM_LAYERS; ++i) {
      value = ((v2 & 0xf) << 4) | (v1 & 0xf);
      result[i] = value;
      v1 >>>= 4;
      v2 >>>= 4;
    }
    return result;
  }

  static boolean hasCrystal(int shape) {
    return (v1(shape) & v2(shape)) != 0;
  }

  static boolean isOneLayer(int shape) {
    return (shape & LAYER_MASK) == shape;
  }

  static boolean isLeftHalf(int shape) {
    int v1 = v1(shape);
    int v2 = v2(shape);
    return ((v1 | v2) & 0x3333) == 0;
  }

  static boolean isRightHalf(int shape) {
    int v1 = v1(shape);
    int v2 = v2(shape);
    return ((v1 | v2) & 0xcccc) == 0;
  }

  static boolean isKeyShape(int shape) {
    return shape == Ops.keyValue(shape);
  }

}
