import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Constructor
 */
public class Constructor {
  private static final int MAX_ITERS = 100;
  private static final int MAX_LAYERS = 2;

  private Set<Integer> allShapes = new HashSet<>();

  private IntStream allShapeStream() {
    return allShapes.stream().mapToInt(Integer::intValue);
    // .peek(num -> System.out.println(String.format("%08x", num) + " " + Thread.currentThread().getName()));
  }

  private static final IntUnaryOperator[] ONE_OPS_ALL = { Ops::rotateRight, Ops::rotate180, Ops::rotateLeft,
      Ops::cutLeft, Ops::cutRight, Ops::pinPush, Ops::crystal };
  private static final IntBinaryOperator[] TWO_OPS_ALL = { Ops::swapLeft, Ops::swapRight, Ops::stack };
  private static final IntUnaryOperator[] ONE_OPS = { Ops::rotateRight, Ops::cutRight, Ops::pinPush, Ops::crystal };
  private static final IntBinaryOperator[] TWO_OPS = { Ops::fastSwapRight, Ops::fastStack };

  private void displayShapes(int[] shapes) {
    if (shapes.length == 0) {
      System.out.println("No shapes to display");
      return;
    }
    for (int shape : shapes) {
      System.out.println(new Shape(shape));
    }
    System.out.printf("Number of shapes: %d\n", shapes.length);
    System.out.println();
  }

  private void displayAllShapes() {
    int shape;
    System.out.println("All shapes");
    for (long i = 0; i <= 0xffffffffl; ++i) {
      shape = (int) i;
      if (allShapes.contains(shape))
        System.out.println(new Shape(shape));
    }
    System.out.println();
  }

  private boolean isNew(int shape) {
    return !allShapes.contains(shape);
  }

  private boolean maxLayers(int shape) {
    if (shape == 0)
      return false;
    int v1 = Shape.v1(shape);
    int v2 = Shape.v2(shape);
    return (v1 | v2) < (1 << (4 * MAX_LAYERS));
  }

  /**
   * makeShapes
   * 
   * Given a list of starting shapes, find the shapes that can be made by performing all operations.
   */
  int[] makeShapes1(int[] input) {
    List<IntStream> streams = new ArrayList<>();

    System.out.printf("ONE_OPS %d %d > %d\n", ONE_OPS.length, input.length, 1l * ONE_OPS.length * input.length);
    for (IntUnaryOperator op : ONE_OPS) {
      streams.add(IntStream.of(input).map(op));
    }

    /* TODO: Add pre-filters for each op. */
    /* For example, swap only half shapes and stack only one-layer, non-crystal shapes. */
    System.out.printf("TWO_OPS %d %d %d > %d\n", TWO_OPS.length, input.length, allShapes.size(),
        1l * TWO_OPS.length * input.length * (input.length + 2 * allShapes.size()));
    for (IntBinaryOperator op : TWO_OPS) {
      streams.add(IntStream.of(input).mapMulti((s1, consumer) -> {
        for (int s2 : input)
          consumer.accept(op.applyAsInt(s1, s2));
      }));
      streams.add(allShapeStream().mapMulti((s1, consumer) -> {
        for (int s2 : input) {
          consumer.accept(op.applyAsInt(s1, s2));
          consumer.accept(op.applyAsInt(s2, s1));
        }
      }));
    }

    Set<Integer> inputShapes = IntStream.of(input).boxed().collect(Collectors.toSet());
    IntStream stream = streams.stream().flatMapToInt(s -> s).filter(this::maxLayers).filter(this::isNew)
        .filter(s -> !inputShapes.contains(s)).distinct();
    return stream.parallel().toArray();
  }

  void run1() {
    int[] shapes = IntStream.concat(IntStream.of(Shape.FLAT_4), IntStream.of(Shape.PIN_4)).toArray();
    int[] newShapes;

    System.out.println("Max iters: " + MAX_ITERS);
    System.out.println("Max layers: " + MAX_LAYERS);
    System.out.println("Input shapes");
    displayShapes(shapes);

    for (int i = 1; i <= MAX_ITERS; ++i) {
      System.out.printf("ITER #%d\n", i);
      // Ops.Stats.clear();
      newShapes = makeShapes1(shapes);
      // System.out.println(Ops.Stats.asString());
      allShapes.addAll(IntStream.of(shapes).boxed().collect(Collectors.toSet()));
      if (newShapes.length == 0) {
        System.out.printf("DONE\n\n");
        break;
      }
      System.out.printf("NEW %d\n\n", newShapes.length);
      shapes = newShapes;

      // System.out.println("Output shapes");
      // displayShapes(newShapes);
    }
    displayResults();
  }

