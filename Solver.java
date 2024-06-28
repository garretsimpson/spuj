import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

/**
 * Solver
 */
public class Solver {
  private static final int MAX_ITERS = 1000;
  private static final int MAX_COST = 1000;
  private static final int MAX_LAYERS = 4;
  private static final int BATCH_SIZE = 100000;

  private static final int PRIM_COST = 1;
  private static boolean exit = false;

  final String RESULTS = "BigData/shapes.db";

  private static final Ops.Name[] ONE_OPS = { Ops.Name.ROTATE_RIGHT, Ops.Name.ROTATE_180, Ops.Name.ROTATE_LEFT,
      Ops.Name.CUT_RIGHT, Ops.Name.CUT_LEFT, Ops.Name.PINPUSH, Ops.Name.CRYSTAL };
  private static final Ops.Name[] TWO_OPS = { Ops.Name.SWAP_RIGHT, Ops.Name.SWAP_LEFT, Ops.Name.STACK };

  static class Build {
    byte op;
    int cost;
    int shape; // result shape
    int shape1, shape2; // input shapes

    Build(int op, int cost, int... shapes) {
      this.op = (byte) op;
      this.cost = cost;
      this.shape = shapes[0];
      if (shapes.length > 1)
        this.shape1 = shapes[1];
      if (shapes.length > 2)
        this.shape2 = shapes[2];
    }
  }

  static String buildAsString(Build build) {
    String result;
    String opCode = Ops.nameByValue.get((int) build.op).code;
    if (build.shape2 == 0)
      result = String.format("%3d %08x <- %s(%08x)", build.cost, build.shape, opCode, build.shape1);
    else
      result = String.format("%3d %08x <- %s(%08x, %08x)", build.cost, build.shape, opCode, build.shape1, build.shape2);
    return result;
  }

  private Set<Integer> allShapes = new HashSet<>();
  private List<Set<Integer>> newShapes = new ArrayList<>(MAX_COST);

  private Map<Integer, Build> allBuilds = Collections.synchronizedMap(new HashMap<>());
  private List<Build> oldBuilds = Collections.synchronizedList(new ArrayList<>());

  private static Map<Ops.Name, Integer> opCosts = new HashMap<>();

  static {
    opCosts.put(Ops.Name.NOP, 0);
    opCosts.put(Ops.Name.ROTATE_RIGHT, 1);
    opCosts.put(Ops.Name.ROTATE_180, 1);
    opCosts.put(Ops.Name.ROTATE_LEFT, 1);
    opCosts.put(Ops.Name.CUT_RIGHT, 1);
    opCosts.put(Ops.Name.CUT_LEFT, 1);
    opCosts.put(Ops.Name.PINPUSH, 1);
    opCosts.put(Ops.Name.CRYSTAL, 2); // 2 because crystal uses paint
    opCosts.put(Ops.Name.SWAP_RIGHT, 1);
    opCosts.put(Ops.Name.SWAP_LEFT, 1);
    opCosts.put(Ops.Name.FAST_SWAP, 1);
    opCosts.put(Ops.Name.STACK, 3); // 3 to compensate for cut before swap
  }

  Solver() {
    for (int i = 0; i < MAX_COST; ++i) {
      newShapes.add(Collections.synchronizedSet(new HashSet<Integer>()));
    }
  }

  private IntStream shapeStream(Set<Integer> shapes) {
    return shapes.stream().mapToInt(Integer::intValue);
  }

  private IntStream shapeStream(int[] shapes) {
    return Arrays.stream(shapes);
  }

  void takeValues(Set<Integer> dstSet, Set<Integer> srcSet, int maxValues) {
    int numValues = maxValues - dstSet.size();
    if (numValues <= 0)
      return;
    if (srcSet.size() <= numValues) {
      dstSet.addAll(srcSet);
      srcSet.clear();
      return;
    }
    for (Integer v : srcSet) {
      if (numValues-- == 0)
        break;
      dstSet.add(v);
    }
    srcSet.removeAll(dstSet);
  }

  private boolean maxLayers(int shape) {
    if (shape == 0)
      return false;
    return (Shape.v1(shape) | Shape.v2(shape)) < (1 << (4 * MAX_LAYERS));
  }

  private boolean oneLayerNoCrystal(int shape) {
    return Shape.isOneLayer(shape) && !Shape.hasCrystal(shape);
  }

  private void debugBuild(String name, Build build) {
    if (build == null)
      return;
    // if (build.shape == 0x00cc00c1)
    System.out.printf("%s: %s\n", name, buildAsString(build));
  }

  private int doOp(Ops.Name opName, int shape) {
    int result = Ops.invoke(opName, shape);
    if ((result == shape) || !maxLayers(result))
      return 0;
    synchronized (allBuilds) {
      Build oldBuild = allBuilds.get(result), newBuild = null;
      int cost = cost(opName, shape);
      if (oldBuild == null) {
        newBuild = new Build(opName.value, cost, result, shape);
        allBuilds.put(result, newBuild);
      } else if (cost < oldBuild.cost) {
        oldBuilds.add(oldBuild);
        newBuild = new Build(opName.value, cost, result, shape);
        allBuilds.put(result, newBuild);
      }
      // debugBuild("OLD", oldBuild);
      // debugBuild("NEW", newBuild);
    }
    return result;
  }

