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
package com.revolsys.jts.operation.valid;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.revolsys.jts.algorithm.CGAlgorithms;
import com.revolsys.jts.algorithm.LineIntersector;
import com.revolsys.jts.algorithm.MCPointInRing;
import com.revolsys.jts.algorithm.PointInRing;
import com.revolsys.jts.algorithm.RobustLineIntersector;
import com.revolsys.jts.geom.Coordinates;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryCollection;
import com.revolsys.jts.geom.LineString;
import com.revolsys.jts.geom.LinearRing;
import com.revolsys.jts.geom.MultiPoint;
import com.revolsys.jts.geom.MultiPolygon;
import com.revolsys.jts.geom.Point;
import com.revolsys.jts.geom.Polygon;
import com.revolsys.jts.geomgraph.Edge;
import com.revolsys.jts.geomgraph.EdgeIntersection;
import com.revolsys.jts.geomgraph.EdgeIntersectionList;
import com.revolsys.jts.geomgraph.GeometryGraph;
import com.revolsys.jts.util.Assert;

/**
 * Implements the algorithms required to compute the <code>isValid()</code> method
 * for {@link Geometry}s.
 * See the documentation for the various geometry types for a specification of validity.
 *
 * @version 1.7
 */
public class IsValidOp {
  /**
   * Find a point from the list of testCoords
   * that is NOT a node in the edge for the list of searchCoords
   *
   * @return the point found, or <code>null</code> if none found
   */
  public static Coordinates findPtNotNode(final Coordinates[] testCoords,
    final LinearRing searchRing, final GeometryGraph graph) {
    // find edge corresponding to searchRing.
    final Edge searchEdge = graph.findEdge(searchRing);
    // find a point in the testCoords which is not a node of the searchRing
    final EdgeIntersectionList eiList = searchEdge.getEdgeIntersectionList();
    // somewhat inefficient - is there a better way? (Use a node map, for
    // instance?)
    for (int i = 0; i < testCoords.length; i++) {
      final Coordinates pt = testCoords[i];
      if (!eiList.isIntersection(pt)) {
        return pt;
      }
    }
    return null;
  }

  /**
   * Checks whether a coordinate is valid for processing.
   * Coordinates are valid iff their x and y ordinates are in the
   * range of the floating point representation.
   *
   * @param coord the coordinate to validate
   * @return <code>true</code> if the coordinate is valid
   */
  public static boolean isValid(final Coordinates coord) {
    if (Double.isNaN(coord.getX())) {
      return false;
    }
    if (Double.isInfinite(coord.getX())) {
      return false;
    }
    if (Double.isNaN(coord.getY())) {
      return false;
    }
    if (Double.isInfinite(coord.getY())) {
      return false;
    }
    return true;
  }

  /**
   * Tests whether a {@link Geometry} is valid.
   * @param geom the Geometry to test
   * @return true if the geometry is valid
   */
  public static boolean isValid(final Geometry geom) {
    final IsValidOp isValidOp = new IsValidOp(geom);
    return isValidOp.isValid();
  }

  private final Geometry parentGeometry; // the base Geometry to be validated

  /**
   * If the following condition is TRUE JTS will validate inverted shells and exverted holes
   * (the ESRI SDE model)
   */
  private boolean isSelfTouchingRingFormingHoleValid = false;

  private TopologyValidationError validErr;

  public IsValidOp(final Geometry parentGeometry) {
    this.parentGeometry = parentGeometry;
  }

  private void checkClosedRing(final LinearRing ring) {
    if (!ring.isClosed()) {
      Coordinates pt = null;
      if (ring.getVertexCount() >= 1) {
        pt = ring.getCoordinate(0);
      }
      validErr = new TopologyValidationError(
        TopologyValidationError.RING_NOT_CLOSED, pt);
    }
  }

  private void checkClosedRings(final Polygon poly) {
    checkClosedRing(poly.getExteriorRing());
    if (hasError()) {
      return;
    }
    for (int i = 0; i < poly.getNumInteriorRing(); i++) {
      checkClosedRing(poly.getInteriorRingN(i));
      if (hasError()) {
        return;
      }
    }
  }

