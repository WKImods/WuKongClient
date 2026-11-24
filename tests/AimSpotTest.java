package dev.wukong.client;
public class AimSpotTest {
  private static void require(boolean ok) { if (!ok) throw new AssertionError(); }
  public static void main(String[] args) {
    require("TORSO".equals(AimSpot.normalize(null)));
    require("TORSO".equals(AimSpot.normalize("garbage")));
    require("HEAD".equals(AimSpot.next("TORSO")));
    require("NECK".equals(AimSpot.next("HEAD")));
    require("ALL_VISIBLE".equals(AimSpot.next("NECK")));
    require("TORSO".equals(AimSpot.next("ALL_VISIBLE")));
    require(AimSpot.samples("ALL_VISIBLE").length > 16);
    for (String spot : AimSpot.MODES) for (double[] pt : AimSpot.samples(spot))
       for (double coord : pt) require(coord > 0 && coord < 1);
    System.out.println("AimSpot normalization/cycle/geometry tests PASS");
  }
}