  private int doOp(Ops.Name opName, int shape1, int shape2) {
    int result = Ops.invoke(opName, shape1, shape2);
    // debugBuild("INP", new Build(opName.value, 0, result, shape1, shape2));
    if ((result == shape1) || (result == shape2) || !maxLayers(result))
      return 0;
    synchronized (allBuilds) {
      Build oldBuild = allBuilds.get(result), newBuild = null;
      int cost = cost(opName, shape1, shape2);
      if (oldBuild == null) {
        newBuild = new Build(opName.value, cost, result, shape1, shape2);
        allBuilds.put(result, newBuild);
      } else if (cost < oldBuild.cost) {
        oldBuilds.add(oldBuild);
        newBuild = new Build(opName.value, cost, result, shape1, shape2);
        allBuilds.put(result, newBuild);
      }
      // debugBuild("OLD", oldBuild);
      // debugBuild("NEW", newBuild);
    }
    return result;
  }

  private int cost(Ops.Name opName, int shape) {
    return opCosts.get(opName) + allBuilds.get(shape).cost;
  }

  private int cost(Ops.Name opName, int shape1, int shape2) {
    return opCosts.get(opName) + allBuilds.get(shape1).cost + allBuilds.get(shape2).cost;
  }

  private int cost2(Ops.Name opName, int shape1, int shape2) {
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

  private String todoString() {
    StringBuilder sb = new StringBuilder();
    int size;
    for (int i = 0; i < newShapes.size(); ++i) {
      size = newShapes.get(i).size();
      if (size > 0)
        sb.append(String.format("%4d: %,8d\n", i, size));
    }
    return sb.toString();
  }

  void run() {
    // int[] shapes = Arrays.stream(Shape.FLAT_4).toArray();
    int[] shapes = Arrays.stream(new int[][] { Shape.FLAT_4, Shape.PIN_4 }).flatMapToInt(Arrays::stream).toArray();

    System.out.println("Max iters: " + MAX_ITERS);
    System.out.println("Max layers: " + MAX_LAYERS);
    System.out.println("Batch size: " + BATCH_SIZE);
    System.out.println("Input shapes");
    Tools.displayShapes(shapes);

    Arrays.stream(shapes).forEach(shape -> newShapes.get(PRIM_COST).add(shape));
    Arrays.stream(shapes).forEach(shape -> allBuilds.put(shape, new Build(Ops.Name.NOP.value, PRIM_COST, shape)));

    Set<Integer> newShapes, inputShapes = new HashSet<>();
    for (int cost = PRIM_COST; cost < MAX_COST; ++cost) {
      if (cost > MAX_ITERS)
        break;
      newShapes = this.newShapes.get(cost);
      if (newShapes.size() > 0) {
        System.out.println("TODO");
        System.out.println(todoString());
        System.out.printf("COST    %,20d\n", cost);
      }
      while (newShapes.size() > 0) {
        takeValues(inputShapes, newShapes, BATCH_SIZE);
        System.out.printf("SIZE    %,20d\n", inputShapes.size());
        System.out.printf("TOTAL   %,20d\n", allShapes.size());
        makeShapes(inputShapes);
        allShapes.addAll(inputShapes);
        inputShapes.clear();
        System.out.println();
      }
    }
    System.out.printf("DONE\n\n");
  }

  /**
   * makeShapes
   * 
   * Given a list of starting shapes, find the shapes that can be made by performing all operations.
   */
  void makeShapes(Set<Integer> inputShapes) {
    Set<Integer> shapes = Collections.synchronizedSet(new HashSet<Integer>());
    List<IntStream> streams = new ArrayList<>();
    int inputLen = inputShapes.size();

    oldBuilds.clear();

    System.out.printf("ONE_OPS %,20d\n", 1l * ONE_OPS.length * inputLen);
    for (Ops.Name opName : ONE_OPS) {
      streams.add(shapeStream(inputShapes).map(shape -> doOp(opName, shape)));
    }

    System.out.printf("TWO_OPS %,20d\n",
        1l * TWO_OPS.length * ((1l * inputLen * inputLen) + (2l * inputLen * allShapes.size())));
    makeStreams(streams, inputShapes, Ops.Name.FAST_SWAP, x -> Shape.isLeftHalf(x), x -> Shape.isRightHalf(x));
    makeStreams(streams, inputShapes, Ops.Name.FAST_SWAP, x -> Shape.isRightHalf(x), x -> Shape.isLeftHalf(x));
    // makeStreams(streams, inputShapes, Ops.Name.STACK, x -> !Shape.hasCrystal(x), x -> true);
    makeStreams(streams, inputShapes, Ops.Name.STACK, x -> this.oneLayerNoCrystal(x), x -> true);

    IntStream stream;
    stream = streams.parallelStream().flatMapToInt(s -> s);
    stream = stream.filter(shape -> shape != 0);
    stream = stream.filter(shape -> !allShapes.contains(shape));
    stream = stream.filter(shape -> !inputShapes.contains(shape));
    stream.forEach(shapes::add);

    // Remove old duplicate shapes
    oldBuilds.stream().forEach(build -> newShapes.get(build.cost).remove(build.shape));

    // Insert new new shapes
    shapes.stream().forEach(shape -> newShapes.get(allBuilds.get(shape).cost).add(shape));

    System.out.printf("BUILDS  %,20d\n", allBuilds.size());
    System.out.printf("DUPS    %,20d\n", oldBuilds.size());
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
    int totalCost = allBuilds.values().stream().mapToInt(v -> v.cost).sum();
    System.out.printf("max cost: %d (%x)\n", maxCost, maxCost);
    System.out.printf("total cost: %d\n", totalCost);
  }

  void shutdown() {
    exit = true;
    ShapeFile.writeDB(RESULTS, allBuilds);
  }

}
