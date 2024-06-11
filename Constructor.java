import java.util.BitSet;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

/**
 * Finder
 */
public class Constructor {
  private static final int MAX_LAYERS = 1;

  /* TODO: BitSet does not handle negative indexes, so it's not useful for sizes > MAXINT/2 */
  private BitSet allShapes = new BitSet();

  /* TODO: Add pre-filters for each op. E.g. stack only one-layer, non-crystal shapes and swap only half shapes. */
  private static final IntUnaryOperator[] ONE_OPS = { Ops::rotateRight, Ops::rotate180, Ops::rotateLeft, Ops::cutLeft,
      Ops::cutRight, Ops::pinPush, Ops::crystal };
  private static final IntBinaryOperator[] TWO_OPS = { Ops::swapLeft, Ops::swapRight, Ops::stack };

  /* TODO: This should probably be an int generator rather than allocate an entire array */
  private int[] getAllShapes() {
    int[] shapes = new int[allShapes.cardinality()];
    int shape = 0;
    int i = 0;
    while (true) {
      shape = allShapes.nextSetBit(shape);
      if (shape == -1)
        break;
      shapes[i++] = shape++;
    }
    return shapes;
  }

  private void displayShapes(int[] shapes) {
    for (int shape : shapes) {
      System.out.println(new Shape(shape));
    }
    System.out.println();
  }

  private boolean isNew(int shape) {
    return (shape != 0) && !allShapes.get(shape);
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

    int[] shapes = getAllShapes();
    System.out.printf("TWO_OPS %d %d %d > %d\n", TWO_OPS.length, shapes.length, input.length,
        2 * TWO_OPS.length * shapes.length * input.length);
    for (int s1 : shapes) {
      for (IntBinaryOperator op : TWO_OPS) {
        stream = IntStream.concat(stream, IntStream.of(input).map(s2 -> op.applyAsInt(s1, s2)));
        stream = IntStream.concat(stream, IntStream.of(input).filter(s2 -> s1 != s2).map(s2 -> op.applyAsInt(s2, s1)));
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
    for (int shape : newShapes)
      allShapes.set(shape);
    for (int i = 0; i < ITERS; ++i) {
      System.out.printf("ITER #%d\n", i);
      newShapes = makeShapes(newShapes);
      if (newShapes.length == 0) {
        System.out.printf("DONE\n\n");
        break;
      }
      System.out.printf("NEW %d\n\n", newShapes.length);
      for (int shape : newShapes)
        allShapes.set(shape);

      // System.out.println("Output shapes");
      // displayShapes(newShapes);
    }

    shapes = getAllShapes();
    System.out.println("All shapes");
    displayShapes(shapes);
    System.out.printf("Number: %d\n", shapes.length);
  }

}
