
/**
 * ShapeOps
 */

public class ShapeOps {

  public int keyCode(int value) {
    int mval = mirrorCode(value);
    int result = Math.min(value, mval);

    for (int i = 1; i < 4; ++i) {
      result = Math.min(result, rotateCode(value, i));
      result = Math.min(result, rotateCode(mval, i));
    }
    return result;
  }

  public static int mirrorCode(int value) {
    int result = 0;
    for (int i = 0; i < 4; ++i) {
      result = (result << 1) | (value & 0x11111111);
      value >>>= 1;
    }
    return result;
  }

  public int rotateCode(int value, int steps) {
    int lShift = steps & 0x3;
    int rShift = 4 - lShift;
    int mask = (0xf >>> rShift) * 0x11111111;
    int result = ((value >>> rShift) & mask) | ((value << lShift) & ~mask);
    return result;
  }

  public int rotateRight(int value) {
    return rotateCode(value, 1);
  }

  public int rotate180(int value) {
    return rotateCode(value, 2);
  }

  public int rotateLeft(int value) {
    return rotateCode(value, 3);
  }

  public int cutLeft(int value) {
    return value & 0xcccccccc;
  }

  public int cutRight(int value) {
    return value & 0x33333333;
  }

}
