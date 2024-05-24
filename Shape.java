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

  private void parseCode(String code) {
    String RE = ".{2}";
    int num = 0;

    String[] values = code.split(SEP);
    Pattern p = Pattern.compile(RE);
    Matcher m;
    for (String val : values) {
      m = p.matcher(val);
      while (m.find()) {
        spots[num++] = m.group();
      }
    }

    for (int i = 0; i < NUM_SPOTS; ++i) {
      System.out.print(spots[i] + " ");
    }
    System.out.println();
  }

  public String toString() {
    return code;
  }

  public int intValue() {
    return this.intValue;
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
