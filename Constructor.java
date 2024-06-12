import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Finder
 */
public class Constructor {
  private static final int MAX_LAYERS = 1;

  /* TODO: BitSet does not handle negative indexes, so it's not useful for sizes > MAXINT/2 */
  // private BitSet allShapes = new BitSet();

  private Set<Integer> allShapes = new HashSet<>();

  private IntStream allShapeStream() {
    return allShapes.stream().mapToInt(Integer::intValue);
  }

  /* TODO: Add pre-filters for each op. E.g. stack only one-layer, non-crystal shapes and swap only half shapes. */
  private static final IntUnaryOperator[] ONE_OPS = { Ops::rotateRight, Ops::rotate180, Ops::rotateLeft, Ops::cutLeft,
      Ops::cutRight, Ops::pinPush, Ops::crystal };
  private static final IntBinaryOperator[] TWO_OPS = { Ops::swapLeft, Ops::swapRight, Ops::stack };

  private void displayShapes(int[] shapes) {
    for (int shape : shapes) {
      System.out.println(new Shape(shape));
    }
    System.out.println();
  }

  private boolean isNew(int shape) {
    return (shape != 0) && !allShapes.contains(shape);
  }

  private boolean maxLayers(int shape) {
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
    IntStream stream = IntStream.empty().parallel();

    System.out.printf("ONE_OPS %d %d > %d\n", ONE_OPS.length, input.length, ONE_OPS.length * input.length);
    for (IntUnaryOperator op : ONE_OPS)
      stream = IntStream.concat(stream, IntStream.of(input).map(op));

    System.out.printf("TWO_OPS %d %d %d > %d\n", TWO_OPS.length, allShapes.size(), input.length,
        2 * TWO_OPS.length * allShapes.size() * input.length);
    for (IntBinaryOperator op : TWO_OPS) {
      for (int s1 : input) {
        stream = IntStream.concat(stream, allShapeStream().map(s2 -> op.applyAsInt(s1, s2)));
        stream = IntStream.concat(stream, allShapeStream().filter(s2 -> s1 != s2).map(s2 -> op.applyAsInt(s2, s1)));
      }
    }

    stream = stream.filter(this::isNew).filter(this::maxLayers).distinct();
    return stream.toArray();
  }

  void run() {
    final int ITERS = 10;
    int[] shapes = IntStream.concat(IntStream.of(Shape.FLAT_4), IntStream.of(Shape.PIN_4)).toArray();
    int[] newShapes;

    System.out.println("Input shapes");
    displayShapes(shapes);

    newShapes = shapes.clone();
    allShapes.addAll(IntStream.of(newShapes).boxed().collect(Collectors.toList()));
    for (int i = 0; i < ITERS; ++i) {
      System.out.printf("ITER #%d\n", i);
      newShapes = makeShapes(newShapes);
      if (newShapes.length == 0) {
        System.out.printf("DONE\n\n");
        break;
      }
      System.out.printf("NEW %d\n\n", newShapes.length);
      allShapes.addAll(IntStream.of(newShapes).boxed().collect(Collectors.toList()));

      // System.out.println("Output shapes");
      // displayShapes(newShapes);
    }

    System.out.println("All shapes");
    // TODO: print as side-effect, don't make an array
    shapes = allShapeStream().toArray();
    displayShapes(shapes);
    System.out.printf("Number: %d\n", shapes.length);
  }

}