  /**
   * makeShapes
   * 
   * Given a list of starting shapes, find the shapes that can be made by performing all operations.
   */
  int[] makeShapes2(int[] input) {
    List<IntStream> streams = new ArrayList<>();

    System.out.printf("ONE_OPS %d %d > %d\n", ONE_OPS.length, input.length, 1l * ONE_OPS.length * input.length);
    for (IntUnaryOperator op : ONE_OPS) {
      streams.add(IntStream.of(input).map(op));
    }

    /* TODO: Add pre-filters for each op. */
    /* For example, swap only half shapes and stack only one-layer, non-crystal shapes. */
    System.out.printf("TWO_OPS %d %d %d > %d\n", TWO_OPS.length, input.length, allShapes.size(),
        1l * TWO_OPS.length * input.length * (input.length + 2 * (allShapes.size() - input.length)));
    /* TODO: Might be better to keep old shape list rather than recreate it every time. */
    /* Or maybe store an object with a used flag and a build op */
    Set<Integer> inputShapes = IntStream.of(input).boxed().collect(Collectors.toSet());
    for (IntBinaryOperator op : TWO_OPS) {
      streams.add(allShapeStream().mapMulti((s1, consumer) -> {
        for (int s2 : input)
          consumer.accept(op.applyAsInt(s1, s2));
      }));
      streams.add(allShapeStream().filter(s -> !inputShapes.contains(s)).mapMulti((s2, consumer) -> {
        for (int s1 : input)
          consumer.accept(op.applyAsInt(s1, s2));
      }));
    }

    IntStream stream = streams.stream().flatMapToInt(s -> s).filter(this::maxLayers).filter(this::isNew).distinct();
    return stream.parallel().toArray();
  }

  void run2() {
    int[] shapes = IntStream.concat(IntStream.of(Shape.FLAT_4), IntStream.of(Shape.PIN_4)).toArray();
    int[] newShapes;

    System.out.println("Max iters: " + MAX_ITERS);
    System.out.println("Max layers: " + MAX_LAYERS);
    System.out.println("Input shapes");
    displayShapes(shapes);

    newShapes = shapes.clone();
    allShapes.addAll(IntStream.of(newShapes).boxed().collect(Collectors.toSet()));
    for (int i = 1; i <= MAX_ITERS; ++i) {
      System.out.printf("ITER #%d\n", i);
      // Ops.Stats.clear();
      newShapes = makeShapes2(newShapes);
      // System.out.println(Ops.Stats.asString());
      if (newShapes.length == 0) {
        System.out.printf("DONE\n\n");
        break;
      }
      System.out.printf("NEW %d\n\n", newShapes.length);
      allShapes.addAll(IntStream.of(newShapes).boxed().collect(Collectors.toSet()));

      // System.out.println("Output shapes");
      // displayShapes(newShapes);
    }
    displayResults();
    test1();
  }

  void displayResults() {
    int[] lefts = allShapeStream().filter(Shape::isLeftHalf).toArray();
    int[] rights = allShapeStream().filter(Shape::isRightHalf).toArray();
    int[] noCrystal = allShapeStream().filter(v -> !Shape.hasCrystal(v)).sorted().toArray();
    int[] oneLayerNoCrystal = allShapeStream().filter(Shape::isOneLayer).filter(v -> !Shape.hasCrystal(v)).toArray();

    System.out.printf("lefts: %d, rights: %d\n", lefts.length, rights.length);
    System.out.printf("noCrystal: %d\n", noCrystal.length);
    System.out.printf("oneLayerNoCrystal: %d\n", oneLayerNoCrystal.length);
    System.out.printf("Number: %d\n", allShapes.size());
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
    displayShapes(shapes);
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
    displayShapes(shapes);
  }

  void test1() {
    final String filename = "test1.txt";
    System.out.printf("Writing file: %s\n", filename);
    ShapeFile.write(filename, allShapeStream().sorted().toArray());
  }
}
