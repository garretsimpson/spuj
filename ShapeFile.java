import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * ShapeFile
 */
public class ShapeFile {

  static void write(String name, int[] data) {
    write(name, data, false);
  }

  static void write(String name, Set<Integer> data) {
    write(name, data, false);
  }

  static void append(String name, int[] data) {
    write(name, data, true);
  }

  static void append(String name, Set<Integer> data) {
    write(name, data, true);
  }

  static void write(String name, int[] data, boolean append) {
    System.out.printf("Writing file: %s\n", name);
    try (FileWriter file = new FileWriter(name, append)) {
      PrintWriter out = new PrintWriter(file);
      for (int shape : data) {
        out.println(new Shape(shape));
      }
    } catch (Exception e) {
      System.err.printf("Error writing file: %s\n", name);
    }
    System.out.printf("Number of values written: %d\n", data.length);
  }

  static void write(String name, Set<Integer> data, boolean append) {
    System.out.printf("Writing file: %s\n", name);
    try (FileWriter file = new FileWriter(name, append)) {
      int value;
      PrintWriter out = new PrintWriter(file);
      for (long i = 0; i <= 0xffffffffl; ++i) {
        value = (int) i;
        if (data.contains(value))
          out.println(new Shape(value));
      }
    } catch (Exception e) {
      System.err.printf("Error writing file: %s\n", name);
    }
    System.out.printf("Number of values written: %d\n", data.size());
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
      System.err.println(e);
    }
    System.out.printf("Number of values read: %d\n", dataSet.size());
    return dataSet;
  }

  static void delete(String name) {
    try {
      Files.delete(Paths.get(name));
    } catch (Exception e) {
      System.err.printf("Error deleting file: %s\n", name);
      System.err.println(e);
    }

  }

}
