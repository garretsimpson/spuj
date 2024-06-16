import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * ShapeFile
 */
public class ShapeFile {

  static void write(String name, int[] data) {
    PrintWriter outputStream = null;
    try {
      outputStream = new PrintWriter(new FileWriter(name));
      for (int shape : data) {
        outputStream.println(new Shape(shape));
      }
    } catch (Exception e) {
      System.err.printf("Error writing file: %s\n", name);
    } finally {
      if (outputStream != null)
        outputStream.close();
    }

  }
}
