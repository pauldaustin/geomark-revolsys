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
package com.revolsys.jts.testold.geom;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import com.revolsys.jts.geom.Coordinate;
import com.revolsys.jts.geom.Coordinates;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.PrecisionModel;
import com.revolsys.jts.geom.Triangle;
import com.revolsys.jts.io.WKTReader;

/**
 * @version 1.7
 */
public class TriangleTest extends TestCase {

  private final PrecisionModel precisionModel = new PrecisionModel();

  private final GeometryFactory geometryFactory = new GeometryFactory(
    this.precisionModel, 0);

  WKTReader reader = new WKTReader(this.geometryFactory);

  private static final double TOLERANCE = 1E-5;

  public static void main(final String args[]) {
    TestRunner.run(TriangleTest.class);
  }

  public TriangleTest(final String name) {
    super(name);
  }

  public void checkAcute(final String wkt, final boolean expectedValue)
    throws Exception {
    final Geometry g = this.reader.read(wkt);
    final Coordinates[] pt = g.getCoordinateArray();

    final Triangle t = new Triangle(pt[0], pt[1], pt[2]);
    final boolean isAcute = t.isAcute();
    System.out.println("isAcute = " + isAcute);
    assertEquals(expectedValue, isAcute);
  }

  public void checkArea(final String wkt, final double expectedValue)
    throws Exception {
    final Geometry g = this.reader.read(wkt);
    final Coordinates[] pt = g.getCoordinateArray();

    final Triangle t = new Triangle(pt[0], pt[1], pt[2]);
    final double signedArea = t.signedArea();
    System.out.println("signed area = " + signedArea);
    assertEquals(expectedValue, signedArea, TOLERANCE);

    final double area = t.area();
    assertEquals(Math.abs(expectedValue), area, TOLERANCE);

  }

  public void checkArea3D(final String wkt, final double expectedValue)
    throws Exception {
    final Geometry g = this.reader.read(wkt);
    final Coordinates[] pt = g.getCoordinateArray();
    final Triangle t = new Triangle(pt[0], pt[1], pt[2]);
    final double area3D = t.area3D();
    // System.out.println("area3D = " + area3D);
    assertEquals(expectedValue, area3D, TOLERANCE);
  }

  public void checkCentroid(final String wkt, final Coordinates expectedValue)
    throws Exception {
    final Geometry g = this.reader.read(wkt);
    final Coordinates[] pt = g.getCoordinateArray();

    Coordinates centroid = Triangle.centroid(pt[0], pt[1], pt[2]);
    System.out.println("(Static) centroid = " + centroid);
    assertEquals(expectedValue.toString(), centroid.toString());

    // Test Instance version
    //
    final Triangle t = new Triangle(pt[0], pt[1], pt[2]);
    centroid = t.centroid();
    System.out.println("(Instance) centroid = " + centroid.toString());
    assertEquals(expectedValue.toString(), centroid.toString());
  }

  public void checkCircumCentre(final String wkt, final Coordinates expectedValue)
    throws Exception {
    final Geometry g = this.reader.read(wkt);
    final Coordinates[] pt = g.getCoordinateArray();

    Coordinates circumcentre = Triangle.circumcentre(pt[0], pt[1], pt[2]);
    System.out.println("(Static) circumcentre = " + circumcentre);
    assertEquals(expectedValue.toString(), circumcentre.toString());

    // Test Instance version
    //
    final Triangle t = new Triangle(pt[0], pt[1], pt[2]);
    circumcentre = t.circumcentre();
    System.out.println("(Instance) circumcentre = " + circumcentre.toString());
    assertEquals(expectedValue.toString(), circumcentre.toString());
  }

  public void checkInterpolateZ(final String wkt, final Coordinates p,
    final double expectedValue) throws Exception {
    final Geometry g = this.reader.read(wkt);
    final Coordinates[] pt = g.getCoordinateArray();

    final Triangle t = new Triangle(pt[0], pt[1], pt[2]);
    final double z = t.interpolateZ(p);
    System.out.println("Z = " + z);
    assertEquals(expectedValue, z, 0.000001);
  }

