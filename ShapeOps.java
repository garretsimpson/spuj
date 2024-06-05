/**
 * ShapeOps
 */

public class ShapeOps {

  public enum OPS {
    ROTATE_RIGHT, ROTATE_180, ROTATE_LEFT, CUT_LEFT, CUT_RIGHT;

    // Function<Integer, Integer> func;
  };

  public int call(OPS op, int... value) {
    switch (op) {
    case ROTATE_RIGHT:
      return rotateRight(value[0]);
    case ROTATE_180:
      return rotate180(value[0]);
    case ROTATE_LEFT:
      return rotateLeft(value[0]);
    case CUT_RIGHT:
      return cutRight(value[0]);
    case CUT_LEFT:
      return cutLeft(value[0]);
    default:
      return 0;
    }
  }

  public int keyValue(int value) {
    int mval = mirrorValue(value);
    int result = Math.min(value, mval);

    for (int i = 1; i < 4; ++i) {
      result = Math.min(result, rotate(value, i));
      result = Math.min(result, rotate(mval, i));
    }
    return result;
  }

  public int mirrorValue(int value) {
    int result = 0;
    for (int i = 0; i < 4; ++i) {
      result = (result << 1) | (value & 0x11111111);
      value >>>= 1;
    }
    return result;
  }

  public int rotate(int value, int steps) {
    int lShift = steps & 0x3;
    int rShift = 4 - lShift;
    int mask = (0xf >>> rShift) * 0x11111111;
    int result = ((value >>> rShift) & mask) | ((value << lShift) & ~mask);
    return result;
  }

  public int rotateRight(int value) {
    return rotate(value, 1);
  }

  public int rotate180(int value) {
    return rotate(value, 2);
  }

  public int rotateLeft(int value) {
    return rotate(value, 3);
  }

  public int cutLeft(int value) {
    return value & 0xcccccccc;
  }

  public int cutRight(int value) {
    return value & 0x33333333;
  }

}