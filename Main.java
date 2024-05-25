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

    test();
  }

  enum Tests {
    T1("123"), T2("Cu"), EMPTY1("--------", 0), LOGO1(LOGO, 0x4b), ROCKET1(ROCKET, 0xfe1f),
    PINS1("P---P---", 0x00050000), CRYSTAL1("crP-crP-", 0x000f0005),
    CRYSTAL2("cccccccc:cccccccc:cccccccc:cccccccc", 0xffffffff);

    boolean valid;
    String code;
    int value;

    Tests(String code) {
      this.valid = false;
      this.code = code;
    }

    Tests(String code, int value) {
      this.valid = true;
      this.code = code;
      this.value = value;
    }

    public String toString() {
      return Integer.toString(value, 16);
    }
  }

  public static void test() {
    Shape s;
    Boolean pass;
    for (Tests t : Tests.values()) {
      System.out.println("TEST " + t.code);
      try {
        s = new Shape(t.code);
        pass = (t.valid == true) && (t.value == s.intValue());
      } catch (Exception e) {
        s = null;
        pass = (t.valid == false);
      }
      System.out.println(pass ? "PASS" : "FAIL");
      if (!pass && s != null) {
        System.out.printf("returned %08x, expected %08x\n", s.intValue(), t.value);
      }
    }

    System.out.println();
    Shape circle = null;
    try {
      circle = new Shape(FULL_CIRC);
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("circle: " + circle);

    ShapezOps ops = new ShapezOps();
    int s1 = circle.intValue();
    int s2 = ops.cutLeft(s1);
    int s3 = ops.cutRight(s1);
    System.out.println("Shape 1: " + new Shape(s1));
    System.out.println("Shape 2: " + new Shape(s2));
    System.out.println("Shape 3: " + new Shape(s3));

    System.out.println();
    int t = 0xffffffff;
    System.out.printf("%08x, %08x, %08x", t, Shape.v1(t), Shape.v2(t));
  }

}