  public void checkLongestSideLength(final String wkt,
    final double expectedValue) throws Exception {
    final Geometry g = this.reader.read(wkt);
    final Coordinates[] pt = g.getCoordinateArray();

    double length = Triangle.longestSideLength(pt[0], pt[1], pt[2]);
    System.out.println("(Static) longestSideLength = " + length);
    assertEquals(expectedValue, length, 0.00000001);

    // Test Instance version
    //
    final Triangle t = new Triangle(pt[0], pt[1], pt[2]);
    length = t.longestSideLength();
    System.out.println("(Instance) longestSideLength = " + length);
    assertEquals(expectedValue, length, 0.00000001);
  }

  public void testAcute() throws Exception {
    // right triangle
    checkAcute("POLYGON((10 10, 20 20, 20 10, 10 10))", false);
    // CCW right tri
    checkAcute("POLYGON((10 10, 20 10, 20 20, 10 10))", false);
    // acute
    checkAcute("POLYGON((10 10, 20 10, 15 20, 10 10))", true);
  }

  public void testArea() throws Exception {
    // CW
    checkArea("POLYGON((10 10, 20 20, 20 10, 10 10))", 50);
    // CCW
    checkArea("POLYGON((10 10, 20 10, 20 20, 10 10))", -50);
    // degenerate point triangle
    checkArea("POLYGON((10 10, 10 10, 10 10, 10 10))", 0);
    // degenerate line triangle
    checkArea("POLYGON((10 10, 20 10, 15 10, 10 10))", 0);
  }

  public void testArea3D() throws Exception {
    checkArea3D("POLYGON((0 0 10, 100 0 110, 100 100 110, 0 0 10))",
      7071.067811865475);
    checkArea3D("POLYGON((0 0 10, 100 0 10, 50 100 110, 0 0 10))",
      7071.067811865475);
  }

  public void testCentroid() throws Exception {
    // right triangle
    checkCentroid("POLYGON((10 10, 20 20, 20 10, 10 10))", new Coordinate(
      (10.0 + 20.0 + 20.0) / 3.0, (10.0 + 20.0 + 10.0) / 3.0,
      Coordinates.NULL_ORDINATE));
    // CCW right tri
    checkCentroid("POLYGON((10 10, 20 10, 20 20, 10 10))", new Coordinate(
      (10.0 + 20.0 + 20.0) / 3.0, (10.0 + 10.0 + 20.0) / 3.0,
      Coordinates.NULL_ORDINATE));
    // acute
    checkCentroid("POLYGON((10 10, 20 10, 15 20, 10 10))", new Coordinate(
      (10.0 + 20.0 + 15.0) / 3.0, (10.0 + 10.0 + 20.0) / 3.0,
      Coordinates.NULL_ORDINATE));
  }

  public void testCircumCentre() throws Exception {
    // right triangle
    checkCircumCentre("POLYGON((10 10, 20 20, 20 10, 10 10))", new Coordinate(
      15.0, 15.0, Coordinates.NULL_ORDINATE));
    // CCW right tri
    checkCircumCentre("POLYGON((10 10, 20 10, 20 20, 10 10))", new Coordinate(
      15.0, 15.0, Coordinates.NULL_ORDINATE));
    // acute
    checkCircumCentre("POLYGON((10 10, 20 10, 15 20, 10 10))", new Coordinate(
      15.0, 13.75, Coordinates.NULL_ORDINATE));
  }

  public void testInterpolateZ() throws Exception {
    checkInterpolateZ("LINESTRING(1 1 0, 2 1 0, 1 2 10)", new Coordinate(1.5,
      1.5, Coordinates.NULL_ORDINATE), 5);
    checkInterpolateZ("LINESTRING(1 1 0, 2 1 0, 1 2 10)", new Coordinate(1.2,
      1.2, Coordinates.NULL_ORDINATE), 2);
    checkInterpolateZ("LINESTRING(1 1 0, 2 1 0, 1 2 10)", new Coordinate(0.0,
      0, Coordinates.NULL_ORDINATE), -10);
  }

  public void testLongestSideLength() throws Exception {
    // right triangle
    checkLongestSideLength("POLYGON((10 10 1, 20 20 2, 20 10 3, 10 10 1))",
      14.142135623730951);
    // CCW right tri
    checkLongestSideLength("POLYGON((10 10 1, 20 10 2, 20 20 3, 10 10 1))",
      14.142135623730951);
    // acute
    checkLongestSideLength("POLYGON((10 10 1, 20 10 2, 15 20 3, 10 10 1))",
      11.180339887498949);
  }

}