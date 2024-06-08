/**
 * OpTests
 */
enum OpTests {
  T01(Ops.MIRROR, 0x84c2, 0x1234) {},
  T02(Ops.MIRROR, 0x84c2a6e1, 0x12345678) {},
  T03(Ops.KEY, 0x1624, 0x4321) {},
  T04(Ops.KEY, 0x10000000, 0x10000000) {},
  T05(Ops.KEY, 0x1e6a2c48, 0x87654321) {},
  T06(Ops.ROTATE_LEFT, 0x0008, 0x0001) {},
  T07(Ops.ROTATE_LEFT, 0x8124, 0x1248) {},
  T08(Ops.ROTATE_LEFT, 0x00080087, 0x0001001e) {},
  T09(Ops.ROTATE_RIGHT, 0x0002, 0x0001) {},
  T10(Ops.ROTATE_180, 0x0004, 0x0001) {},
  T11(Ops.CUT_LEFT, 0x00cc, 0x936c) {},
  T12(Ops.CUT_RIGHT, 0x0132, 0x936c) {},
  T13(Ops.CUT_LEFT, 0x000c0000, 0x000f0000) {},
  T14(Ops.CUT_RIGHT, 0x00030000, 0x000f0000) {};
  // ["cutS2Code", [0x936c], [0x00cc, 0x0132]],
  // ["cutS2Code", [0x000f0000], [0x000c0000, 0x00030000]],
  // ["cutS2Code", [0x000f000f], [0x0000, 0x0000]],
  // ["cutS2Code", [0xe8c4f8c4], [0x0000, 0x0001]],
  // ["cutS2Code", [0x00500073], [0x0000, 0x00100033]],
  // ["cutS2Code", [0x005e00ff], [0x0008, 0x00100031]],
  // ["swapCode", [0x000f, 0x000f], [0x000f, 0x000f]],
  // ["swapCode", [0x0009, 0x0006], [0x0005, 0x000a]],
  // ["stackS2Code", [0xfffa, 0x5111], 0x511b],
  // ["stackS2Code", [0x00010000, 0x1111], 0x1111],
  // ["stackS2Code", [0x000f, 0x00010000], 0x000100f0],
  // ["stackS2Code", [0x00100001, 0x00010110], 0x00011110],
  // ["stackS2Code", [0x000f0000, 0x08ce], 0x842108ce],
  // ["crystalCode", [0x0001], 0x000e000f],
  // ["crystalCode", [0x00010010], 0x00ef00ff],
  // ["pinPushCode", [0x0001], 0x00010010],
  // ["pinPushCode", [0x00030030], 0x00330300],
  // ["pinPushCode", [0xf931], 0x00019310],
  // ["pinPushCode", [0x11701571], 0x00010014],

  Ops op;
  int result;
  int[] values;

  OpTests(Ops op, int result, int... values) {
    this.op = op;
    this.result = result;
    this.values = values;
  }

  public static void run() {
    System.out.println("Run OpTests...");
    int result;
    boolean pass;
    for (OpTests t : OpTests.values()) {
      result = t.op.call(t.values);
      pass = (result == t.result);
      System.out.printf("%s %s %s %08x\n", pass ? "PASS" : "FAIL", t.name(), t.op.name(), t.values[0]);
      if (!pass) {
        System.out.printf("  returned %08x, expected %08x\n", result, t.result);
      }
    }
    System.out.println();
  }
}
