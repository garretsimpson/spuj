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
import java.util.stream.IntStream;

/**
 * ShapeFile
 */
public class ShapeFile {

  static void write(String name, int[] data) {
    write(name, IntStream.of(data), false);
  }

  static void write(String name, Set<Integer> data) {
    write(name, data.stream().mapToInt(Integer::intValue), false);
  }

  static void writeDB(String name, Map<Integer, Solver.Build> data) {
    writeDB(name, data, false);
  }

  static void appendDB(String name, Map<Integer, Solver.Build> data) {
    writeDB(name, data, true);
  }

  static void append(String name, int[] data) {
    write(name, IntStream.of(data), true);
  }

  static void append(String name, Set<Integer> data) {
    write(name, data.stream().mapToInt(Integer::intValue), true);
  }

  static void delete(String name) {
    try {
      Files.delete(Path.of(name));
    } catch (Exception e) {
      System.err.printf("Error deleting file: %s\n", name);
      e.printStackTrace();
    }
  }

  static void write(String name, IntStream data, boolean append) {
    System.out.printf("Writing file: %s\n", name);
    try (FileWriter file = new FileWriter(name, append)) {
      PrintWriter out = new PrintWriter(file);
      data.forEach(value -> out.printf("%08x\n", value));
    } catch (Exception e) {
      System.err.printf("Error writing file: %s\n", name);
      e.printStackTrace();
    }
  }

  static void writeDB(String name, Map<Integer, Solver.Build> data, boolean append) {
    System.out.printf("Writing file: %s\n", name);
    try (FileWriter file = new FileWriter(name, append)) {
      PrintWriter out = new PrintWriter(file);
      Integer[] shapes = data.keySet().toArray(Integer[]::new);
      Arrays.parallelSort(shapes, (x, y) -> Integer.compareUnsigned(x, y));
      for (Integer shape : shapes) {
        Solver.Build build = data.get(shape);
        out.printf("%08x,%s,%08x,%08x,%02x\n", shape, build.opName.code, build.shape1, build.shape2, build.cost);
      }
    } catch (Exception e) {
      System.err.printf("Error writing file: %s\n", name);
      e.printStackTrace();
    }
  }

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

  static void sort(String name) {
    System.out.printf("Sorting file: %s\n", name);
    Map<Integer, Solver.Build> dataMap = readDB(name);
    writeDB(name, dataMap, false);
  }

  static void write(String name, Set<Integer> data, boolean append) {
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

}
