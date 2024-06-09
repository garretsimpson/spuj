import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Main
 */
public class Main {

  static final String VERSION = "SPU 1.0";

  public static void main(String[] args) {
    System.out.println(VERSION);
    System.out.println(new Date());
    System.out.println();

    ShapeTests.run();
    OpTests.run();
  }

  static int plusone(int x) {
    return x + 1;
  }

  static void tests() {
    Integer[] a = new Integer[] { 1, 2, 3, 4, 5 };
    System.out.println(Arrays.toString(a));
    List<Integer> b = Arrays.asList(a);
    List<Integer> c = Arrays.asList(1, 2, 3);

    Stream.of(b);
    b.stream();

    Integer[] d = Stream.of(a).map((x) -> x + 1).toArray(Integer[]::new);
    System.out.println(Arrays.toString(d));
    Integer[] e = Stream.of(a).map(Main::plusone).toArray(Integer[]::new);
    System.out.println(Arrays.toString(e));

    int[] i1 = new int[] { 1, 2, 3, 4 };
    IntUnaryOperator f = Ops::cutLeft;
    int[] i2 = IntStream.of(i1).map(f).toArray();
    System.out.println(Arrays.toString(i2));
  }

}
