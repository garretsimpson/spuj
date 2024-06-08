import java.util.ArrayList;
import java.util.List;

/**
 * Ops
 */
public enum Ops {
  KEY {
    public int call(int... value) {
      return keyValue(value[0]);
    }
  },
  MIRROR {
    public int call(int... value) {
      return mirrorValue(value[0]);
    }
  },
  ROTATE_RIGHT {
    public int call(int... value) {
      return rotate(value[0], 1);
    }
  },
  ROTATE_180 {
    public int call(int... value) {
      return rotate(value[0], 2);
    }
  },
  ROTATE_LEFT {
    public int call(int... value) {
      return rotate(value[0], 3);
    }
  },
  CUT_RIGHT {
    public int call(int... value) {
      return cutRight(value[0]);
    }
  },
  CUT_LEFT {
    public int call(int... value) {
      return cutLeft(value[0]);
    }
  };

  public abstract int call(int... value);

  /**
   * Unsigned min()
   * 
   * @param x
   * @param y
   * @return Integer value
   */
  private int umin(int x, int y) {
    return Integer.compareUnsigned(x, y) < 0 ? x : y;
  }

  /**
   * Compute the shape's key value.
   * 
   * @param value
   * @return Key value
   */
  int keyValue(int value) {
    int mvalue = mirrorValue(value);
    int result = umin(value, mvalue);

    for (int i = 1; i < 4; ++i) {
      result = umin(result, rotate(value, i));
      result = umin(result, rotate(mvalue, i));
    }
    return result;
  }

  /**
   * Compute the value of the shape's mirror image.
   * 
   * @param value
   * @return Value of the shape's mirror image.
   */
  int mirrorValue(int value) {
    int result = 0;
    for (int i = 0; i < 4; ++i) {
      result = (result << 1) | (value & 0x11111111);
      value >>>= 1;
    }
    return result;
  }

  /**
   * Rotate the shape to the right a given number of steps.
   * 
   * @param value
   * @param steps
   * @return Value of the rotated shape
   */
  int rotate(int value, int steps) {
    int lShift = steps & 0x3;
    int rShift = 4 - lShift;
    int mask = (0xf >>> rShift) * 0x11111111;
    int result = ((value >>> rShift) & mask) | ((value << lShift) & ~mask);
    return result;
  }

  /**
   * Drop a part on top of a shape. The part is assumed to be a single solid part that won't separate.
   * 
   * @param base
   * @param part
   * @param layerNum starting layer number
   * @return
   */
  int dropPart(int base, int part, int layerNum) {
    if (part == 0)
      return base;
    int v1 = Shape.v1(base);
    int v2 = Shape.v2(base);
    int value = v1 | v2;
    for (int offset = layerNum; offset > 0; --offset) {
      if (((part << (4 * (offset - 1))) & value) != 0) {
        return base | ((part << (4 * offset)) & 0xffff);
      }
    }
    return base | part;
  }

  /**
   * Drop a pin on top of a shape.
   * 
   * TODO: Only need the quad/column of the pin. Find top zero.
   * 
   * @param base
   * @param quad
   * @param layerNum starting layer number
   * @return
   */
  int dropPin(int base, int quad, int layerNum) {
    int pin = 1 << quad;
    int v1 = Shape.v1(base);
    int v2 = Shape.v2(base);
    int value = v1 | v2;
    for (int offset = layerNum; offset > 0; --offset) {
      if (((pin << (4 * (offset - 1))) & value) != 0) {
        return base | (Shape.PIN_MASK << (4 * offset + quad));
      }
    }
    return base | (Shape.PIN_MASK << quad);
  }

  private static int[][] NEXT_SPOTS2 = { { 1, 4 }, { 0, 5 }, { 3, 6 }, { 2, 7 }, { 0, 5, 8 }, { 1, 4, 9 }, { 2, 7, 10 },
      { 3, 6, 11 }, { 4, 9, 12 }, { 5, 8, 13 }, { 6, 11, 14 }, { 7, 10, 15 }, { 8, 13 }, { 9, 12 }, { 10, 15 },
      { 11, 14 }, };

