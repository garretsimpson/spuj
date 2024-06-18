import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * ShapeFile
 */
public class ShapeFile {

  static void write(String name, int[] data) {
    System.out.printf("Writing file: %s\n", name);
    try (FileWriter file = new FileWriter(name)) {
      PrintWriter out = new PrintWriter(file);
      for (int shape : data) {
        out.println(new Shape(shape));
      }
    } catch (Exception e) {
      System.err.printf("Error writing file: %s\n", name);
    }
    System.out.printf("Number of values written: %d\n", data.length);
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

}
