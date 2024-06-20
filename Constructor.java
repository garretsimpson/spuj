import java.util.ArrayList;
import java.util.Arrays;
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
  private static final int MAX_LAYERS = 3;

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
    return (Shape.v1(shape) | Shape.v2(shape)) < (1 << (4 * MAX_LAYERS));
  }

  private boolean oneLayerNoCrystal(int shape) {
    return Shape.isOneLayer(shape) && !Shape.hasCrystal(shape);
  }

  /**
   * makeShapes
   * 
   * Given a list of starting shapes, find the shapes that can be made by performing all operations.
   */
  int[] makeShapes1(int[] input) {
    List<IntStream> streams = new ArrayList<>();
    int inputLen = input.length;

    System.out.printf("ONE_OPS %d %d > %d\n", ONE_OPS.length, inputLen, 1l * ONE_OPS.length * inputLen);
    for (IntUnaryOperator op : ONE_OPS) {
      streams.add(IntStream.of(input).map(op));
    }

    System.out.printf("TWO_OPS %d %d %d > %d\n", TWO_OPS.length, inputLen, allShapes.size(),
        1l * TWO_OPS.length * inputLen * (inputLen + 2 * (allShapes.size() - inputLen)));
    /* TODO: keep track of old shapes, or use flag on all shapes */
    Set<Integer> newShapes = IntStream.of(input).boxed().collect(Collectors.toSet());
    int[] rights = IntStream.of(input).filter(Shape::isRightHalf).toArray();
    streams.add(allShapeStream().filter(Shape::isLeftHalf).mapMulti((s1, consumer) -> {
      for (int s2 : rights)
        consumer.accept(Ops.fastSwapRight(s1, s2));
    }));
    int[] lefts = IntStream.of(input).filter(Shape::isLeftHalf).toArray();
    streams.add(oldShapeStream(newShapes).filter(Shape::isRightHalf).mapMulti((s2, consumer) -> {
      for (int s1 : lefts)
        consumer.accept(Ops.fastSwapRight(s1, s2));
    }));
    streams.add(allShapeStream().filter(this::oneLayerNoCrystal).mapMulti((s1, consumer) -> {
      for (int s2 : input)
        consumer.accept(Ops.stack(s1, s2));
    }));
    int[] tops = IntStream.of(input).filter(this::oneLayerNoCrystal).toArray();
    streams.add(oldShapeStream(newShapes).mapMulti((s2, consumer) -> {
      for (int s1 : tops)
        consumer.accept(Ops.stack(s1, s2));
    }));

    // IntStream stream = streams.stream().flatMapToInt(s -> s).filter(this::isNew).distinct();
    IntStream stream = streams.stream().flatMapToInt(s -> s).filter(this::maxLayers).filter(this::isNew).distinct();
    return stream.parallel().toArray();
  }

  void run1() {
    // int[] shapes = IntStream.of(Shape.FLAT_4).toArray();
    int[] shapes = Arrays.asList(Shape.FLAT_4, Shape.PIN_4).stream().flatMapToInt(v -> IntStream.of(v)).toArray();
    int[] newShapes;

    System.out.println("Max iters: " + MAX_ITERS);
    System.out.println("Max layers: " + MAX_LAYERS);
    System.out.println("Input shapes");
    Tools.displayShapes(shapes);

    newShapes = shapes;
    allShapes.addAll(IntStream.of(newShapes).boxed().collect(Collectors.toSet()));
    for (int i = 1; i <= MAX_ITERS; ++i) {
      System.out.printf("ITER #%d\n", i);
      // Ops.Stats.clear();
      newShapes = makeShapes1(newShapes);
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

  /**
   * makeShapes
   * 
   * Given a list of starting shapes, find the shapes that can be made by performing all operations.
   */
  Set<Integer> makeShapes2(Set<Integer> input) {
    List<IntStream> streams = new ArrayList<>();
    // Set<Integer> result = Collections.synchronizedSet(new HashSet<>());
    int inputLen = input.size();

    System.out.printf("ONE_OPS %d %d > %d\n", ONE_OPS.length, inputLen, 1l * ONE_OPS.length * inputLen);
    for (IntUnaryOperator op : ONE_OPS) {
      streams.add(input.stream().mapToInt(Integer::intValue).map(op));
    }

    System.out.printf("TWO_OPS %d %d %d > %d\n", TWO_OPS.length, inputLen, allShapes.size(),
        1l * TWO_OPS.length * inputLen * (inputLen + 2 * (allShapes.size() - inputLen)));
    /* TODO: keep track of old shapes, or use flag on all shapes */
    Set<Integer> newShapes = input;
    int[] rights = input.stream().mapToInt(Integer::intValue).filter(Shape::isRightHalf).toArray();
    streams.add(allShapeStream().filter(Shape::isLeftHalf).mapMulti((s1, consumer) -> {
      for (int s2 : rights)
        consumer.accept(Ops.fastSwapRight(s1, s2));
    }));
    int[] lefts = input.stream().mapToInt(Integer::intValue).filter(Shape::isLeftHalf).toArray();
    streams.add(oldShapeStream(newShapes).filter(Shape::isRightHalf).mapMulti((s2, consumer) -> {
      for (int s1 : lefts)
        consumer.accept(Ops.fastSwapRight(s1, s2));
    }));
    streams.add(allShapeStream().filter(this::oneLayerNoCrystal).mapMulti((s1, consumer) -> {
      for (int s2 : input)
        consumer.accept(Ops.stack(s1, s2));
    }));
    int[] tops = input.stream().mapToInt(Integer::intValue).filter(this::oneLayerNoCrystal).toArray();
    streams.add(oldShapeStream(newShapes).mapMulti((s2, consumer) -> {
      for (int s1 : tops)
        consumer.accept(Ops.stack(s1, s2));
    }));

    return streams.stream().flatMapToInt(s -> s).filter(this::maxLayers).filter(this::isNew).boxed().parallel()
        .collect(Collectors.toSet());
  }

  /*
   * This uses a Set<Integer> instead of int[] for the list of new shapes. I was thinking that using a Set would
   * eliminate conversions to/from int[] and distict() filter. But it is slower (about 40% more time), and probably uses
   * more memory.
   */
  void run2() {
    // int[] shapes = IntStream.of(Shape.FLAT_4).toArray();
    int[] shapes = Arrays.asList(Shape.FLAT_4, Shape.PIN_4).stream().flatMapToInt(v -> IntStream.of(v)).toArray();
    Set<Integer> newShapes;

    System.out.println("Max iters: " + MAX_ITERS);
    System.out.println("Max layers: " + MAX_LAYERS);
    System.out.println("Input shapes");
    Tools.displayShapes(shapes);

    newShapes = IntStream.of(shapes).boxed().collect(Collectors.toSet());
    allShapes.addAll(newShapes);
    for (int i = 1; i <= MAX_ITERS; ++i) {
      System.out.printf("ITER #%d\n", i);
      // Ops.Stats.clear();
      newShapes = makeShapes2(newShapes);
      // System.out.println(Ops.Stats.asString());
      if (newShapes.size() == 0) {
        System.out.printf("DONE\n\n");
        break;
      }
      System.out.printf("NEW %d\n\n", newShapes.size());
      allShapes.addAll(newShapes);

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
