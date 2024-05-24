import java.util.Date;

/**
 * Main
 */
public class Main {

  static final String VERSION = "SPU 1.0";

  static final String FULL_CIRC = "CuCuCuCu"; // 0x000F
  static final String HALF_RECT = "RuRu----"; // 0x0003
  static final String LOGO = "RuCw--Cw:----Ru--"; // 0x004B
  static final String ROCKET = "CbCuCbCu:Sr------:--CrSrCr:CwCwCwCw"; // 0xFE1F

  public static void main(String[] args) {
    System.out.println(VERSION);
    System.out.println(new Date());
    System.out.println();

    Boolean pass;
    final String[] CODES = { "123", "Cu", "--------", "--", "P---P---", LOGO, ROCKET};
    for (String code : CODES) {
      pass = true;
      try {
        new Shape(code);
      } catch (Exception e) {
        pass = false;
      }
      System.out.print(pass ? "PASS " : "FAIL ");
      System.out.println(code);
    }

    Shape circle = null;
    try {
      circle = new Shape(FULL_CIRC);
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("circle: " + circle);

    // ShapezOps ops = new ShapezOps();
    // int s1 = Shape.intValue(circle.toString());
    // int s2 = ops.cutRight(s1);
    // System.out.println("Shape1: " + Shape.valueOf(s1));
    // System.out.println("Shape2: " + Shape.valueOf(s2));

  }

}