  private void checkConnectedInteriors(final GeometryGraph graph) {
    final ConnectedInteriorTester cit = new ConnectedInteriorTester(graph);
    if (!cit.isInteriorsConnected()) {
      validErr = new TopologyValidationError(
        TopologyValidationError.DISCONNECTED_INTERIOR, cit.getCoordinate());
    }
  }

  /**
   * Checks that the arrangement of edges in a polygonal geometry graph
   * forms a consistent area.
   *
   * @param graph
   *
   * @see ConsistentAreaTester
   */
  private void checkConsistentArea(final GeometryGraph graph) {
    final ConsistentAreaTester cat = new ConsistentAreaTester(graph);
    final boolean isValidArea = cat.isNodeConsistentArea();
    if (!isValidArea) {
      validErr = new TopologyValidationError(
        TopologyValidationError.SELF_INTERSECTION, cat.getInvalidPoint());
      return;
    }
    if (cat.hasDuplicateRings()) {
      validErr = new TopologyValidationError(
        TopologyValidationError.DUPLICATE_RINGS, cat.getInvalidPoint());
    }
  }

  /**
   * Tests that each hole is inside the polygon shell.
   * This routine assumes that the holes have previously been tested
   * to ensure that all vertices lie on the shell oon the same side of it
   * (i.e that the hole rings do not cross the shell ring).
   * In other words, this test is only correct if the ConsistentArea test is passed first.
   * Given this, a simple point-in-polygon test of a single point in the hole can be used,
   * provided the point is chosen such that it does not lie on the shell.
   *
   * @param p the polygon to be tested for hole inclusion
   * @param graph a GeometryGraph incorporating the polygon
   */
  private void checkHolesInShell(final Polygon p, final GeometryGraph graph) {
    final LinearRing shell = p.getExteriorRing();

    // PointInRing pir = new SimplePointInRing(shell);
    // PointInRing pir = new SIRtreePointInRing(shell);
    final PointInRing pir = new MCPointInRing(shell);
    for (int i = 0; i < p.getNumInteriorRing(); i++) {
      final LinearRing hole = p.getInteriorRingN(i);
      final Coordinates holePt = findPtNotNode(hole.getCoordinateArray(),
        shell, graph);
      /**
       * If no non-node hole vertex can be found, the hole must
       * split the polygon into disconnected interiors.
       * This will be caught by a subsequent check.
       */
      if (holePt == null) {
        return;
      } else {

        final boolean outside = !pir.isInside(holePt);
        if (outside) {
          validErr = new TopologyValidationError(
            TopologyValidationError.HOLE_OUTSIDE_SHELL, holePt);
          return;
        }
      }
    }
  }

  /**
   * Tests that no hole is nested inside another hole.
   * This routine assumes that the holes are disjoint.
   * To ensure this, holes have previously been tested
   * to ensure that:
   * <ul>
   * <li>they do not partially overlap
   *      (checked by <code>checkRelateConsistency</code>)
   * <li>they are not identical
   *      (checked by <code>checkRelateConsistency</code>)
   * </ul>
   */
  private void checkHolesNotNested(final Polygon p, final GeometryGraph graph) {
    final IndexedNestedRingTester nestedTester = new IndexedNestedRingTester(
      graph);
    // SimpleNestedRingTester nestedTester = new SimpleNestedRingTester(arg[0]);
    // SweeplineNestedRingTester nestedTester = new
    // SweeplineNestedRingTester(arg[0]);

    for (int i = 0; i < p.getNumInteriorRing(); i++) {
      final LinearRing innerHole = p.getInteriorRingN(i);
      nestedTester.add(innerHole);
    }
    final boolean isNonNested = nestedTester.isNonNested();
    if (!isNonNested) {
      validErr = new TopologyValidationError(
        TopologyValidationError.NESTED_HOLES, nestedTester.getNestedPoint());
    }
  }

  private void checkInvalidCoordinates(final Coordinates[] coords) {
    for (int i = 0; i < coords.length; i++) {
      if (!isValid(coords[i])) {
        validErr = new TopologyValidationError(
          TopologyValidationError.INVALID_COORDINATE, coords[i]);
        return;
      }
    }
  }

