import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Solver
 */
public class Solver {
  private static final int MAX_ITERS = 1000;
  private static final int MAX_LAYERS = 2;
  private static final int BATCH_SIZE = 100000;

  final String RESULTS = "BigData/shapes.db";

  private static final IntUnaryOperator[] ONE_OPS_ALL = { Ops::rotateRight, Ops::rotate180, Ops::rotateLeft,
      Ops::cutLeft, Ops::cutRight, Ops::pinPush, Ops::crystal };
  private static final IntBinaryOperator[] TWO_OPS_ALL = { Ops::swapLeft, Ops::swapRight, Ops::stack };
  private static final IntUnaryOperator[] ONE_OPS = { Ops::rotateRight, Ops::cutRight, Ops::pinPush, Ops::crystal };
  private static final IntBinaryOperator[] TWO_OPS = { Ops::fastSwapRight, Ops::fastStack };

  private Set<Integer> allShapes = new HashSet<>();
  private Set<Integer> newShapes = Collections.synchronizedSet(new HashSet<>());

  private IntStream allShapeStream() {
    return allShapes.stream().mapToInt(Integer::intValue);
  }

  private IntStream oldShapeStream(Set<Integer> newShapes) {
    return allShapeStream().filter(s -> !newShapes.contains(s));
  }

  Set<Integer> takeValues(Set<Integer> srcSet, int maxValues) {
    Set<Integer> dstSet = new HashSet<>();
    for (int v : srcSet) {
      if (maxValues-- == 0)
        break;
      dstSet.add(v);
    }
    srcSet.removeAll(dstSet);
    return dstSet;
  }

  private boolean isNew(int shape) {
    return !allShapes.contains(shape);
  }

  private boolean maxLayers(int shape) {
    if (shape == 0)
      return false;
    return (Shape.v1(shape) | Shape.v2(shape)) < (1 << (4 * MAX_LAYERS));
  }

  private boolean oneLayerNoCrystal(int shape) {
    return Shape.isOneLayer(shape) && !Shape.hasCrystal(shape);
  }

  void run() {
    // int[] shapes = IntStream.of(Shape.FLAT_4).toArray();
    int[] shapes = Arrays.asList(Shape.FLAT_4, Shape.PIN_4).stream().flatMapToInt(v -> IntStream.of(v)).toArray();
    // int[] shapes = Arrays.stream(new int[][] { Shape.FLAT_4, Shape.PIN_4 }).flatMapToInt(Arrays::stream).toArray();

    System.out.println("Max iters: " + MAX_ITERS);
    System.out.println("Max layers: " + MAX_LAYERS);
    System.out.println("Batch size: " + BATCH_SIZE);
    System.out.println("Input shapes");
    Tools.displayShapes(shapes);

    Set<Integer> inputShapes = IntStream.of(shapes).boxed().collect(Collectors.toSet());
    newShapes.addAll(inputShapes);

    ShapeFile.delete(RESULTS);
    ShapeFile.append(RESULTS, newShapes);

    for (int i = 1; i <= MAX_ITERS; ++i) {
      System.out.printf("ITER #%d\n", i);
      inputShapes = takeValues(newShapes, BATCH_SIZE);
      /* TODO: add inputShapes to allShapes before calling makeShapes */
      makeShapes(inputShapes);
      allShapes.addAll(inputShapes);
      ShapeFile.append(RESULTS, newShapes);

      if (newShapes.size() > 0) {
        System.out.printf("TODO %d\n\n", newShapes.size());
      } else {
        System.out.printf("DONE\n\n");
        break;
      }

      // System.out.println("Output shapes");
      // displayShapes(newShapes);
    }
  }

  /**
   * makeShapes
   * 
   * Given a list of starting shapes, find the shapes that can be made by performing all operations.
   */
  void makeShapes(Set<Integer> inputShapes) {
    List<IntStream> streams = new ArrayList<>();
    int[] input = inputShapes.stream().mapToInt(Integer::intValue).toArray();
    int inputLen = input.length;

    System.out.printf("ONE_OPS %d %d > %d\n", ONE_OPS.length, inputLen, 1l * ONE_OPS.length * inputLen);
    for (IntUnaryOperator op : ONE_OPS) {
      streams.add(IntStream.of(input).map(op));
    }

    System.out.printf("TWO_OPS %d %d %d > %d\n", TWO_OPS.length, inputLen, allShapes.size(),
        1l * TWO_OPS.length * ((1l * inputLen * inputLen) + (2l * inputLen * allShapes.size())));

    // swap
    int[] lefts = IntStream.of(input).filter(Shape::isLeftHalf).toArray();
    streams.add(allShapeStream().filter(Shape::isRightHalf).mapMulti((s2, consumer) -> {
      for (int s1 : lefts)
        consumer.accept(Ops.fastSwapRight(s1, s2));
    }));
    int[] rights = IntStream.of(input).filter(Shape::isRightHalf).toArray();
    streams.add(allShapeStream().filter(Shape::isLeftHalf).mapMulti((s1, consumer) -> {
      for (int s2 : rights)
        consumer.accept(Ops.fastSwapRight(s1, s2));
    }));
    streams.add(IntStream.of(lefts).mapMulti((s1, consumer) -> {
      for (int s2 : rights)
        consumer.accept(Ops.fastSwapRight(s1, s2));
    }));

    // stack
    int[] tops = IntStream.of(input).filter(this::oneLayerNoCrystal).toArray();
    streams.add(allShapeStream().mapMulti((s2, consumer) -> {
      for (int s1 : tops)
        consumer.accept(Ops.stack(s1, s2));
    }));
    streams.add(allShapeStream().filter(this::oneLayerNoCrystal).mapMulti((s1, consumer) -> {
      for (int s2 : input)
        consumer.accept(Ops.stack(s1, s2));
    }));
    streams.add(IntStream.of(tops).mapMulti((s1, consumer) -> {
      for (int s2 : input)
        consumer.accept(Ops.stack(s1, s2));
    }));

    IntStream stream = streams.parallelStream().flatMapToInt(s -> s);
    stream = stream.filter(this::maxLayers);
    stream = stream.filter(s -> !allShapes.contains(s));
    stream = stream.filter(s -> !inputShapes.contains(s));
    stream.forEach(s -> newShapes.add(s));
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
    ShapeFile.write(RESULTS, allShapes);
  }

}
