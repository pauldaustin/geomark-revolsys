/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */
package com.revolsys.jts.geom;

import java.io.Serializable;

import com.revolsys.gis.model.coordinates.DoubleCoordinates;
import com.revolsys.jts.algorithm.CGAlgorithms;
import com.revolsys.jts.algorithm.HCoordinate;
import com.revolsys.jts.algorithm.LineIntersector;
import com.revolsys.jts.algorithm.NotRepresentableException;
import com.revolsys.jts.algorithm.RobustLineIntersector;

/**
 * Represents a line segment defined by two {@link Coordinates}s.
 * Provides methods to compute various geometric properties
 * and relationships of line segments.
 * <p>
 * This class is designed to be easily mutable (to the extent of
 * having its contained points public).
 * This supports a common pattern of reusing a single LineSegment
 * object as a way of computing segment properties on the
 * segments defined by arrays or lists of {@link Coordinates}s.
 *
 *@version 1.7
 */
public class LineSegment implements Comparable, Serializable {
  private static final long serialVersionUID = 3252005833466256227L;

  /**
   * Computes the midpoint of a segment
   *
   * @return the midpoint of the segment
   */
  public static Coordinates midPoint(final Coordinates p0, final Coordinates p1) {
    return new Coordinate((p0.getX() + p1.getX()) / 2,
      (p0.getY() + p1.getY()) / 2, Coordinates.NULL_ORDINATE);
  }

  private Coordinates p0;

  private Coordinates p1;

  public LineSegment() {
    this(new Coordinate(), new Coordinate());
  }

  public LineSegment(final Coordinates p0, final Coordinates p1) {
    this.p0 = p0;
    this.p1 = p1;
  }

  public LineSegment(final double x0, final double y0, final double x1,
    final double y1) {
    this(new Coordinate(x0, y0, Coordinates.NULL_ORDINATE), new Coordinate(x1,
      y1, Coordinates.NULL_ORDINATE));
  }

  public LineSegment(final LineSegment ls) {
    this(ls.getP0(), ls.getP1());
  }

  /**
   * Computes the angle that the vector defined by this segment
   * makes with the X-axis.
   * The angle will be in the range [ -PI, PI ] radians.
   *
   * @return the angle this segment makes with the X-axis (in radians)
   */
  public double angle() {
    return Math.atan2(getP1().getY() - getP0().getY(), getP1().getX()
      - getP0().getX());
  }

  /**
   * Computes the closest point on this line segment to another point.
   * @param p the point to find the closest point to
   * @return a Coordinates which is the closest point on the line segment to the point p
   */
  public Coordinates closestPoint(final Coordinates p) {
    final double factor = projectionFactor(p);
    if (factor > 0 && factor < 1) {
      return project(p);
    }
    final double dist0 = getP0().distance(p);
    final double dist1 = getP1().distance(p);
    if (dist0 < dist1) {
      return getP0();
    }
    return getP1();
  }

  /**
   * Computes the closest points on two line segments.
   * 
   * @param line the segment to find the closest point to
   * @return a pair of Coordinates which are the closest points on the line segments
   */
  public Coordinates[] closestPoints(final LineSegment line) {
    // test for intersection
    final Coordinates intPt = intersection(line);
    if (intPt != null) {
      return new Coordinates[] {
        intPt, intPt
      };
    }

    /**
     *  if no intersection closest pair contains at least one endpoint.
     * Test each endpoint in turn.
     */
    final Coordinates[] closestPt = new Coordinates[2];
    double minDistance = Double.MAX_VALUE;
    double dist;

    final Coordinates close00 = closestPoint(line.getP0());
    minDistance = close00.distance(line.getP0());
    closestPt[0] = close00;
    closestPt[1] = line.getP0();

    final Coordinates close01 = closestPoint(line.getP1());
    dist = close01.distance(line.getP1());
    if (dist < minDistance) {
      minDistance = dist;
      closestPt[0] = close01;
      closestPt[1] = line.getP1();
    }

    final Coordinates close10 = line.closestPoint(getP0());
    dist = close10.distance(getP0());
    if (dist < minDistance) {
      minDistance = dist;
      closestPt[0] = getP0();
      closestPt[1] = close10;
    }

    final Coordinates close11 = line.closestPoint(getP1());
    dist = close11.distance(getP1());
    if (dist < minDistance) {
      minDistance = dist;
      closestPt[0] = getP1();
      closestPt[1] = close11;
    }

    return closestPt;
  }

