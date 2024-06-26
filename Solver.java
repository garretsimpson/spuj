import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

/**
 * Solver
 */
public class Solver {
  private static final int MAX_ITERS = 100;
  private static final int MAX_LAYERS = 3;
  private static final int BATCH_SIZE = 100000;

  private static final int PRIM_COST = 1;
  private static boolean exit = false;

  final String RESULTS = "BigData/shapes.db";

  private static final IntUnaryOperator[] ONE_OPS_ALL = { Ops::rotateRight, Ops::rotate180, Ops::rotateLeft,
      Ops::cutLeft, Ops::cutRight, Ops::pinPush, Ops::crystal };
  private static final IntBinaryOperator[] TWO_OPS_ALL = { Ops::swapLeft, Ops::swapRight, Ops::stack };
  private static final Ops.Name[] ONE_OPS = { Ops.Name.ROTATE_RIGHT, Ops.Name.ROTATE_180, Ops.Name.ROTATE_LEFT,
      Ops.Name.CUT_RIGHT, Ops.Name.CUT_LEFT, Ops.Name.PINPUSH, Ops.Name.CRYSTAL };
  private static final Ops.Name[] TWO_OPS = { Ops.Name.SWAP_RIGHT, Ops.Name.SWAP_LEFT, Ops.Name.STACK };

  static class Build {
    byte op;
    int cost;
    int shape1, shape2; // input shapes

    Build(int op, int shape, int cost) {
      this.op = (byte) op;
      this.shape1 = shape;
      this.cost = cost;
    }

    Build(int op, int shape1, int shape2, int cost) {
      this.op = (byte) op;
      this.shape1 = shape1;
      this.shape2 = shape2;
      this.cost = cost;
    }
  }

  static String buildAsString(int shape, Build build) {
    String result;
    String opCode = Ops.nameByValue.get((int) build.op).code;

    if (build.shape2 == 0)
      result = String.format("%3d %08x <- %s(%08x)", build.cost, shape, opCode, build.shape1);
    else
      result = String.format("%3d %08x <- %s(%08x, %08x)", build.cost, shape, opCode, build.shape1, build.shape2);
    return result;
  }

  private Set<Integer> allShapes = new HashSet<>();
  private Set<Integer> newShapes = Collections.synchronizedSet(new LinkedHashSet<>());
  private Set<Integer> lowShapes = Collections.synchronizedSet(new LinkedHashSet<>());

  private Map<Integer, Build> allBuilds = Collections.synchronizedMap(new HashMap<>());
  private static Map<Ops.Name, Integer> opCosts = new HashMap<>();

  static {
    opCosts.put(Ops.Name.NOP, 0);
    opCosts.put(Ops.Name.ROTATE_RIGHT, 1);
    opCosts.put(Ops.Name.ROTATE_180, 1);
    opCosts.put(Ops.Name.ROTATE_LEFT, 1);
    opCosts.put(Ops.Name.CUT_RIGHT, 1);
    opCosts.put(Ops.Name.CUT_LEFT, 1);
    opCosts.put(Ops.Name.PINPUSH, 1);
    opCosts.put(Ops.Name.CRYSTAL, 2);
    opCosts.put(Ops.Name.SWAP_RIGHT, 1);
    opCosts.put(Ops.Name.SWAP_LEFT, 1);
    opCosts.put(Ops.Name.FAST_SWAP, 1);
    opCosts.put(Ops.Name.STACK, 1);
  }

  private IntStream shapeStream(Set<Integer> shapes) {
    return shapes.stream().mapToInt(Integer::intValue);
  }

  private IntStream shapeStream(int[] shapes) {
    return Arrays.stream(shapes);
  }

  Set<Integer> takeValues(Set<Integer> srcSet, int maxValues) {
    Set<Integer> dstSet = new HashSet<>();
    for (int v : srcSet) {
      if (maxValues-- <= 0)
        break;
      dstSet.add(v);
    }
    srcSet.removeAll(dstSet);
    return dstSet;
  }