  private void checkInvalidCoordinates(final Polygon poly) {
    checkInvalidCoordinates(poly.getExteriorRing().getCoordinateArray());
    if (hasError()) {
      return;
    }
    for (int i = 0; i < poly.getNumInteriorRing(); i++) {
      checkInvalidCoordinates(poly.getInteriorRingN(i).getCoordinateArray());
      if (hasError()) {
        return;
      }
    }
  }

  /**
   * Check that a ring does not self-intersect, except at its endpoints.
   * Algorithm is to count the number of times each node along edge occurs.
   * If any occur more than once, that must be a self-intersection.
   */
  private void checkNoSelfIntersectingRing(final EdgeIntersectionList eiList) {
    final Set nodeSet = new TreeSet();
    boolean isFirst = true;
    for (final Iterator i = eiList.iterator(); i.hasNext();) {
      final EdgeIntersection ei = (EdgeIntersection)i.next();
      if (isFirst) {
        isFirst = false;
        continue;
      }
      if (nodeSet.contains(ei.coord)) {
        validErr = new TopologyValidationError(
          TopologyValidationError.RING_SELF_INTERSECTION, ei.coord);
        return;
      } else {
        nodeSet.add(ei.coord);
      }
    }
  }

  /**
   * Check that there is no ring which self-intersects (except of course at its endpoints).
   * This is required by OGC topology rules (but not by other models
   * such as ESRI SDE, which allow inverted shells and exverted holes).
   *
   * @param graph the topology graph of the geometry
   */
  private void checkNoSelfIntersectingRings(final GeometryGraph graph) {
    for (final Iterator i = graph.getEdgeIterator(); i.hasNext();) {
      final Edge e = (Edge)i.next();
      checkNoSelfIntersectingRing(e.getEdgeIntersectionList());
      if (hasError()) {
        return;
      }
    }
  }

  /**
   * This routine checks to see if a shell is properly contained in a hole.
   * It assumes that the edges of the shell and hole do not
   * properly intersect.
   *
   * @return <code>null</code> if the shell is properly contained, or
   *   a Coordinates which is not inside the hole if it is not
   *
   */
  private Coordinates checkShellInsideHole(final LinearRing shell,
    final LinearRing hole, final GeometryGraph graph) {
    final Coordinates[] shellPts = shell.getCoordinateArray();
    final Coordinates[] holePts = hole.getCoordinateArray();
    // TODO: improve performance of this - by sorting pointlists for instance?
    final Coordinates shellPt = findPtNotNode(shellPts, hole, graph);
    // if point is on shell but not hole, check that the shell is inside the
    // hole
    if (shellPt != null) {
      final boolean insideHole = CGAlgorithms.isPointInRing(shellPt, holePts);
      if (!insideHole) {
        return shellPt;
      }
    }
    final Coordinates holePt = findPtNotNode(holePts, shell, graph);
    // if point is on hole but not shell, check that the hole is outside the
    // shell
    if (holePt != null) {
      final boolean insideShell = CGAlgorithms.isPointInRing(holePt, shellPts);
      if (insideShell) {
        return holePt;
      }
      return null;
    }
    Assert.shouldNeverReachHere("points in shell and hole appear to be equal");
    return null;
  }

  /**
   * Check if a shell is incorrectly nested within a polygon.  This is the case
   * if the shell is inside the polygon shell, but not inside a polygon hole.
   * (If the shell is inside a polygon hole, the nesting is valid.)
   * <p>
   * The algorithm used relies on the fact that the rings must be properly contained.
   * E.g. they cannot partially overlap (this has been previously checked by
   * <code>checkRelateConsistency</code> )
   */
  private void checkShellNotNested(final LinearRing shell, final Polygon p,
    final GeometryGraph graph) {
    final Coordinates[] shellPts = shell.getCoordinateArray();
    // test if shell is inside polygon shell
    final LinearRing polyShell = p.getExteriorRing();
    final Coordinates[] polyPts = polyShell.getCoordinateArray();
    final Coordinates shellPt = findPtNotNode(shellPts, polyShell, graph);
    // if no point could be found, we can assume that the shell is outside the
    // polygon
    if (shellPt == null) {
      return;
    }
    final boolean insidePolyShell = CGAlgorithms.isPointInRing(shellPt, polyPts);
    if (!insidePolyShell) {
      return;
    }

    // if no holes, this is an error!
    if (p.getNumInteriorRing() <= 0) {
      validErr = new TopologyValidationError(
        TopologyValidationError.NESTED_SHELLS, shellPt);
      return;
    }

    /**
     * Check if the shell is inside one of the holes.
     * This is the case if one of the calls to checkShellInsideHole
     * returns a null coordinate.
     * Otherwise, the shell is not properly contained in a hole, which is an error.
     */
    Coordinates badNestedPt = null;
    for (int i = 0; i < p.getNumInteriorRing(); i++) {
      final LinearRing hole = p.getInteriorRingN(i);
      badNestedPt = checkShellInsideHole(shell, hole, graph);
      if (badNestedPt == null) {
        return;
      }
    }
    validErr = new TopologyValidationError(
      TopologyValidationError.NESTED_SHELLS, badNestedPt);
  }