  private static int[][] NEXT_SPOTS4 = { { 1, 3, 4 }, { 0, 2, 5 }, { 1, 3, 6 }, { 0, 2, 7 }, { 0, 5, 7, 8 },
      { 1, 4, 6, 9 }, { 2, 5, 7, 10 }, { 3, 4, 6, 11 }, { 4, 9, 11, 12 }, { 5, 8, 10, 13 }, { 6, 9, 11, 14 },
      { 7, 8, 10, 15 }, {}, {}, {}, {}, };

  /**
   * @param shape
   * @param todo
   * @param mesh
   * @return
   */
  int findCrystals(int shape, List<Integer> todo, int[][] mesh) {
    int result = 0;
    int num, val;
    for (int i = 0; i < todo.size(); ++i) {
      num = todo.get(i);
      result |= Shape.CRYSTAL_MASK << num;
      for (int spot : mesh[num]) {
        if (todo.contains(spot))
          continue;
        val = (shape >>> spot) & Shape.CRYSTAL_MASK;
        if (val == Shape.CRYSTAL_MASK)
          todo.add(spot);
      }
    }
    return result;
  }

  /**
   * TODO: Use spot number instead of layers and quads.
   * 
   * @param shape
   * @param quads
   * @return
   */
  int collapseS2(int shape, int[] quads) {
    int part, val;

    // First layer remains unchanged
    int result = shape & Shape.LAYER_MASK;
    for (int layerNum = 1; layerNum < 4; ++layerNum) {
      part = (shape >>> (4 * layerNum)) & Shape.LAYER_MASK;
      if (part == 0)
        continue;
      // Drop all pins
      for (int quad : quads) {
        val = (part >>> quad) & Shape.CRYSTAL_MASK;
        if (val == Shape.PIN_MASK) {
          part &= ~(Shape.PIN_MASK << quad);
          result = dropPin(result, quad, layerNum);
        }
      }

      // Find all parts
      List<Integer> parts = new ArrayList<>();
      int v1 = Shape.v1(part);
      int v2 = Shape.v2(part);
      if (v1 == 0x5) {
        parts.add(part & (Shape.CRYSTAL_MASK << 0));
        parts.add(part & (Shape.CRYSTAL_MASK << 2));
      } else if (v1 == 0xa) {
        parts.add(part & (Shape.CRYSTAL_MASK << 1));
        parts.add(part & (Shape.CRYSTAL_MASK << 3));
      } else {
        parts.add(part);
      }

      int prevLayer;
      for (int part1 : parts) {
        // Check if part is supported
        prevLayer = (result >>> (4 * (layerNum - 1))) & Shape.LAYER_MASK;
        v1 = Shape.v1(prevLayer);
        v2 = Shape.v2(prevLayer);
        boolean supported = (part & 0xffff & (v1 | v2)) != 0;
        if (supported) {
          // copy part
          result |= part << (4 * layerNum);
        } else {
          // break crystals
          v1 = Shape.v1(part);
          v2 = Shape.v2(part);
          part &= ~(v2 * Shape.CRYSTAL_MASK);
          // drop part
          result = dropPart(result, part, layerNum);
        }
      }
    }
    return result;
  }

  /**
   * @param shape
   * @return
   */
  // static int cutLeft(int shape) {
  // List<Integer> layers = Shape.toLayers(shape);
  // // Step 1: break all cut crystals
  // // Check all 8 places that a crystal can span the cut
  // int layer;
  // List<Integer> todo = new ArrayList<>();
  // for (int layerNum = 0; layerNum < layers.size(); ++layerNum) {
  // layer = layers[layerNum];
  // if ((layer & 0x99) == 0x99) todo.add(4 * layerNum + 3);
  // if ((layer & 0x66) == 0x66) todo.add(4 * layerNum + 2);
  // }
  // // Find all connected crystals
  // int found = findCrystals(shape, todo, NEXT_SPOTS2);
  // // Break all connected crystals
  // shape &= ~found;

  // // Step 2: COllapse parts
  // return collapseS2(shape & 0xcccccccc, [2, 3]) >>> 0;
  // }

  int cutRight(int value) {
    return value & 0x33333333;
  }

  int cutLeft(int value) {
    return value & 0xcccccccc;
  }

}
