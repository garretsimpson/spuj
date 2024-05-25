import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Shape {

  private final int NUM_SPOTS = 16;
  private final String SEP = ":";

  private String code;
  private int intValue;

  private String[] spots = new String[NUM_SPOTS];

  public Shape(String code) throws Exception {
    if (!verifyCode(code))
      throw new Exception("Invalid shape code");
    this.code = code;
    parseCode(code);
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

  private void parseCode(String code) {
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
    intValue = (v2 << 16) + v1;
    // System.out.printf("%08x\n", intValue);
  }

  public String toString() {
    return code;
  }

  public int intValue() {
    return intValue;
  }

  private static int convertToInt(String code) {
    return 0;
  }

  public static int intValue(String code) {
    return Shape.convertToInt(code);
  }

  public static String valueOf(int value) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'valueOf'");
  }

}