  /**
   * Tests that no element polygon is wholly in the interior of another element polygon.
   * <p>
   * Preconditions:
   * <ul>
   * <li>shells do not partially overlap
   * <li>shells do not touch along an edge
   * <li>no duplicate rings exist
   * </ul>
   * This routine relies on the fact that while polygon shells may touch at one or
   * more vertices, they cannot touch at ALL vertices.
   */
  private void checkShellsNotNested(final MultiPolygon mp,
    final GeometryGraph graph) {
    for (int i = 0; i < mp.getNumGeometries(); i++) {
      final Polygon p = (Polygon)mp.getGeometry(i);
      final LinearRing shell = p.getExteriorRing();
      for (int j = 0; j < mp.getNumGeometries(); j++) {
        if (i == j) {
          continue;
        }
        final Polygon p2 = (Polygon)mp.getGeometry(j);
        checkShellNotNested(shell, p2, graph);
        if (hasError()) {
          return;
        }
      }
    }
  }

  private void checkTooFewPoints(final GeometryGraph graph) {
    if (graph.hasTooFewPoints()) {
      validErr = new TopologyValidationError(
        TopologyValidationError.TOO_FEW_POINTS, graph.getInvalidPoint());
      return;
    }
  }

  private void checkValid(final Geometry g) {
    validErr = null;

    // empty geometries are always valid!
    if (g.isEmpty()) {
      return;
    }

    if (g instanceof Point) {
      checkValid((Point)g);
    } else if (g instanceof MultiPoint) {
      checkValid((MultiPoint)g);
    } else if (g instanceof LinearRing) {
      checkValid((LinearRing)g);
    } else if (g instanceof LineString) {
      checkValid((LineString)g);
    } else if (g instanceof Polygon) {
      checkValid((Polygon)g);
    } else if (g instanceof MultiPolygon) {
      checkValid((MultiPolygon)g);
    } else if (g instanceof GeometryCollection) {
      checkValid((GeometryCollection)g);
    } else {
      throw new UnsupportedOperationException(g.getClass().getName());
    }
  }

  private void checkValid(final GeometryCollection gc) {
    for (int i = 0; i < gc.getNumGeometries(); i++) {
      final Geometry g = gc.getGeometry(i);
      checkValid(g);
      if (hasError()) {
        return;
      }
    }
  }

  /**
   * Checks validity of a LinearRing.
   */
  private void checkValid(final LinearRing g) {
    checkInvalidCoordinates(g.getCoordinateArray());
    if (hasError()) {
      return;
    }
    checkClosedRing(g);
    if (hasError()) {
      return;
    }

    final GeometryGraph graph = new GeometryGraph(0, g);
    checkTooFewPoints(graph);
    if (hasError()) {
      return;
    }
    final LineIntersector li = new RobustLineIntersector();
    graph.computeSelfNodes(li, true);
    checkNoSelfIntersectingRings(graph);
  }

  /**
   * Checks validity of a LineString.  Almost anything goes for linestrings!
   */
  private void checkValid(final LineString g) {
    checkInvalidCoordinates(g.getCoordinateArray());
    if (hasError()) {
      return;
    }
    final GeometryGraph graph = new GeometryGraph(0, g);
    checkTooFewPoints(graph);
  }

  /**
   * Checks validity of a MultiPoint.
   */
  private void checkValid(final MultiPoint g) {
    checkInvalidCoordinates(g.getCoordinateArray());
  }

