import java.util.function.Function;

/**
 * Ops
 */

public enum Ops {
  ROTATE_RIGHT(Ops::rotateRight), ROTATE_LEFT(Ops::rotateLeft);

  Function<Integer, Integer> f;

  Ops(Function<Integer, Integer> f) {
    this.f = f;
  }

  public static int rotate(int value, int steps) {
    int lShift = steps & 0x3;
    int rShift = 4 - lShift;
    int mask = (0xf >>> rShift) * 0x11111111;
    int result = ((value >>> rShift) & mask) | ((value << lShift) & ~mask);
    return result;
  }

  public static int rotateRight(int value) {
    return rotate(value, 1);
  }

  public static int rotate180(int value) {
    return rotate(value, 2);
  }

  public static int rotateLeft(int value) {
    return rotate(value, 3);
  }

  // public int cutLeft(int value) {
  // return value & 0xcccccccc;
  // }

  // public int cutRight(int value) {
  // return value & 0x33333333;
  // }

}
