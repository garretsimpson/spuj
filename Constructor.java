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
    for (int shape : shapes) {
      System.out.println(new Shape(shape));
    }
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
  int[] makeShapes(int[] input) {
    List<IntStream> streams = new ArrayList<>();

    System.out.printf("ONE_OPS %d %d > %d\n", ONE_OPS.length, input.length, 1l * ONE_OPS.length * input.length);
    for (IntUnaryOperator op : ONE_OPS) {
      streams.add(IntStream.of(input).map(op));
    }

    /* TODO: Add pre-filters for each op. */
    /* For example, swap only half shapes and stack only one-layer, non-crystal shapes. */
    System.out.printf("TWO_OPS %d %d %d > %d\n", TWO_OPS.length, input.length, allShapes.size(),
        TWO_OPS.length * input.length * (input.length + 2 * allShapes.size()));
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

  /*
   * TODO: Try adding input shapes to allShapes before calling makeShapes. This will remove the need to filter the input
   * shapes.
   */
  void run() {
    int[] shapes = IntStream.concat(IntStream.of(Shape.FLAT_4), IntStream.of(Shape.PIN_4)).toArray();
    int[] newShapes;

    System.out.println("Max iters: " + MAX_ITERS);
    System.out.println("Max layers: " + MAX_LAYERS);
    System.out.println("Input shapes");
    displayShapes(shapes);

    for (int i = 1; i <= MAX_ITERS; ++i) {
      System.out.printf("ITER #%d\n", i);
      // Ops.Stats.clear();
      newShapes = makeShapes(shapes);
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
    // displayAllShapes();
    System.out.printf("Number: %d\n", allShapes.size());
  }
}