  private void checkValid(final MultiPolygon g) {
    for (int i = 0; i < g.getNumGeometries(); i++) {
      final Polygon p = (Polygon)g.getGeometry(i);
      checkInvalidCoordinates(p);
      if (hasError()) {
        return;
      }
      checkClosedRings(p);
      if (hasError()) {
        return;
      }
    }

    final GeometryGraph graph = new GeometryGraph(0, g);

    checkTooFewPoints(graph);
    if (hasError()) {
      return;
    }
    checkConsistentArea(graph);
    if (hasError()) {
      return;
    }
    if (!isSelfTouchingRingFormingHoleValid) {
      checkNoSelfIntersectingRings(graph);
      if (hasError()) {
        return;
      }
    }
    for (int i = 0; i < g.getNumGeometries(); i++) {
      final Polygon p = (Polygon)g.getGeometry(i);
      checkHolesInShell(p, graph);
      if (hasError()) {
        return;
      }
    }
    for (int i = 0; i < g.getNumGeometries(); i++) {
      final Polygon p = (Polygon)g.getGeometry(i);
      checkHolesNotNested(p, graph);
      if (hasError()) {
        return;
      }
    }
    checkShellsNotNested(g, graph);
    if (hasError()) {
      return;
    }
    checkConnectedInteriors(graph);
  }

  /**
   * Checks validity of a Point.
   */
  private void checkValid(final Point g) {
    checkInvalidCoordinates(g.getCoordinateArray());
  }

  /**
   * Checks the validity of a polygon.
   * Sets the validErr flag.
   */
  private void checkValid(final Polygon g) {
    checkInvalidCoordinates(g);
    if (hasError()) {
      return;
    }
    checkClosedRings(g);
    if (hasError()) {
      return;
    }

    final GeometryGraph graph = new GeometryGraph(0, g);

    checkTooFewPoints(graph);
    if (hasError()) {
      return;
    }
    checkConsistentArea(graph);
    if (hasError()) {
      return;
    }

    if (!isSelfTouchingRingFormingHoleValid) {
      checkNoSelfIntersectingRings(graph);
      if (hasError()) {
        return;
      }
    }
    checkHolesInShell(g, graph);
    if (hasError()) {
      return;
    }
    // SLOWcheckHolesNotNested(g);
    checkHolesNotNested(g, graph);
    if (hasError()) {
      return;
    }
    checkConnectedInteriors(graph);
  }

  /**
   * Computes the validity of the geometry,
   * and if not valid returns the validation error for the geometry,
   * or null if the geometry is valid.
   * 
   * @return the validation error, if the geometry is invalid
   * or null if the geometry is valid
   */
  public TopologyValidationError getValidationError() {
    checkValid(parentGeometry);
    return validErr;
  }

  public boolean hasError() {
    if (validErr == null) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Computes the validity of the geometry,
   * and returns <tt>true</tt> if it is valid.
   * 
   * @return true if the geometry is valid
   */
  public boolean isValid() {
    checkValid(parentGeometry);
    return validErr == null;
  }

  /**
   * Sets whether polygons using <b>Self-Touching Rings</b> to form
   * holes are reported as valid.
   * If this flag is set, the following Self-Touching conditions
   * are treated as being valid:
   * <ul>
   * <li>the shell ring self-touches to create a hole touching the shell
   * <li>a hole ring self-touches to create two holes touching at a point
   * </ul>
   * <p>
   * The default (following the OGC SFS standard)
   * is that this condition is <b>not</b> valid (<code>false</code>).
   * <p>
   * This does not affect whether Self-Touching Rings
   * disconnecting the polygon interior are considered valid
   * (these are considered to be <b>invalid</b> under the SFS, and many other
   * spatial models as well).
   * This includes "bow-tie" shells,
   * which self-touch at a single point causing the interior to
   * be disconnected,
   * and "C-shaped" holes which self-touch at a single point causing an island to be formed.
   *
   * @param isValid states whether geometry with this condition is valid
   */
  public void setSelfTouchingRingFormingHoleValid(final boolean isValid) {
    isSelfTouchingRingFormingHoleValid = isValid;
  }

  @Override
  public String toString() {
    if (validErr == null) {
      return "Valid";
    } else {
      return validErr.toString();
    }
  }

}