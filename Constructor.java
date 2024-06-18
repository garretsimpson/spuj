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
  }

  private IntStream oldShapeStream(Set<Integer> newShapes) {
    return allShapeStream().filter(s -> !newShapes.contains(s));
  }

  private static final IntUnaryOperator[] ONE_OPS_ALL = { Ops::rotateRight, Ops::rotate180, Ops::rotateLeft,
      Ops::cutLeft, Ops::cutRight, Ops::pinPush, Ops::crystal };
  private static final IntBinaryOperator[] TWO_OPS_ALL = { Ops::swapLeft, Ops::swapRight, Ops::stack };
  private static final IntUnaryOperator[] ONE_OPS = { Ops::rotateRight, Ops::cutRight, Ops::pinPush, Ops::crystal };
  private static final IntBinaryOperator[] TWO_OPS = { Ops::fastSwapRight, Ops::fastStack };

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
    Tools.displayShapes(shapes);

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

  private boolean oneLayerNoCrystal(int shape) {
    return Shape.isOneLayer(shape) && !Shape.hasCrystal(shape);
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

    System.out.printf("TWO_OPS %d %d %d > %d\n", TWO_OPS.length, input.length, allShapes.size(),
        1l * TWO_OPS.length * input.length * (input.length + 2 * (allShapes.size() - input.length)));
    /* TODO: keep track of old shapes, or use flag on all shapes */
    Set<Integer> newShapes = IntStream.of(input).boxed().collect(Collectors.toSet());
    /* TODO: use the same variable for all of these, might be able to free it sooner */
    int[] rights = IntStream.of(input).filter(Shape::isRightHalf).toArray();
    int[] lefts = IntStream.of(input).filter(Shape::isLeftHalf).toArray();
    int[] tops = IntStream.of(input).filter(this::oneLayerNoCrystal).toArray();
    streams.add(allShapeStream().filter(Shape::isLeftHalf).mapMulti((s1, consumer) -> {
      for (int s2 : rights)
        consumer.accept(Ops.fastSwapRight(s1, s2));
    }));
    streams.add(oldShapeStream(newShapes).filter(Shape::isRightHalf).mapMulti((s2, consumer) -> {
      for (int s1 : lefts)
        consumer.accept(Ops.fastSwapRight(s1, s2));
    }));
    streams.add(allShapeStream().filter(this::oneLayerNoCrystal).mapMulti((s1, consumer) -> {
      for (int s2 : input)
        consumer.accept(Ops.stack(s1, s2));
    }));
    streams.add(oldShapeStream(newShapes).mapMulti((s2, consumer) -> {
      for (int s1 : tops)
        consumer.accept(Ops.stack(s1, s2));
    }));

    IntStream stream = streams.stream().flatMapToInt(s -> s).filter(this::maxLayers).filter(this::isNew).distinct();
    return stream.parallel().toArray();
  }

  void run2() {
    int[] shapes = IntStream.concat(IntStream.of(Shape.FLAT_4), IntStream.of(Shape.PIN_4)).toArray();
    int[] newShapes;

    System.out.println("Max iters: " + MAX_ITERS);
    System.out.println("Max layers: " + MAX_LAYERS);
    System.out.println("Input shapes");
    Tools.displayShapes(shapes);

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

  void saveResults() {
    final String RESULTS = "shapes.txt";
    ShapeFile.write(RESULTS, allShapes);
  }

}