  private boolean maxLayers(int shape) {
    if (shape == 0)
      return false;
    return (Shape.v1(shape) | Shape.v2(shape)) < (1 << (4 * MAX_LAYERS));
  }

  private boolean oneLayerNoCrystal(int shape) {
    return Shape.isOneLayer(shape) && !Shape.hasCrystal(shape);
  }

  private int doOp(Ops.Name opName, int shape) {
    int result = Ops.invoke(opName, shape);
    if ((result == shape) || !maxLayers(result))
      return 0;
    Build oldBuild = allBuilds.get(result);
    int cost = cost(opName, shape);
    if (oldBuild == null) {
      allBuilds.put(result, new Build(opName.value, shape, cost));
    } else if (cost < oldBuild.cost) {
      // System.out.printf("OLD: %s\n", buildAsString(result, oldBuild));
      oldBuild.op = opName.value;
      oldBuild.shape1 = shape;
      oldBuild.shape2 = 0;
      oldBuild.cost = cost;
      // System.out.printf("NEW: %s\n", buildAsString(result, oldBuild));
      lowShapes.add(result);
    }
    return result;
  }

  private int doOp(Ops.Name opName, int shape1, int shape2) {
    int result = Ops.invoke(opName, shape1, shape2);
    if ((result == shape1) || (result == shape2) || !maxLayers(result))
      return 0;
    Build oldBuild = allBuilds.get(result);
    int cost = cost(opName, shape1, shape2);
    if (oldBuild == null) {
      allBuilds.put(result, new Build(opName.value, shape1, shape2, cost));
    } else if (cost < oldBuild.cost) {
      // System.out.printf("OLD: %s\n", buildAsString(result, oldBuild));
      oldBuild.op = opName.value;
      oldBuild.shape1 = shape1;
      oldBuild.shape2 = shape2;
      oldBuild.cost = cost;
      // System.out.printf("NEW: %s\n", buildAsString(result, oldBuild));
      lowShapes.add(result);
    }
    return result;
  }

  private int cost(Ops.Name opName, int shape) {
    return opCosts.get(opName) + allBuilds.get(shape).cost;
  }

  private int cost(Ops.Name opName, int shape1, int shape2) {
    Build build1 = allBuilds.get(shape1);
    Build build2 = allBuilds.get(shape2);
    int cost = opCosts.get(opName) + build1.cost + build2.cost;
    // if Op is FAST_SWAP, reduce cost for each CUT that was done just prior
    if (opName == Ops.Name.FAST_SWAP) {
      Ops.Name opName1 = Ops.nameByValue.get((int) build1.op);
      Ops.Name opName2 = Ops.nameByValue.get((int) build2.op);
      if ((opName1 == Ops.Name.CUT_RIGHT) || (opName2 == Ops.Name.CUT_RIGHT))
        cost -= opCosts.get(Ops.Name.CUT_RIGHT);
      if ((opName1 == Ops.Name.CUT_LEFT) || (opName2 == Ops.Name.CUT_LEFT))
        cost -= opCosts.get(Ops.Name.CUT_LEFT);
    }
    return cost;
  }

  void run() {
    // int[] shapes = Arrays.stream(Shape.FLAT_4).toArray();
    int[] shapes = Arrays.stream(new int[][] { Shape.FLAT_4, Shape.PIN_4 }).flatMapToInt(Arrays::stream).toArray();

    System.out.println("Max iters: " + MAX_ITERS);
    System.out.println("Max layers: " + MAX_LAYERS);
    System.out.println("Batch size: " + BATCH_SIZE);
    System.out.println("Input shapes");
    Tools.displayShapes(shapes);

    Arrays.stream(shapes).forEach(newShapes::add);
    Arrays.stream(shapes).forEach(shape -> allBuilds.put(shape, new Build(Ops.Name.NOP.value, shape, PRIM_COST)));
    // ShapeFile.delete(RESULTS);

    Set<Integer> inputShapes;
    for (int i = 1; i <= MAX_ITERS; ++i) {
      System.out.printf("ITER #%d\n", i);
      inputShapes = takeValues(newShapes, BATCH_SIZE);
      /* TODO: Insert inputBuilds into allBuilds before calling makeShapes() */
      lowShapes.clear();
      makeShapes(inputShapes);
      allShapes.addAll(inputShapes);
      // ShapeFile.append(RESULTS, inputShapes);

      newShapes.addAll(lowShapes);
      System.out.printf("LOW %d\n", lowShapes.size());
      if (newShapes.size() > 0) {
        System.out.printf("TODO %d\n\n", newShapes.size());
      } else {
        System.out.printf("DONE\n\n");
        break;
      }
    }
    // if (newShapes.size() > 0)
    // ShapeFile.append(RESULTS, newShapes);
  }

