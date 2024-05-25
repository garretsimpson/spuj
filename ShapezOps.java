
/**
 * ShapezOps
 */

public class ShapezOps {

  public int rotateRight(int value) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'rotateRight'");
  }

  public int cutLeft(int value) {
    return value & 0xcccccccc;
  }

  public int cutRight(int value) {
    return value & 0x33333333;
  }

}
