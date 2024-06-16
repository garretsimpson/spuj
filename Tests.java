import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Tests {

  static Random rng = new Random();

  static void run() {
    // test1();
    perf1();
    // code1();
  }

  static void test1() {
    Shape shape1 = new Shape(0xdafd8739);
    System.out.println(shape1);
    Shape shape2 = new Shape(Ops.cutRight(shape1.intValue()));
    System.out.println(shape2);
    Shape shape3 = new Shape(Ops.stack(0x00010000, 0xffff));
    System.out.println(shape3);
  }

  static void perf1() {
    final int ROUNDS = 10;
    final int ITERS = 100000000;
    String name = "stack";
    IntUnaryOperator func1 = Ops::pinPush;
    IntBinaryOperator func2 = Ops::stack;

    int[] times = IntStream.range(0, ROUNDS).map((x) -> Tests.opPerf(name, func2, ITERS)).toArray();
    double aveTime = IntStream.of(times).average().getAsDouble();
    double mops = 1f * ITERS / 1000 / aveTime;
    System.out.printf("average time: %.2f, %.2f MOPS\n", aveTime, mops);
  }

  static int opPerf(String name, IntUnaryOperator func, int iters) {
    Random rng = new Random();
    int[] values = rng.ints(iters).toArray();
    long before = new Date().getTime();
    int[] results = IntStream.of(values).map(func).parallel().toArray();
    long after = new Date().getTime();
    int delta = (int) (after - before);

    System.out.printf("%04d %s(%08x) => %08x\n", delta, name, values[0], results[0]);
    return delta;
  }

  static int randomLayers(int value) {
    int mask;
    switch (rng.nextInt() % 4) {
    case 1:
      mask = 0x000f000f;
      break;
    case 2:
      mask = 0x00ff00ff;
      break;
    case 3:
      mask = 0x0fff0fff;
      break;
    default:
      mask = 0xffffffff;
      break;
    }
    return value & mask;
  }

  static int opPerf(String name, IntBinaryOperator func, int iters) {
    int[] v1 = rng.ints(iters).map(Tests::randomLayers).toArray();
    int[] v2 = rng.ints(iters).map(Tests::randomLayers).toArray();
    long before = new Date().getTime();
    int[] results = IntStream.range(0, iters).map(i -> func.applyAsInt(v1[i], v2[i])).parallel().toArray();
    long after = new Date().getTime();
    int delta = (int) (after - before);

    System.out.printf("%04d %s(%08x, %08x) => %08x\n", delta, name, v1[0], v2[0], results[0]);
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

    IntStream.rangeClosed(1, 20).peek(number -> System.out.println(number + " " + Thread.currentThread().getName()))
        .map(x -> -x).parallel().toArray();
  }

}
