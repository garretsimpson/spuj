import java.util.Date;

/**
 * Main
 */
public class Main {

  static final String VERSION = "SPU 1.0";

  public static void main(String[] args) {
    System.out.println(VERSION);
    System.out.println(new Date());
    System.out.println();

    // ShapeTests.run();
    // OpTests.run();
    // Tests.run();

    Constructor c = new Constructor();
    c.run();
  }

}