  /**
   *  Compares this object with the specified object for order.
   *  Uses the standard lexicographic ordering for the points in the LineSegment.
   *
   *@param  o  the <code>LineSegment</code> with which this <code>LineSegment</code>
   *      is being compared
   *@return    a negative integer, zero, or a positive integer as this <code>LineSegment</code>
   *      is less than, equal to, or greater than the specified <code>LineSegment</code>
   */
  @Override
  public int compareTo(final Object o) {
    final LineSegment other = (LineSegment)o;
    final int comp0 = getP0().compareTo(other.getP0());
    if (comp0 != 0) {
      return comp0;
    }
    return getP1().compareTo(other.getP1());
  }

  /**
   * Computes the distance between this line segment and a given point.
   *
   * @return the distance from this segment to the given point
   */
  public double distance(final Coordinates p) {
    return CGAlgorithms.distancePointLine(p, getP0(), getP1());
  }

  /**
   * Computes the distance between this line segment and another segment.
   *
   * @return the distance to the other segment
   */
  public double distance(final LineSegment ls) {
    return CGAlgorithms.distanceLineLine(getP0(), getP1(), ls.getP0(),
      ls.getP1());
  }

  /**
   * Computes the perpendicular distance between the (infinite) line defined
   * by this line segment and a point.
   *
   * @return the perpendicular distance between the defined line and the given point
   */
  public double distancePerpendicular(final Coordinates p) {
    return CGAlgorithms.distancePointLinePerpendicular(p, getP0(), getP1());
  }

