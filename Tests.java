import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Tests {

  static Random rng = new Random();
  static Set<Integer> allShapes = new HashSet<>();

  private IntStream allShapeStream() {
    return allShapes.stream().mapToInt(Integer::intValue);
    // .peek(num -> System.out.println(String.format("%08x", num) + " " + Thread.currentThread().getName()));
  }

  static void run() {
    // test1();
    // perf1();
    // code1();
    readFile1();
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

  /* Misc analysis */
  void Find1() {
    int[] lefts = allShapeStream().filter(Shape::isLeftHalf).toArray();
    int[] rights = allShapeStream().filter(Shape::isRightHalf).toArray();
    Set<Integer> workSet = new HashSet<>(allShapes);
    int[] shapes;
    // Get list of all shapes that can be swapped.
    Set<Integer> swapped = IntStream.of(lefts).mapMulti((left, consumer) -> {
      for (int right : rights)
        consumer.accept(Ops.fastSwapRight(left, right));
    }).boxed().collect(Collectors.toSet());
    // Remove them from the list of all shapes.
    workSet.removeAll(swapped);
    // Filter out the lefts and rights.
    Set<Integer> halves = new HashSet<>();
    halves.addAll(IntStream.of(lefts).boxed().collect(Collectors.toSet()));
    halves.addAll(IntStream.of(lefts).map(Ops::rotateRight).boxed().collect(Collectors.toSet()));
    halves.addAll(IntStream.of(lefts).map(Ops::rotate180).boxed().collect(Collectors.toSet()));
    halves.addAll(IntStream.of(lefts).map(Ops::rotateLeft).boxed().collect(Collectors.toSet()));
    workSet.removeAll(halves);
    // Display the remaining.
    shapes = workSet.stream().mapToInt(Integer::intValue).sorted().toArray();
    Tools.displayShapes(shapes);
  }

  /* Find all shapes that cannot be made by swapping */
  void find2() {
    Ops.Stats.clear();
    // Find all shapes that can be made by swapping
    int[] shapes;
    // Set<Integer> workSet;
    int[] lefts = allShapeStream().filter(Shape::isLeftHalf).toArray();
    int[] rights = allShapeStream().filter(Shape::isRightHalf).toArray();
    // Stream stream = allShapeStream().mapMulti((left, consumer) -> {
    // for (int right : allShapes)
    // consumer.accept(Ops.swapRight(left, right));
    // });
    IntStream stream = IntStream.of(lefts).mapMulti((left, consumer) -> {
      for (int right : rights)
        consumer.accept(Ops.fastSwapRight(left, right));
    });
    Set<Integer> allSwapKeys = stream.parallel().filter(s -> (s == Ops.keyValue(s))).boxed()
        .collect(Collectors.toSet());
    shapes = allShapeStream().filter(s -> !allSwapKeys.contains(Ops.keyValue(s))).parallel().toArray();
    Tools.displayShapes(shapes);
  }

  void writeTest1() {
    final String filename = "test1.txt";
    System.out.printf("Writing file: %s\n", filename);
    ShapeFile.write(filename, allShapeStream().sorted().toArray());
  }

  static void readFile1() {
    String name = "allShapes1.txt";
    System.out.printf("Read file: %s\n", name);
    int[] shapes = ShapeFile.read(name);
    Tools.displayShapes(shapes);
  }

}
