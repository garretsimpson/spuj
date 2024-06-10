import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Tests {
  static void run() {
    // code1();
    perf1();
    // test1();
  }

  static void test1() {
    Shape shape1 = new Shape(0xdafd8739);
    System.out.println(shape1);
    Shape shape2 = new Shape(Ops.cutRight(shape1.intValue()));
    System.out.println(shape2);
  }

  static void perf1() {
    final int ROUNDS = 20;
    final int ITERS = 1000000;
    int[] rounds = new int[ROUNDS];
    String name = "pinPush";
    IntUnaryOperator func = Ops::pinPush;

    int[] times = IntStream.of(rounds).map((x) -> Tests.opPerf(name, func, ITERS)).toArray();
    OptionalInt totalTime = IntStream.of(times).reduce((a, b) -> a + b);
    double aveTime = 1f * totalTime.getAsInt() / ROUNDS;
    System.out.printf("average time: %.2f, %.2f MOPS\n", aveTime, 1000f / aveTime);
  }

  static int opPerf(String name, IntUnaryOperator func, int iters) {
    Random rng = new Random();
    int[] values = rng.ints(iters).toArray();
    long before = new Date().getTime();
    int[] results = IntStream.of(values).map(func).toArray();
    long after = new Date().getTime();
    int delta = (int) (after - before);

    System.out.printf("%04d %s(%08x) => %08x\n", delta, name, values[0], results[0]);
    return delta;
  }

  static int plusone(int x) {
    return x + 1;
  }

  static void code1() {
    Integer[] a = new Integer[] { 1, 2, 3, 4, 5 };
    System.out.println(Arrays.toString(a));
    List<Integer> b = Arrays.asList(a);
    List<Integer> c = Arrays.asList(1, 2, 3);

    Stream.of(b);
    c.stream();

    Integer[] d = Stream.of(a).map((x) -> x + 1).toArray(Integer[]::new);
    System.out.println(Arrays.toString(d));
    Integer[] e = Stream.of(a).map(Tests::plusone).toArray(Integer[]::new);
    System.out.println(Arrays.toString(e));

    int[] i1 = new int[] { 1, 2, 3, 4 };
    IntUnaryOperator f = Ops::cutLeft;
    int[] i2 = IntStream.of(i1).map(f).toArray();
    System.out.println(Arrays.toString(i2));
  }

}
