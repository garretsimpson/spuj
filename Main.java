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

    makeShapes();
    ShapeFile.sort("BigData/shapes.db");
  }

  static void makeShapes() {
    Ops.Stats.clear();
    Constructor f = new Constructor();
    // Solver f = new Solver();
    long before = new Date().getTime();
    f.run();
    long after = new Date().getTime();
    System.out.printf("Time: %d\n", after - before);
    System.out.println(Ops.Stats.asString());

    // s.saveResults();
  }

}
