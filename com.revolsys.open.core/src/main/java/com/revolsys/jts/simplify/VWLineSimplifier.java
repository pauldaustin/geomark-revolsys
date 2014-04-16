package com.revolsys.jts.simplify;

import com.revolsys.jts.geom.Coordinate;
import com.revolsys.jts.geom.CoordinateList;
import com.revolsys.jts.geom.Coordinates;
import com.revolsys.jts.geom.Triangle;

/**
 * Simplifies a linestring (sequence of points) using the 
 * Visvalingam-Whyatt algorithm.
 * The Visvalingam-Whyatt algorithm simplifies geometry 
 * by removing vertices while trying to minimize the area changed.
 * 
 * @version 1.7
 */
class VWLineSimplifier {
  static class VWVertex {
    public static double MAX_AREA = Double.MAX_VALUE;

    public static VWLineSimplifier.VWVertex buildLine(final Coordinates[] pts) {
      VWLineSimplifier.VWVertex first = null;
      VWLineSimplifier.VWVertex prev = null;
      for (int i = 0; i < pts.length; i++) {
        final VWLineSimplifier.VWVertex v = new VWVertex(pts[i]);
        if (first == null) {
          first = v;
        }
        v.setPrev(prev);
        if (prev != null) {
          prev.setNext(v);
          prev.updateArea();
        }
        prev = v;
      }
      return first;
    }

    private final Coordinates pt;

    private VWLineSimplifier.VWVertex prev;

    private VWLineSimplifier.VWVertex next;

    private double area = MAX_AREA;

    private boolean isLive = true;

    public VWVertex(final Coordinates pt) {
      this.pt = pt;
    }

    public double getArea() {
      return area;
    }

    public Coordinates[] getCoordinates() {
      final CoordinateList coords = new CoordinateList();
      VWLineSimplifier.VWVertex curr = this;
      do {
        coords.add(curr.pt, false);
        curr = curr.next;
      } while (curr != null);
      return coords.toCoordinateArray();
    }

    public boolean isLive() {
      return isLive;
    }

    public VWLineSimplifier.VWVertex remove() {
      final VWLineSimplifier.VWVertex tmpPrev = prev;
      final VWLineSimplifier.VWVertex tmpNext = next;
      VWLineSimplifier.VWVertex result = null;
      if (prev != null) {
        prev.setNext(tmpNext);
        prev.updateArea();
        result = prev;
      }
      if (next != null) {
        next.setPrev(tmpPrev);
        next.updateArea();
        if (result == null) {
          result = next;
        }
      }
      isLive = false;
      return result;
    }

    public void setNext(final VWLineSimplifier.VWVertex next) {
      this.next = next;
    }

    public void setPrev(final VWLineSimplifier.VWVertex prev) {
      this.prev = prev;
    }

    public void updateArea() {
      if (prev == null || next == null) {
        area = MAX_AREA;
        return;
      }
      area = Math.abs(Triangle.area(prev.pt, pt, next.pt));
    }
  }

  public static Coordinates[] simplify(final Coordinates[] pts,
    final double distanceTolerance) {
    final VWLineSimplifier simp = new VWLineSimplifier(pts, distanceTolerance);
    return simp.simplify();
  }

  private final Coordinates[] pts;

  private final double tolerance;

  public VWLineSimplifier(final Coordinates[] pts,
    final double distanceTolerance) {
    this.pts = pts;
    this.tolerance = distanceTolerance * distanceTolerance;
  }

  public Coordinates[] simplify() {
    final VWLineSimplifier.VWVertex vwLine = VWVertex.buildLine(pts);
    double minArea = tolerance;
    do {
      minArea = simplifyVertex(vwLine);
    } while (minArea < tolerance);
    final Coordinates[] simp = vwLine.getCoordinates();
    // ensure computed value is a valid line
    if (simp.length < 2) {
      return new Coordinates[] {
        simp[0], new Coordinate(simp[0])
      };
    }
    return simp;
  }

  private double simplifyVertex(final VWLineSimplifier.VWVertex vwLine) {
    /**
     * Scan vertices in line and remove the one with smallest effective area.
     */
    // TODO: use an appropriate data structure to optimize finding the smallest
    // area vertex
    VWLineSimplifier.VWVertex curr = vwLine;
    double minArea = curr.getArea();
    VWLineSimplifier.VWVertex minVertex = null;
    while (curr != null) {
      final double area = curr.getArea();
      if (area < minArea) {
        minArea = area;
        minVertex = curr;
      }
      curr = curr.next;
    }
    if (minVertex != null && minArea < tolerance) {
      minVertex.remove();
    }
    if (!vwLine.isLive()) {
      return -1;
    }
    return minArea;
  }
}