  /**
   *  Returns <code>true</code> if <code>other</code> has the same values for
   *  its points.
   *
   *@param  o  a <code>LineSegment</code> with which to do the comparison.
   *@return        <code>true</code> if <code>other</code> is a <code>LineSegment</code>
   *      with the same values for the x and y ordinates.
   */
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof LineSegment)) {
      return false;
    }
    final LineSegment other = (LineSegment)o;
    return getP0().equals(other.getP0()) && getP1().equals(other.getP1());
  }

  /**
   *  Returns <code>true</code> if <code>other</code> is
   *  topologically equal to this LineSegment (e.g. irrespective
   *  of orientation).
   *
   *@param  other  a <code>LineSegment</code> with which to do the comparison.
   *@return        <code>true</code> if <code>other</code> is a <code>LineSegment</code>
   *      with the same values for the x and y ordinates.
   */
  public boolean equalsTopo(final LineSegment other) {
    return getP0().equals(other.getP0()) && getP1().equals(other.getP1())
      || getP0().equals(other.getP1()) && getP1().equals(other.getP0());
  }

  public Coordinates getCoordinate(final int i) {
    if (i == 0) {
      return getP0();
    }
    return getP1();
  }

  /**
   * Computes the length of the line segment.
   * @return the length of the line segment
   */
  public double getLength() {
    return getP0().distance(getP1());
  }

  public Coordinates getP0() {
    return p0;
  }

  public Coordinates getP1() {
    return p1;
  }

  /**
   * Gets a hashcode for this object.
   * 
   * @return a hashcode for this object
   */
  @Override
  public int hashCode() {
    long bits0 = java.lang.Double.doubleToLongBits(getP0().getX());
    bits0 ^= java.lang.Double.doubleToLongBits(getP0().getY()) * 31;
    final int hash0 = (((int)bits0) ^ ((int)(bits0 >> 32)));

    long bits1 = java.lang.Double.doubleToLongBits(getP1().getX());
    bits1 ^= java.lang.Double.doubleToLongBits(getP1().getY()) * 31;
    final int hash1 = (((int)bits1) ^ ((int)(bits1 >> 32)));

    // XOR is supposed to be a good way to combine hashcodes
    return hash0 ^ hash1;
  }

  /**
   * Computes an intersection point between two line segments, if there is one.
   * There may be 0, 1 or many intersection points between two segments.
   * If there are 0, null is returned. If there is 1 or more, 
   * exactly one of them is returned 
   * (chosen at the discretion of the algorithm).  
   * If more information is required about the details of the intersection,
   * the {@link RobustLineIntersector} class should be used.
   *
   * @param line a line segment
   * @return an intersection point, or <code>null</code> if there is none
   * 
   * @see RobustLineIntersector
   */
  public Coordinates intersection(final LineSegment line) {
    final LineIntersector li = new RobustLineIntersector();
    li.computeIntersection(getP0(), getP1(), line.getP0(), line.getP1());
    if (li.hasIntersection()) {
      return li.getIntersection(0);
    }
    return null;
  }

  /**
   * Tests whether the segment is horizontal.
   *
   * @return <code>true</code> if the segment is horizontal
   */
  public boolean isHorizontal() {
    return getP0().getY() == getP1().getY();
  }

  /**
   * Tests whether the segment is vertical.
   *
   * @return <code>true</code> if the segment is vertical
   */
  public boolean isVertical() {
    return getP0().getX() == getP1().getX();
  }

  /**
   * Computes the intersection point of the lines of infinite extent defined
   * by two line segments (if there is one).
   * There may be 0, 1 or an infinite number of intersection points 
   * between two lines.
   * If there is a unique intersection point, it is returned. 
   * Otherwise, <tt>null</tt> is returned.
   * If more information is required about the details of the intersection,
   * the {@link RobustLineIntersector} class should be used.
   *
   * @param line a line segment defining an straight line with infinite extent
   * @return an intersection point, 
   * or <code>null</code> if there is no point of intersection
   * or an infinite number of intersection points
   * 
   * @see RobustLineIntersector
   */
  public Coordinates lineIntersection(final LineSegment line) {
    try {
      final Coordinates intPt = HCoordinate.intersection(getP0(), getP1(),
        line.getP0(), line.getP1());
      return intPt;
    } catch (final NotRepresentableException ex) {
      // eat this exception, and return null;
    }
    return null;
  }

  /**
   * Computes the midpoint of the segment
   *
   * @return the midpoint of the segment
   */
  public Coordinates midPoint() {
    return midPoint(getP0(), getP1());
  }

  /**
   * Puts the line segment into a normalized form.
   * This is useful for using line segments in maps and indexes when
   * topological equality rather than exact equality is desired.
   * A segment in normalized form has the first point smaller
   * than the second (according to the standard ordering on {@link Coordinates}).
   */

  public LineSegment normalize() {
    if (getP1().compareTo(getP0()) < 0) {
      return new LineSegment(getP1(), getP0());
    } else {
      return this;
    }
  }

  /**
   * Determines the orientation index of a {@link Coordinates} relative to this segment.
   * The orientation index is as defined in {@link CGAlgorithms#computeOrientation}.
   *
   * @param p the coordinate to compare
   *
   * @return 1 (LEFT) if <code>p</code> is to the left of this segment
   * @return -1 (RIGHT) if <code>p</code> is to the right of this segment
   * @return 0 (COLLINEAR) if <code>p</code> is collinear with this segment
   * 
   * @see CGAlgorithms#computeOrientation(Coordinate, Coordinate, Coordinate)
   */
  public int orientationIndex(final Coordinates p) {
    return CGAlgorithms.orientationIndex(getP0(), getP1(), p);
  }

  /**
   * Determines the orientation of a LineSegment relative to this segment.
   * The concept of orientation is specified as follows:
   * Given two line segments A and L,
   * <ul
   * <li>A is to the left of a segment L if A lies wholly in the
   * closed half-plane lying to the left of L
   * <li>A is to the right of a segment L if A lies wholly in the
   * closed half-plane lying to the right of L
   * <li>otherwise, A has indeterminate orientation relative to L. This
   * happens if A is collinear with L or if A crosses the line determined by L.
   * </ul>
   *
   * @param seg the LineSegment to compare
   *
   * @return 1 if <code>seg</code> is to the left of this segment
   * @return -1 if <code>seg</code> is to the right of this segment
   * @return 0 if <code>seg</code> has indeterminate orientation relative to this segment
   */
  public int orientationIndex(final LineSegment seg) {
    final int orient0 = CGAlgorithms.orientationIndex(getP0(), getP1(),
      seg.getP0());
    final int orient1 = CGAlgorithms.orientationIndex(getP0(), getP1(),
      seg.getP1());
    // this handles the case where the points are L or collinear
    if (orient0 >= 0 && orient1 >= 0) {
      return Math.max(orient0, orient1);
    }
    // this handles the case where the points are R or collinear
    if (orient0 <= 0 && orient1 <= 0) {
      return Math.max(orient0, orient1);
    }
    // points lie on opposite sides ==> indeterminate orientation
    return 0;
  }

  /**
   * Computes the {@link Coordinates} that lies a given
   * fraction along the line defined by this segment.
   * A fraction of <code>0.0</code> returns the start point of the segment;
   * a fraction of <code>1.0</code> returns the end point of the segment.
   * If the fraction is < 0.0 or > 1.0 the point returned 
   * will lie before the start or beyond the end of the segment. 
   *
   * @param segmentLengthFraction the fraction of the segment length along the line
   * @return the point at that distance
   */
  public Coordinates pointAlong(final double segmentLengthFraction) {
    final double x = getP0().getX() + segmentLengthFraction
      * (getP1().getX() - getP0().getX());
    final double y = getP0().getY() + segmentLengthFraction
      * (getP1().getY() - getP0().getY());
    final Coordinates coord = new DoubleCoordinates(x, y,
      Coordinates.NULL_ORDINATE);
    return coord;
  }

  /**
   * Computes the {@link Coordinates} that lies a given
   * fraction along the line defined by this segment and offset from 
   * the segment by a given distance.
   * A fraction of <code>0.0</code> offsets from the start point of the segment;
   * a fraction of <code>1.0</code> offsets from the end point of the segment.
   * The computed point is offset to the left of the line if the offset distance is
   * positive, to the right if negative.
   *
   * @param segmentLengthFraction the fraction of the segment length along the line
   * @param offsetDistance the distance the point is offset from the segment
   *    (positive is to the left, negative is to the right)
   * @return the point at that distance and offset
   * 
   * @throws IllegalStateException if the segment has zero length
   */
  public Coordinates pointAlongOffset(final double segmentLengthFraction,
    final double offsetDistance) {
    // the point on the segment line
    final double segx = getP0().getX() + segmentLengthFraction
      * (getP1().getX() - getP0().getX());
    final double segy = getP0().getY() + segmentLengthFraction
      * (getP1().getY() - getP0().getY());

    final double dx = getP1().getX() - getP0().getX();
    final double dy = getP1().getY() - getP0().getY();
    final double len = Math.sqrt(dx * dx + dy * dy);
    double ux = 0.0;
    double uy = 0.0;
    if (offsetDistance != 0.0) {
      if (len <= 0.0) {
        throw new IllegalStateException(
          "Cannot compute offset from zero-length line segment");
      }

      // u is the vector that is the length of the offset, in the direction of
      // the segment
      ux = offsetDistance * dx / len;
      uy = offsetDistance * dy / len;
    }

    // the offset point is the seg point plus the offset vector rotated 90
    // degrees CCW
    final double offsetx = segx - uy;
    final double offsety = segy + ux;

    final Coordinates coord = new Coordinate(offsetx, offsety,
      Coordinates.NULL_ORDINATE);
    return coord;
  }

  /**
   * Compute the projection of a point onto the line determined
   * by this line segment.
   * <p>
   * Note that the projected point
   * may lie outside the line segment.  If this is the case,
   * the projection factor will lie outside the range [0.0, 1.0].
   */
  public Coordinates project(final Coordinates p) {
    if (p.equals(getP0()) || p.equals(getP1())) {
      return new Coordinate(p);
    }

    final double r = projectionFactor(p);
    final double x = getP0().getX() + r * (getP1().getX() - getP0().getX());
    final double y = getP0().getY() + r * (getP1().getY() - getP0().getY());
    final Coordinates coord = new DoubleCoordinates(x, y,
      Coordinates.NULL_ORDINATE);
    return coord;
  }

  /**
   * Project a line segment onto this line segment and return the resulting
   * line segment.  The returned line segment will be a subset of
   * the target line line segment.  This subset may be null, if
   * the segments are oriented in such a way that there is no projection.
   * <p>
   * Note that the returned line may have zero length (i.e. the same endpoints).
   * This can happen for instance if the lines are perpendicular to one another.
   *
   * @param seg the line segment to project
   * @return the projected line segment, or <code>null</code> if there is no overlap
   */
  public LineSegment project(final LineSegment seg) {
    final double pf0 = projectionFactor(seg.getP0());
    final double pf1 = projectionFactor(seg.getP1());
    // check if segment projects at all
    if (pf0 >= 1.0 && pf1 >= 1.0) {
      return null;
    }
    if (pf0 <= 0.0 && pf1 <= 0.0) {
      return null;
    }

    Coordinates newp0 = project(seg.getP0());
    if (pf0 < 0.0) {
      newp0 = getP0();
    }
    if (pf0 > 1.0) {
      newp0 = getP1();
    }

    Coordinates newp1 = project(seg.getP1());
    if (pf1 < 0.0) {
      newp1 = getP0();
    }
    if (pf1 > 1.0) {
      newp1 = getP1();
    }

    return new LineSegment(newp0, newp1);
  }

  /**
   * Computes the Projection Factor for the projection of the point p
   * onto this LineSegment.  The Projection Factor is the constant r
   * by which the vector for this segment must be multiplied to
   * equal the vector for the projection of <tt>p<//t> on the line
   * defined by this segment.
   * <p>
   * The projection factor will lie in the range <tt>(-inf, +inf)</tt>,
   * or be <code>NaN</code> if the line segment has zero length..
   * 
   * @param p the point to compute the factor for
   * @return the projection factor for the point
   */
  public double projectionFactor(final Coordinates p) {
    if (p.equals(getP0())) {
      return 0.0;
    }
    if (p.equals(getP1())) {
      return 1.0;
    }
    // Otherwise, use comp.graphics.algorithms Frequently Asked Questions method
    /*
     * AC dot AB r = --------- ||AB||^2 r has the following meaning: r=0 P = A
     * r=1 P = B r<0 P is on the backward extension of AB r>1 P is on the
     * forward extension of AB 0<r<1 P is interior to AB
     */
    final double dx = getP1().getX() - getP0().getX();
    final double dy = getP1().getY() - getP0().getY();
    final double len = dx * dx + dy * dy;

    // handle zero-length segments
    if (len <= 0.0) {
      return Double.NaN;
    }

    final double r = ((p.getX() - getP0().getX()) * dx + (p.getY() - getP0().getY())
      * dy)
      / len;
    return r;
  }

  /**
   * Reverses the direction of the line segment.
   */
  public void reverse() {
    final Coordinates temp = getP0();
    setP0(getP1());
    setP1(temp);
  }

  /**
   * Computes the fraction of distance (in <tt>[0.0, 1.0]</tt>) 
   * that the projection of a point occurs along this line segment.
   * If the point is beyond either ends of the line segment,
   * the closest fractional value (<tt>0.0</tt> or <tt>1.0</tt>) is returned.
   * <p>
   * Essentially, this is the {@link #projectionFactor} clamped to 
   * the range <tt>[0.0, 1.0]</tt>.
   * If the segment has zero length, 1.0 is returned.
   *  
   * @param inputPt the point
   * @return the fraction along the line segment the projection of the point occurs
   */
  public double segmentFraction(final Coordinates inputPt) {
    double segFrac = projectionFactor(inputPt);
    if (segFrac < 0.0) {
      segFrac = 0.0;
    } else if (segFrac > 1.0 || Double.isNaN(segFrac)) {
      segFrac = 1.0;
    }
    return segFrac;
  }

  public void setCoordinates(final Coordinates p0, final Coordinates p1) {
    this.getP0().setX(p0.getX());
    this.getP0().setY(p0.getY());
    this.getP1().setX(p1.getX());
    this.getP1().setY(p1.getY());
  }

  public void setCoordinates(final LineSegment ls) {
    setCoordinates(ls.getP0(), ls.getP1());
  }

  public void setP0(final Coordinates p0) {
    this.p0 = p0;
  }

  public void setP1(final Coordinates p1) {
    this.p1 = p1;
  }

  /**
   * Creates a LineString with the same coordinates as this segment
   * 
   * @param geomFactory the geometery factory to use
   * @return a LineString with the same geometry as this segment
   */
  public LineString toGeometry(final GeometryFactory geomFactory) {
    return geomFactory.lineString(new Coordinates[] {
      getP0(), getP1()
    });
  }

  @Override
  public String toString() {
    return "LINESTRING( " + getP0().getX() + " " + getP0().getY() + ", "
      + getP1().getX() + " " + getP1().getY() + ")";
  }
}