  /**
   * makeShapes
   * 
   * Given a list of starting shapes, find the shapes that can be made by performing all operations.
   */
  void makeShapes(Set<Integer> inputShapes) {
    int inputLen = inputShapes.size();
    List<IntStream> streams = new ArrayList<>();
    IntStream stream;

    System.out.printf("ONE_OPS %d %d > %d\n", ONE_OPS.length, inputLen, 1l * ONE_OPS.length * inputLen);
    for (Ops.Name opName : ONE_OPS) {
      streams.add(shapeStream(inputShapes).map(shape -> doOp(opName, shape)));
    }

    System.out.printf("TWO_OPS %d %d %d > %d\n", TWO_OPS.length, inputLen, allShapes.size(),
        1l * TWO_OPS.length * ((1l * inputLen * inputLen) + (2l * inputLen * allShapes.size())));
    makeStreams(streams, inputShapes, Ops.Name.FAST_SWAP, x -> Shape.isLeftHalf(x), x -> Shape.isRightHalf(x));
    makeStreams(streams, inputShapes, Ops.Name.FAST_SWAP, x -> Shape.isRightHalf(x), x -> Shape.isLeftHalf(x));
    // makeStreams(streams, inputShapes, Ops.Name.STACK, x -> !Shape.hasCrystal(x), x -> true);
    makeStreams(streams, inputShapes, Ops.Name.STACK, x -> this.oneLayerNoCrystal(x), x -> true);

    stream = streams.parallelStream().flatMapToInt(s -> s);
    // stream = stream.filter(shape -> this.maxLayers(shape));
    stream = stream.filter(shape -> shape != 0);
    stream = stream.filter(shape -> !allShapes.contains(shape));
    stream = stream.filter(shape -> !inputShapes.contains(shape));
    // stream = stream.filter(shape -> !newShapes.contains(shape));
    stream.forEach(shape -> newShapes.add(shape));
  }

  /* This "completes the square" by doing all operations that have not been done before. */
  void makeStreams(List<IntStream> streams, Set<Integer> inputShapes, Ops.Name opName, IntPredicate pre1,
      IntPredicate pre2) {
    int[] set1 = shapeStream(inputShapes).filter(pre1).toArray();
    int[] set2 = shapeStream(inputShapes).filter(pre2).toArray();
    streams.add(shapeStream(allShapes).filter(pre2).mapMulti((s2, consumer) -> {
      for (int s1 : set1)
        consumer.accept(doOp(opName, s1, s2));
    }));
    streams.add(shapeStream(allShapes).filter(pre1).mapMulti((s1, consumer) -> {
      for (int s2 : set2)
        consumer.accept(doOp(opName, s1, s2));
    }));
    streams.add(shapeStream(set1).mapMulti((s1, consumer) -> {
      for (int s2 : set2)
        consumer.accept(doOp(opName, s1, s2));
    }));
  }

  void saveResults() {
    System.out.println("Solver results");
    System.out.printf("size: %d\n", allBuilds.size());
    ShapeFile.writeDB(RESULTS, allBuilds);
    int maxCost = allBuilds.values().stream().mapToInt(v -> v.cost).max().getAsInt();
    System.out.printf("max cost: %d (%x)", maxCost, maxCost);

  }

  void shutdown() {
    exit = true;
    if (newShapes.size() > 0)
      ShapeFile.append(RESULTS, newShapes);
  }

}
