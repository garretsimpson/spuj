import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ShapeFile
 */
public class ShapeFile {

  static void write(String name, int[] data) {
    Set<Integer> dataSet = IntStream.of(data).boxed().collect(Collectors.toSet());
    write(name, dataSet, false);
  }

  static void write(String name, Set<Integer> data) {
    write(name, data, false);
  }

  static void append(String name, int[] data) {
    Set<Integer> dataSet = IntStream.of(data).boxed().collect(Collectors.toSet());
    write(name, dataSet, true);
  }

  static void append(String name, Set<Integer> data) {
    write(name, data, true);
  }

  static void writeDB(String name, Map<Integer, Solver.Build> data) {
    writeDB(name, data, false);
  }

  static void appendDB(String name, Map<Integer, Solver.Build> data) {
    writeDB(name, data, true);
  }

  static void delete(String name) {
    try {
      Files.delete(Path.of(name));
    } catch (Exception e) {
      System.err.printf("Error deleting file: %s\n", name);
      e.printStackTrace();
    }
  }

  /* TODO: Need a faster file reader */
  static Set<Integer> read(String name) {
    System.out.printf("Reading file: %s\n", name);
    Set<Integer> dataSet = new HashSet<>();
    String value;
    int shape;
    try (Scanner scan = new Scanner(new FileReader(name))) {
      scan.useRadix(16);
      scan.useDelimiter("[\\s+,]");
      while (scan.hasNext()) {
        value = scan.next();
        // System.out.println(value);
        if (value.startsWith("value")) {
          shape = scan.nextInt();
          // System.out.printf("SHAPE: %08x\n", shape);
          dataSet.add(shape);
          scan.nextLine();
        }
      }
    } catch (Exception e) {
      System.err.printf("Error reading file: %s\n", name);
      e.printStackTrace();
    }
    System.out.printf("Number of values read: %,d\n", dataSet.size());
    return dataSet;
  }

  static void write(String name, Set<Integer> data, boolean append) {
    final int BIG_DATA_SIZE = 10000000;
    System.out.printf("Writing file: %s\n", name);
    try (FileWriter file = new FileWriter(name, append)) {
      PrintWriter out = new PrintWriter(file);
      if (data.size() < BIG_DATA_SIZE)
        writeFast(out, data);
      else
        writeSlow(out, data);
    } catch (Exception e) {
      System.err.printf("Error writing file: %s\n", name);
      e.printStackTrace();
    }
  }

  private static void writeFast(PrintWriter out, Set<Integer> data) {
    Integer[] shapes = data.toArray(Integer[]::new);
    Arrays.parallelSort(shapes, (x, y) -> Integer.compareUnsigned(x, y));
    for (Integer shape : shapes)
      out.printf("%08x\n", shape);
  }

  private static void writeSlow(PrintWriter out, Set<Integer> data) {
    int shape;
    for (long i = 0; i <= 0xffffffffl; ++i) {
      shape = (int) i;
      if (data.contains(shape))
        out.printf("%08x\n", shape);
    }
  }

  static void writeSlow(String name, Set<Integer> data, boolean append) {
    System.out.printf("Writing file: %s\n", name);
    try (FileWriter file = new FileWriter(name, append)) {
      int value;
      PrintWriter out = new PrintWriter(file);
      for (long i = 0; i <= 0xffffffffl; ++i) {
        value = (int) i;
        if (data.contains(value))
          out.printf("%08x\n", value);
        // out.println(new Shape(value));
      }
    } catch (Exception e) {
      System.err.printf("Error writing file: %s\n", name);
      e.printStackTrace();
    }
    System.out.printf("Number of values written: %,d\n", data.size());
  }

  static void sort(String name) {
    System.out.printf("Sorting file: %s\n", name);
    Map<Integer, Solver.Build> dataMap = readDB(name);
    writeDB(name, dataMap, false);
  }

  /* TODO: Need a faster file reader */
  static Map<Integer, Solver.Build> readDB(String name) {
    final int BASE = 16;
    System.out.printf("Reading file: %s\n", name);
    Map<Integer, Solver.Build> dataMap = new HashMap<>();
    Integer shape, shape1, shape2, cost;
    String opCode;
    try (Scanner scan = new Scanner(new FileReader(name))) {
      scan.useRadix(BASE);
      scan.useDelimiter("[\\s,]");
      while (scan.hasNext()) {
        shape = Integer.parseUnsignedInt(scan.next(), BASE);
        opCode = scan.next();
        shape1 = Integer.parseUnsignedInt(scan.next(), BASE);
        shape2 = Integer.parseUnsignedInt(scan.next(), BASE);
        cost = scan.nextInt();
        dataMap.put(shape, new Solver.Build(cost, Ops.nameByCode.get(opCode), shape, shape1, shape2));
      }
    } catch (Exception e) {
      System.err.printf("Error reading file: %s\n", name);
      e.printStackTrace();
    }
    System.out.printf("Number of values read: %,d\n\n", dataMap.size());
    return dataMap;
  }

  private static String buildAsString(Solver.Build build) {
    return String.format("%08x,%s,%08x,%08x,%02x\n", build.shape, build.opName.code, build.shape1, build.shape2,
        build.cost);
  }

  static void writeDB(String name, Map<Integer, Solver.Build> data, boolean append) {
    final int BIG_DATA_SIZE = 10000000;
    System.out.printf("Writing file: %s\n", name);
    try (FileWriter file = new FileWriter(name, append)) {
      PrintWriter out = new PrintWriter(file);
      if (data.size() < BIG_DATA_SIZE)
        writeDBFast(out, data);
      else
        writeDBSlow(out, data);
    } catch (Exception e) {
      System.err.printf("Error writing file: %s\n", name);
      e.printStackTrace();
    }
  }

  private static void writeDBFast(PrintWriter out, Map<Integer, Solver.Build> data) {
    Integer[] shapes = data.keySet().toArray(Integer[]::new);
    Arrays.parallelSort(shapes, (x, y) -> Integer.compareUnsigned(x, y));
    for (Integer shape : shapes)
      out.printf(buildAsString(data.get(shape)));
  }

  private static void writeDBSlow(PrintWriter out, Map<Integer, Solver.Build> data) {
    Solver.Build build;
    for (long i = 0; i <= 0xffffffffl; ++i) {
      build = data.get((int) i);
      if (build != null)
        out.printf(buildAsString(build));
    }
  }

}
