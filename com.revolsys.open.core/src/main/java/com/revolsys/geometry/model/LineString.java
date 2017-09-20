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
package com.revolsys.geometry.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.revolsys.datatype.DataType;
import com.revolsys.datatype.DataTypes;
import com.revolsys.geometry.algorithm.LineIntersector;
import com.revolsys.geometry.algorithm.LineStringLocation;
import com.revolsys.geometry.algorithm.RayCrossingCounter;
import com.revolsys.geometry.algorithm.RobustLineIntersector;
import com.revolsys.geometry.cs.CoordinateSystem;
import com.revolsys.geometry.cs.GeographicCoordinateSystem;
import com.revolsys.geometry.cs.ProjectedCoordinateSystem;
import com.revolsys.geometry.cs.projection.CoordinatesOperation;
import com.revolsys.geometry.graph.linemerge.LineMerger;
import com.revolsys.geometry.model.coordinates.CoordinatesUtil;
import com.revolsys.geometry.model.coordinates.LineSegmentUtil;
import com.revolsys.geometry.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.geometry.model.editor.LineStringEditor;
import com.revolsys.geometry.model.metrics.PointLineStringMetrics;
import com.revolsys.geometry.model.prep.PreparedLineString;
import com.revolsys.geometry.model.segment.LineSegmentDouble;
import com.revolsys.geometry.model.segment.LineStringSegment;
import com.revolsys.geometry.model.segment.Segment;
import com.revolsys.geometry.model.vertex.AbstractVertex;
import com.revolsys.geometry.model.vertex.LineStringVertex;
import com.revolsys.geometry.model.vertex.Vertex;
import com.revolsys.geometry.operation.BoundaryOp;
import com.revolsys.geometry.util.BoundingBoxUtil;
import com.revolsys.util.MathUtil;
import com.revolsys.util.Pair;
import com.revolsys.util.Property;
import com.revolsys.util.number.Doubles;

/**
 * Models an OGC-style <code>LineString</code>. A LineString consists of a
 * sequence of two or more vertices, along with all points along the
 * linearly-interpolated curves (line segments) between each pair of consecutive
 * vertices. Consecutive vertices may be equal. The line segments in the line
 * may intersect each other (in other words, the linestring may "curl back" in
 * itself and self-intersect. Linestrings with exactly two identical points are
 * invalid.
 * <p>
 * A linestring must have either 0 or 2 or more points. If these conditions are
 * not met, the constructors throw an {@link IllegalArgumentException}
 *
 * @version 1.7
 */
public interface LineString extends Lineal {
  @SuppressWarnings("unchecked")
  static <G extends Geometry> G newLineString(final Object value) {
    if (value == null) {
      return null;
    } else if (value instanceof LineString) {
      return (G)value;
    } else if (value instanceof Geometry) {
      throw new IllegalArgumentException(
        ((Geometry)value).getGeometryType() + " cannot be converted to a LineString");
    } else {
      final String string = DataTypes.toString(value);
      return (G)GeometryFactory.DEFAULT_3D.geometry(string, false);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  default <V extends Geometry> V appendVertex(Point newPoint, final int... geometryId) {
    if (geometryId.length == 0) {
      final GeometryFactory geometryFactory = getGeometryFactory();
      if (newPoint == null || newPoint.isEmpty()) {
        return (V)this;
      } else if (isEmpty()) {
        return newPoint.convertGeometry(geometryFactory);
      } else {
        newPoint = newPoint.convertGeometry(geometryFactory);
        final int vertexCount = getVertexCount();
        final double[] coordinates = getCoordinates();
        final int axisCount = getAxisCount();
        final double[] newCoordinates = new double[axisCount * (vertexCount + 1)];

        final int length = vertexCount * axisCount;
        System.arraycopy(coordinates, 0, newCoordinates, 0, length);
        CoordinatesListUtil.setCoordinates(newCoordinates, axisCount, vertexCount, newPoint);

        return (V)newLineString(newCoordinates);
      }
    } else {
      throw new IllegalArgumentException("Geometry id's for " + getGeometryType()
        + " must have length 0. " + Arrays.toString(geometryId));
    }
  }

  @Override
  default Lineal applyLineal(final Function<LineString, LineString> function) {
    final LineString newLine = function.apply(this);
    return newLine;
  }

  default LineString cleanCloseVertices(final double maxDistance) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    final int axisCount = geometryFactory.getAxisCount();
    final int vertexCount = getVertexCount();
    final LineStringEditor newLine = new LineStringEditor(geometryFactory, vertexCount);
    double x1 = getX(0);
    double y1 = getY(0);
    newLine.appendVertex(x1, y1);
    for (int axisIndex = 2; axisIndex < axisCount; axisIndex++) {
      final double coordinate = getCoordinate(0, axisIndex);
      newLine.setCoordinate(0, axisIndex, coordinate);
    }
    final int lastVertexIndex = getLastVertexIndex();
    for (int vertexIndex = 1; vertexIndex < lastVertexIndex; vertexIndex++) {
      final double x2 = getX(vertexIndex);
      final double y2 = getY(vertexIndex);
      if (MathUtil.distance(x1, y1, x2, y2) < maxDistance) {
        // Skip vertex
      } else {
        newLine.appendVertex(x2, y2);
        final int newVertexIndex = newLine.getLastVertexIndex();
        for (int axisIndex = 2; axisIndex < axisCount; axisIndex++) {
          final double coordinate = getCoordinate(newVertexIndex, axisIndex);
          newLine.setCoordinate(newVertexIndex, axisIndex, coordinate);
        }
        x1 = x2;
        y1 = y2;
      }
    }
    final double xn = getX(lastVertexIndex);
    final double yn = getY(lastVertexIndex);
    if (lastVertexIndex > 2 && MathUtil.distance(x1, y1, xn, yn) < maxDistance) {
      final int newVertexIndex = newLine.getLastVertexIndex();
      for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
        final double coordinate = getCoordinate(lastVertexIndex, axisIndex);
        newLine.setCoordinate(newVertexIndex, axisIndex, coordinate);
      }
    } else {
      newLine.appendVertex(xn, yn);
      final int newVertexIndex = newLine.getLastVertexIndex();
      for (int axisIndex = 2; axisIndex < axisCount; axisIndex++) {
        final double coordinate = getCoordinate(lastVertexIndex, axisIndex);
        newLine.setCoordinate(newVertexIndex, axisIndex, coordinate);
      }
    }
    if (newLine.getVertexCount() == 1) {
      return this;
    } else {
      final LineString newLineString = newLine.newLineString();
      return newLineString;
    }
  }

  @Override
  LineString clone();

  @Override
  default int compareToSameClass(final Geometry geometry) {
    final LineString line2 = (LineString)geometry;
    final Iterator<Vertex> iterator1 = vertices().iterator();
    final Iterator<Vertex> iterator2 = line2.vertices().iterator();
    while (iterator1.hasNext() && iterator2.hasNext()) {
      final Point vertex1 = iterator1.next();
      final Point vertex2 = iterator2.next();
      final int comparison = vertex1.compareTo(vertex2);
      if (comparison != 0) {
        return comparison;
      }
    }
    if (iterator1.hasNext()) {
      return 1;
    } else if (iterator2.hasNext()) {
      return -1;
    } else {
      return 0;
    }
  }

  default int compareVertex(final int vertexIndex1, final int vertexIndex2) {
    final double x1 = getX(vertexIndex1);
    final double y1 = getY(vertexIndex1);
    final double x2 = getX(vertexIndex2);
    final double y2 = getY(vertexIndex2);
    return CoordinatesUtil.compare(x1, y1, x2, y2);
  }

  default int compareVertex(final int vertexIndex1, final LineString line2,
    final int vertexIndex2) {
    final double x1 = getX(vertexIndex1);
    final double y1 = getY(vertexIndex1);
    final double x2 = line2.getX(vertexIndex2);
    final double y2 = line2.getY(vertexIndex2);
    return CoordinatesUtil.compare(x1, y1, x2, y2);
  }

  default double[] convertCoordinates(GeometryFactory geometryFactory) {
    final GeometryFactory sourceGeometryFactory = getGeometryFactory();
    final double[] coordinates = getCoordinates();
    if (isEmpty()) {
      return coordinates;
    } else {
      geometryFactory = getNonZeroGeometryFactory(geometryFactory);
      double[] targetCoordinates;
      final CoordinatesOperation coordinatesOperation = sourceGeometryFactory
        .getCoordinatesOperation(geometryFactory);
      if (coordinatesOperation == null) {
        return coordinates;
      } else {
        final int sourceAxisCount = getAxisCount();
        targetCoordinates = new double[sourceAxisCount * getVertexCount()];
        coordinatesOperation.perform(sourceAxisCount, coordinates, sourceAxisCount,
          targetCoordinates);
        return targetCoordinates;
      }
    }
  }

  default void convertVertexCoordinates2d(final int vertexIndex,
    final GeometryFactory geometryFactory, final double[] targetCoordinates) {
    final double x = getX(vertexIndex);
    final double y = getY(vertexIndex);
    targetCoordinates[X] = x;
    targetCoordinates[Y] = y;
    final CoordinatesOperation coordinatesOperation = getGeometryFactory()
      .getCoordinatesOperation(geometryFactory);
    if (coordinatesOperation != null) {
      coordinatesOperation.perform(2, targetCoordinates, 2, targetCoordinates);
    }
  }

  default int copyCoordinates(final int axisCount, final double nanValue,
    final double[] destCoordinates, int destOffset) {
    if (isEmpty()) {
      return destOffset;
    } else {
      for (int vertexIndex = 0; vertexIndex < getVertexCount(); vertexIndex++) {
        for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
          double coordinate = getCoordinate(vertexIndex, axisIndex);
          if (Double.isNaN(coordinate)) {
            coordinate = nanValue;
          }
          destCoordinates[destOffset++] = coordinate;
        }
      }
      return destOffset;
    }
  }

  default int copyCoordinatesReverse(final int axisCount, final double nanValue,
    final double[] destCoordinates, int destOffset) {
    if (isEmpty()) {
      return destOffset;
    } else {
      for (int vertexIndex = getVertexCount() - 1; vertexIndex >= 0; vertexIndex--) {
        for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
          double coordinate = getCoordinate(vertexIndex, axisIndex);
          if (Double.isNaN(coordinate)) {
            coordinate = nanValue;
          }
          destCoordinates[destOffset++] = coordinate;
        }
      }
      return destOffset;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  default <V extends Geometry> V deleteVertex(final int... vertexId) {
    if (vertexId.length == 1) {
      final int vertexIndex = vertexId[0];
      return (V)deleteVertex(vertexIndex);
    } else {
      throw new IllegalArgumentException("Vertex id's for " + getGeometryType()
        + " must have length 1. " + Arrays.toString(vertexId));
    }
  }

  default LineString deleteVertex(final int vertexIndex) {
    if (isEmpty()) {
      throw new IllegalArgumentException("Cannot delete vertex for empty LineString");
    } else {
      final int vertexCount = getVertexCount();
      if (vertexCount <= 2) {
        throw new IllegalArgumentException("LineString must have a minimum of 2 vertices");
      } else if (vertexIndex >= 0 && vertexIndex < vertexCount) {
        final double[] coordinates = getCoordinates();
        final int axisCount = getAxisCount();
        final double[] newCoordinates = new double[axisCount * (vertexCount - 1)];
        final int beforeLength = vertexIndex * axisCount;
        if (vertexIndex > 0) {
          System.arraycopy(coordinates, 0, newCoordinates, 0, beforeLength);
        }
        final int sourceIndex = (vertexIndex + 1) * axisCount;
        final int length = (vertexCount - vertexIndex - 1) * axisCount;
        System.arraycopy(coordinates, sourceIndex, newCoordinates, beforeLength, length);

        return newLineString(newCoordinates);
      } else {
        throw new IllegalArgumentException("Vertex index must be between 0 and " + vertexCount);
      }
    }
  }

  @Override
  default double distance(final double x, final double y) {
    return distance(x, y, 0);
  }

  @Override
  default double distance(final double x, final double y, final double terminateDistance) {
    if (isEmpty()) {
      return Double.POSITIVE_INFINITY;
    } else {
      double minDistance = Double.POSITIVE_INFINITY;
      double x1 = getX(0);
      double y1 = getY(0);
      final int vertexCount = getVertexCount();
      for (int vertexIndex = 1; vertexIndex < vertexCount; vertexIndex++) {
        final double x2 = getX(vertexIndex);
        final double y2 = getY(vertexIndex);
        final double distance = LineSegmentUtil.distanceLinePoint(x1, y1, x2, y2, x, y);
        if (distance < minDistance) {
          minDistance = distance;
          if (minDistance == terminateDistance) {
            return minDistance;
          }
        }
        x1 = x2;
        y1 = y2;
      }
      return minDistance;
    }
  }

  @Override
  default double distance(final Geometry geometry, final double terminateDistance) {
    if (geometry instanceof Point) {
      final Point point = (Point)geometry;
      return distance(point, terminateDistance);
    } else if (geometry instanceof LineString) {
      final LineString line = (LineString)geometry;
      return distance(line, terminateDistance);
    } else {
      return geometry.distance(this, terminateDistance);
    }
  }

  default double distance(final LineString line, final double terminateDistance) {
    if (isEmpty()) {
      return Double.POSITIVE_INFINITY;
    } else if (Property.isEmpty(line)) {
      return Double.POSITIVE_INFINITY;
    } else {
      final GeometryFactory geometryFactory = getGeometryFactory();
      double minDistance = Double.POSITIVE_INFINITY;
      final int vertexCount1 = getVertexCount();
      final int vertexCount2 = line.getVertexCount();
      final CoordinatesOperation coordinatesOperation = getGeometryFactory()
        .getCoordinatesOperation(geometryFactory);
      double line2x1 = line.getX(0);
      double line2y1 = line.getY(0);
      double[] coordinates = null;
      if (coordinatesOperation != null) {
        coordinates = new double[] {
          line2x1, line2y1
        };
        coordinatesOperation.perform(2, coordinates, 2, coordinates);
        line2x1 = coordinates[X];
        line2y1 = coordinates[Y];
      }
      for (int vertexIndex2 = 1; vertexIndex2 < vertexCount2; vertexIndex2++) {
        double line2x2 = line.getX(vertexIndex2);
        double line2y2 = line.getY(vertexIndex2);
        if (coordinatesOperation != null) {
          coordinates[X] = line2x2;
          coordinates[Y] = line2y2;
          coordinatesOperation.perform(2, coordinates, 2, coordinates);
          line2x2 = coordinates[X];
          line2y2 = coordinates[Y];
        }
        double line1x1 = getX(0);
        double line1y1 = getY(0);
        for (int vertexIndex1 = 1; vertexIndex1 < vertexCount1; vertexIndex1++) {
          final double line1x2 = getX(vertexIndex1);
          final double line1y2 = getY(vertexIndex1);

          final double distance = LineSegmentUtil.distanceLineLine(line1x1, line1y1, line1x2,
            line1y2, line2x1, line2y1, line2x2, line2y2);
          if (distance < minDistance) {
            minDistance = distance;
            if (minDistance <= terminateDistance) {
              return minDistance;
            }
          }
          line1x1 = line1x2;
          line1y1 = line1y2;
        }
        line2x1 = line2x2;
        line2y1 = line2y2;
      }
      return minDistance;
    }
  }

  default double distanceAlong(final Point point) {
    if (isEmpty() && point.isEmpty()) {
      return Double.MAX_VALUE;
    } else {
      double distanceAlongSegments = 0;
      double closestDistance = Double.MAX_VALUE;
      double distanceAlong = 0;
      final GeometryFactory geometryFactory = getGeometryFactory();
      final double x = point.getX();
      final double y = point.getY();
      final double resolutionXy = geometryFactory.getResolutionX();
      final int vertexCount = getVertexCount();
      if (vertexCount > 0) {
        double x1 = getX(0);
        double y1 = getY(0);
        for (int vertexIndex = 1; vertexIndex < vertexCount; vertexIndex++) {
          final double x2 = getX(vertexIndex);
          final double y2 = getY(vertexIndex);
          if (x1 == x && y1 == y) {
            return distanceAlongSegments;
          } else {
            final double segmentLength = MathUtil.distance(x1, y1, x2, y2);
            final double distance = LineSegmentUtil.distanceLinePoint(x1, y1, x2, y2, x, y);
            final double projectionFactor = LineSegmentUtil.projectionFactor(x1, y1, x2, y2, x, y);
            if (distance < resolutionXy) {
              return distanceAlongSegments + MathUtil.distance(x1, y1, x, y);
            } else if (distance < closestDistance) {
              closestDistance = distance;
              if (projectionFactor == 0) {
                distanceAlong = distanceAlongSegments;
              } else if (projectionFactor < 0) {
                if (vertexIndex == 1) {
                  distanceAlong = segmentLength * projectionFactor;
                } else {
                  distanceAlong = distanceAlongSegments;
                }
              } else if (projectionFactor >= 1) {
                if (vertexIndex == vertexCount - 1) {
                  distanceAlong = distanceAlongSegments + segmentLength * projectionFactor;
                } else {
                  distanceAlong = distanceAlongSegments + segmentLength;
                }
              } else {
                distanceAlong = distanceAlongSegments + segmentLength * projectionFactor;
              }
            }
            distanceAlongSegments += segmentLength;
          }

          x1 = x2;
          y1 = y2;
        }
      }
      return distanceAlong;
    }
  }

  default double distanceVertex(final int index, final double x, final double y) {
    final double x1 = getX(index);
    final double y1 = getY(index);
    return MathUtil.distance(x1, y1, x, y);
  }

  default double distanceVertex(final int index, final Point point) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    final Point convertedPoint = point.convertPoint2d(geometryFactory);
    final double x = convertedPoint.getX();
    final double y = convertedPoint.getY();
    return distanceVertex(index, x, y);
  }

  @Override
  default boolean equals(final int axisCount, final Geometry geometry) {
    if (geometry == this) {
      return true;
    } else if (geometry == null) {
      return false;
    } else if (axisCount < 2) {
      throw new IllegalArgumentException("Axis Count must be >=2");
    } else if (isEquivalentClass(geometry)) {
      if (isEmpty()) {
        return geometry.isEmpty();
      } else if (geometry.isEmpty()) {
        return false;
      } else {
        final LineString line = (LineString)geometry;
        final int vertexCount = getVertexCount();
        final int vertexCount2 = line.getVertexCount();
        if (vertexCount == vertexCount2) {
          for (int i = 0; i < vertexCount2; i++) {
            for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
              final double value1 = getCoordinate(i, axisIndex);
              final double value2 = line.getCoordinate(i, axisIndex);
              if (!Doubles.equal(value1, value2)) {
                return false;
              }
            }
          }
          return true;
        }
      }
    }
    return false;
  }

  default boolean equals(final int axisCount, final int vertexIndex, final Point point) {
    for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
      final double value = getCoordinate(vertexIndex, axisIndex);
      final double value2 = point.getCoordinate(axisIndex);
      if (!Doubles.equal(value, value2)) {
        return false;
      }
    }
    return true;
  }

  @Override
  default boolean equalsExact(final Geometry other, final double tolerance) {
    if (!isEquivalentClass(other)) {
      return false;
    }
    final LineString otherLineString = (LineString)other;
    if (getVertexCount() != otherLineString.getVertexCount()) {
      return false;
    }
    for (int i = 0; i < getVertexCount(); i++) {
      final Point point = getPoint(i);
      final Point otherPoint = otherLineString.getPoint(i);
      if (!equal(point, otherPoint, tolerance)) {
        return false;
      }
    }
    return true;
  }

  default boolean equalsVertex(final int vertexIndex, final double... coordinates) {
    if (isEmpty() || coordinates == null || coordinates.length < 2) {
      return false;
    } else {
      for (int axisIndex = 0; axisIndex < coordinates.length; axisIndex++) {
        final double coordinate = coordinates[axisIndex];
        final double matchCoordinate = getCoordinate(vertexIndex, axisIndex);
        if (!Doubles.equal(coordinate, matchCoordinate)) {
          return false;
        }
      }
      return true;
    }
  }

  default boolean equalsVertex(final int vertexIndex, final double x, final double y) {
    final double x1 = getX(vertexIndex);
    if (Double.compare(x, x1) == 0) {
      final double y1 = getY(vertexIndex);
      if (Double.compare(y, y1) == 0) {
        return true;
      }
    }
    return false;
  }

  default boolean equalsVertex(final int axisCount, final int vertexIndex1,
    final int vertexIndex2) {
    if (isEmpty()) {
      return false;
    } else {
      for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
        final double coordinate1 = getCoordinate(vertexIndex1, axisIndex);
        final double coordinate2 = getCoordinate(vertexIndex2, axisIndex);
        if (!Doubles.equal(coordinate1, coordinate2)) {
          return false;
        }
      }
      return true;
    }
  }

  default boolean equalsVertex(final int axisCount, final int vertexIndex, final LineString line2,
    final int vertexIndex2) {
    if (Property.isEmpty(line2)) {
      return false;
    } else {
      final Vertex vertex2 = line2.getVertex(vertexIndex2);
      return equalsVertex(axisCount, vertexIndex, vertex2);
    }
  }

  default boolean equalsVertex(final int axisCount, final int vertexIndex, Point point) {
    if (isEmpty() || Property.isEmpty(point)) {
      return false;
    } else {
      final GeometryFactory geometryFactory = getGeometryFactory();
      point = point.convertGeometry(geometryFactory, axisCount);
      for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
        final double coordinate = point.getCoordinate(axisIndex);
        final double matchCoordinate = getCoordinate(vertexIndex, axisIndex);
        if (!Doubles.equal(coordinate, matchCoordinate)) {
          return false;
        }
      }
      return true;
    }
  }

  default boolean equalsVertex(final int vertexIndex, final Point point) {
    if (Property.isEmpty(point)) {
      return false;
    } else {
      final int axisCount = point.getAxisCount();
      return equalsVertex(axisCount, vertexIndex, point);
    }
  }

  default boolean equalsVertex2d(final int vertexIndex, final double x, final double y) {
    final double x1 = getX(vertexIndex);
    if (x1 == x) {
      final double y1 = getY(vertexIndex);
      if (y1 == y) {
        return true;
      }
    }
    return false;
  }

  default boolean equalsVertex2d(final int vertexIndex1, final int vertexIndex2) {
    final double x1 = getX(vertexIndex1);
    final double x2 = getX(vertexIndex2);
    if (x1 == x2) {
      final double y1 = getY(vertexIndex1);
      final double y2 = getY(vertexIndex2);
      if (y1 == y2) {
        return true;
      }
    }
    return false;
  }

  @Override
  default Pair<GeometryComponent, Double> findClosestGeometryComponent(final double x,
    final double y) {
    if (isEmpty()) {
      return new Pair<>();
    } else {
      final GeometryFactory geometryFactory = getGeometryFactory();
      boolean closestIsVertex = false;
      int closestIndex = 0;
      double x1 = getX(0);
      double y1 = getY(0);
      if (x == x1 && y == y1) {
        final AbstractVertex closestVertex = getVertex(0);
        return new Pair<>(closestVertex, 0.0);
      } else {
        double closestDistance = geometryFactory.makePrecise(0, MathUtil.distance(x, y, x1, y1));

        final int vertexCount = getVertexCount();
        for (int vertexIndex = 1; vertexIndex < vertexCount; vertexIndex++) {
          final double x2 = getX(vertexIndex);
          final double y2 = getY(vertexIndex);
          if (x == x2 && y == y2) {
            final AbstractVertex closestVertex = getVertex(vertexIndex);
            return new Pair<>(closestVertex, 0.0);
          } else {
            final double toDistance = geometryFactory.makePrecise(0,
              MathUtil.distance(x, y, x2, y2));
            if (toDistance <= closestDistance) {
              if (!closestIsVertex || toDistance < closestDistance) {
                closestIndex = vertexIndex;
                closestIsVertex = true;
                closestDistance = toDistance;
              }
            }
            final double segmentDistance = geometryFactory.makePrecise(0,
              LineSegmentUtil.distanceLinePoint(x1, y1, x2, y2, x, y));
            if (segmentDistance == 0) {
              final Segment closestSegment = getSegment(vertexIndex - 1);
              return new Pair<>(closestSegment, 0.0);
            } else if (segmentDistance < closestDistance) {
              closestIsVertex = false;
              closestIndex = vertexIndex - 1;
              closestDistance = segmentDistance;
            }
          }
          x1 = x2;
          y1 = y2;
        }
        if (closestIsVertex) {
          final Vertex closestVertex = getVertex(closestIndex);
          return new Pair<>(closestVertex, closestDistance);
        } else {
          final Segment closestSegment = getSegment(closestIndex);
          return new Pair<>(closestSegment, closestDistance);
        }
      }
    }
  }

  @Override
  default Pair<GeometryComponent, Double> findClosestGeometryComponent(Point point) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    point = point.convertPoint2d(geometryFactory);
    return findClosestGeometryComponent(point.getX(), point.getY());
  }

  /**
   * Gets the boundary of this geometry. The boundary of a lineal geometry is
   * always a zero-dimensional geometry (which may be empty).
   *
   * @return the boundary geometry
   * @see Geometry#getBoundary
   */
  @Override
  default Geometry getBoundary() {
    return new BoundaryOp(this).getBoundary();
  }

  @Override
  default int getBoundaryDimension() {
    if (isClosed()) {
      return Dimension.FALSE;
    }
    return 0;
  }

  default ClockDirection getClockDirection() {
    final int pointCount = getVertexCount() - 1;

    // find highest point
    double hiPtX = getX(0);
    double hiPtY = getY(0);
    int hiIndex = 0;
    for (int i = 1; i <= pointCount; i++) {
      final double x = getX(i);
      final double y = getY(i);
      if (y > hiPtY) {
        hiPtX = x;
        hiPtY = y;
        hiIndex = i;
      }
    }

    // find distinct point before highest point
    int iPrev = hiIndex;
    do {
      iPrev = iPrev - 1;
      if (iPrev < 0) {
        iPrev = pointCount;
      }
    } while (equalsVertex(iPrev, hiPtX, hiPtY) && iPrev != hiIndex);

    // find distinct point after highest point
    int iNext = hiIndex;
    do {
      iNext = (iNext + 1) % pointCount;
    } while (equalsVertex(iNext, hiPtX, hiPtY) && iNext != hiIndex);

    /**
     * This check catches cases where the ring contains an A-B-A configuration
     * of points. This can happen if the ring does not contain 3 distinct points
     * (including the case where the input array has fewer than 4 elements), or
     * it contains coincident line segments.
     */
    if (equalsVertex(iPrev, hiPtX, hiPtY) || equalsVertex(iNext, hiPtX, hiPtY)
      || equalsVertex2d(iPrev, iNext)) {
      return ClockDirection.CLOCKWISE;
    }

    final int disc = orientationIndex(iPrev, hiIndex, iNext);

    /**
     * If disc is exactly 0, lines are collinear. There are two possible cases:
     * (1) the lines lie along the x axis in opposite directions (2) the lines
     * lie on top of one another (1) is handled by checking if next is left of
     * prev ==> CCW (2) will never happen if the ring is valid, so don't check
     * for it (Might want to assert this)
     */
    boolean counterClockwise = false;
    if (disc == 0) {
      // poly is CCW if prev x is right of next x
      final double prevX = getX(iPrev);
      final double nextX = getX(iNext);
      counterClockwise = prevX > nextX;
    } else {
      // if area is positive, points are ordered CCW
      counterClockwise = disc > 0;
    }
    if (counterClockwise) {
      return ClockDirection.COUNTER_CLOCKWISE;
    } else {
      return ClockDirection.CLOCKWISE;
    }
  }

  double getCoordinate(int vertexIndex, final int axisIndex);

  @Override
  default double getCoordinate(final int partIndex, final int vertexIndex, final int axisIndex) {
    if (partIndex == 0) {
      return getCoordinate(vertexIndex, axisIndex);
    } else {
      return Double.NaN;
    }
  }

  default double getCoordinateFast(final int vertexIndex, final int axisIndex) {
    return getCoordinate(vertexIndex, axisIndex);
  }

  double[] getCoordinates();

  default double[] getCoordinates(final int axisCount) {
    if (axisCount == getAxisCount()) {
      return getCoordinates();
    } else if (isEmpty()) {
      return new double[0];
    } else {
      final int vertexCount = getVertexCount();
      final double[] coordinates = new double[axisCount * vertexCount];
      int i = 0;
      for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
        for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
          final double coordinate = getCoordinate(vertexIndex, axisIndex);
          coordinates[i++] = coordinate;
        }
      }
      return coordinates;
    }
  }

  @Override
  default DataType getDataType() {
    return DataTypes.LINE_STRING;
  }

  @Override
  default int getDimension() {
    return 1;
  }

  default Point getFromPoint() {
    if (isEmpty()) {
      return null;
    } else {
      return getPoint(0);
    }
  }

  default int getLastVertexIndex() {
    return getVertexCount() - 1;
  }

  /**
   * Returns the length of this <code>LineString</code>
   *
   * @return the length of the linestring
   */
  @Override
  default double getLength() {
    final int vertexCount = getVertexCount();
    if (vertexCount <= 1) {
      return 0.0;
    } else {
      double len = 0.0;
      double x0 = getX(0);
      double y0 = getY(0);
      for (int i = 1; i < vertexCount; i++) {
        final double x1 = getX(i);
        final double y1 = getY(i);
        final double dx = x1 - x0;
        final double dy = y1 - y0;
        len += Math.sqrt(dx * dx + dy * dy);
        x0 = x1;
        y0 = y1;
      }
      return len;
    }
  }

  @Override
  default double getLength(final Unit<Length> unit) {
    double length = 0;
    final CoordinateSystem coordinateSystem = getCoordinateSystem();
    if (coordinateSystem instanceof GeographicCoordinateSystem) {
      final int vertexCount = getVertexCount();
      if (vertexCount > 1) {
        double lon0 = getX(0);
        double lat0 = getY(0);
        for (int i = 1; i < vertexCount; i++) {
          final double lon1 = getX(i);
          final double lat1 = getY(i);
          length += GeographicCoordinateSystem.distanceMetres(lon0, lat0, lon1, lat1);
          lon0 = lon1;
          lat0 = lat1;
        }
      }
      final Measure<Length> lengthMeasure = Measure.valueOf(length, SI.METRE);
      length = lengthMeasure.doubleValue(unit);
    } else if (coordinateSystem instanceof ProjectedCoordinateSystem) {
      final ProjectedCoordinateSystem projectedCoordinateSystem = (ProjectedCoordinateSystem)coordinateSystem;
      final Unit<Length> lengthUnit = projectedCoordinateSystem.getLengthUnit();

      length = getLength();
      final Measure<Length> lengthMeasure = Measure.valueOf(length, lengthUnit);
      length = lengthMeasure.doubleValue(unit);
    } else {
      length = getLength();
    }
    return length;
  }

  @Override
  default LineString getLineString(final int partIndex) {
    if (partIndex == 0) {
      return this;
    } else {
      return null;
    }
  }

  /**
   * Get the
   * @author Paul Austin <paul.austin@revolsys.com>
   * @param x
   * @param y
   * @param maxDistance
   * @return
   */
  default LineStringLocation getLineStringLocation(final double x, final double y) {
    double minDistance = Double.POSITIVE_INFINITY;
    int minSegmentIndex = 0;
    double minFraction = -1.0;

    double x1 = getX(0);
    double y1 = getY(0);
    if (x1 == x && y1 == y) {
      minDistance = 0;
      minFraction = 0;
    } else {
      final int vertexCount = getVertexCount();
      for (int vertexIndex = 1; vertexIndex < vertexCount && minDistance > 0; vertexIndex++) {
        final double x2 = getX(vertexIndex);
        final double y2 = getY(vertexIndex);
        if (x2 == x && y2 == y) {
          minSegmentIndex = vertexIndex - 1;
          minDistance = 0;
          minFraction = 1.0;
        } else {
          final double segmentDistance = LineSegmentUtil.distanceLinePoint(x1, y1, x2, y2, x, y);
          if (segmentDistance < minDistance) {
            minSegmentIndex = vertexIndex - 1;
            minFraction = LineSegmentUtil.segmentFractionOnLine(x1, y1, x2, y2, x, y);
            minDistance = segmentDistance;
          }
          x1 = x2;
          y1 = y2;
        }
      }
    }
    if (minFraction >= 0) {
      return new LineStringLocation(this, minSegmentIndex, minFraction, minDistance);
    } else {
      return null;
    }
  }

  default double getM(final int vertexIndex) {
    return getCoordinate(vertexIndex, 3);
  }

  /**
   * Computes the Maximal Nearest Subline of a given linestring relative to
   * another linestring. The Maximal Nearest Subline of A relative to B is the
   * shortest subline of A which contains all the points of A which are the
   * nearest points to the points in B. This effectively "trims" the ends of A
   * which are not near to B.
   * <p>
   * An exact computation of the MNS would require computing a line Voronoi. For
   * this reason, the algorithm used in this class is heuristic-based. It may
   * compute a geometry which is shorter than the actual MNS.
   */
  default LineString getMaximalNearestSubline(LineString line) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    line = line.convertGeometry(geometryFactory);
    LineStringLocation fromLocation = null;

    LineStringLocation toLocation = null;

    /**
    * The basic strategy is to pick test points on B and find their nearest
    * point on A. The interval containing these nearest points is approximately
    * the MaximalNeareastSubline of A.
    */

    {

      final int vertexCount2 = line.getVertexCount();
      for (int vertexIndex2 = 0; vertexIndex2 < vertexCount2; vertexIndex2++) {
        final double x = line.getX(vertexIndex2);
        final double y = line.getY(vertexIndex2);
        final LineStringLocation location = getLineStringLocation(x, y);
        if (location != null) {
          if (fromLocation == null || location.compareTo(fromLocation) < 0) {
            fromLocation = location;
          }
          if (toLocation == null || location.compareTo(toLocation) > 0) {
            toLocation = location;
          }
        }
      }
    }

    /**
    * Heuristic #2: find the nearest point on B to all vertices of A and use
    * those points of B as test points. For efficiency use only vertices of A
    * outside current max interval.
    */
    if (fromLocation != null) {
      final int vertexCount1 = getVertexCount();
      for (int vertexIndex1 = 0; vertexIndex1 < vertexCount1; vertexIndex1++) {
        final double x = getX(vertexIndex1);
        final double y = getY(vertexIndex1);

        if (vertexIndex1 <= fromLocation.getSegmentIndex()
          || vertexIndex1 > toLocation.getSegmentIndex()) {
          final LineStringLocation location2 = line.getLineStringLocation(x, y);
          if (location2 != null) {
            final Point point2 = location2.getPoint();
            final double x2 = point2.getX();
            final double y2 = point2.getY();
            final LineStringLocation location = getLineStringLocation(x2, y2);
            if (fromLocation == null || location.compareTo(fromLocation) < 0) {
              fromLocation = location;
            }
            if (toLocation == null || location.compareTo(toLocation) > 0) {
              toLocation = location;
            }
          }
        }
      }
    }
    return subLine(fromLocation, toLocation);
  }

  default PointLineStringMetrics getMetrics(Point point) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    point = point.convertGeometry(geometryFactory, 2);
    if (isEmpty() && point.isEmpty()) {
      return PointLineStringMetrics.EMPTY;
    } else {
      final double x = point.getX();
      final double y = point.getY();
      double lineLength = 0;
      double closestDistance = Double.MAX_VALUE;
      double distanceAlong = 0;
      Side side = null;
      final double resolutionXy;
      if (geometryFactory.isGeographics()) {
        resolutionXy = 0.0000001;
      } else {
        resolutionXy = 0.001;
      }
      final int vertexCount = getVertexCount();
      if (vertexCount > 0) {
        double x1 = getX(0);
        double y1 = getY(0);
        for (int vertexIndex = 1; vertexIndex < vertexCount; vertexIndex++) {
          final double x2 = getX(vertexIndex);
          final double y2 = getY(vertexIndex);

          final double distance = LineSegmentUtil.distanceLinePoint(x1, y1, x2, y2, x, y);
          final double segmentLength = MathUtil.distance(x1, y1, x2, y2);
          final double projectionFactor = LineSegmentUtil.projectionFactor(x1, y1, x2, y2, x, y);
          final boolean isEnd = vertexIndex == vertexCount - 1;
          if (vertexIndex == 1) {
            if (isEnd || projectionFactor <= 1) {
              if (distance < resolutionXy) {
                side = null;
              } else {
                side = Side.getSide(x1, y1, x2, y2, x, y);
              }
              closestDistance = distance;
              if (projectionFactor <= 1 || isEnd) {
                distanceAlong = segmentLength * projectionFactor;
              } else {
                distanceAlong = segmentLength;
              }
            }
          } else if (distance < closestDistance) {
            if (isEnd || projectionFactor <= 1) {
              closestDistance = distance;
              if (distance == 0 || distance < resolutionXy) {
                side = null;
              } else {
                side = Side.getSide(x1, y1, x2, y2, x, y);
              }
              // TODO handle intermediate cases right right hand bends in lines
              if (projectionFactor == 0) {
                distanceAlong = lineLength;
              } else if (projectionFactor < 0) {
                distanceAlong = lineLength;
              } else if (projectionFactor >= 1) {
                if (isEnd) {
                  distanceAlong = lineLength + segmentLength * projectionFactor;
                } else {
                  distanceAlong = lineLength + segmentLength;
                }
              } else {
                distanceAlong = lineLength + segmentLength * projectionFactor;
              }
            }
          }
          lineLength += segmentLength;
          x1 = x2;
          y1 = y2;
        }
      }
      return new PointLineStringMetrics(lineLength, distanceAlong, closestDistance, side);
    }
  }

  default int getMinVertexCount() {
    return 2;
  }

  @Override
  default Point getPoint() {
    if (isEmpty()) {
      return null;
    } else {
      return getPoint(0);
    }
  }

  default Point getPoint(final End lineEnd) {
    if (End.isFrom(lineEnd)) {
      return getFromPoint();
    } else {
      return getToPoint();
    }
  }

  default Point getPoint(int vertexIndex) {
    if (isEmpty()) {
      return null;
    } else {
      final int vertexCount = getVertexCount();
      while (vertexIndex < 0) {
        vertexIndex += vertexCount;
      }
      if (vertexIndex > vertexCount) {
        return null;
      } else {
        final int axisCount = getAxisCount();
        final double[] coordinates = new double[axisCount];
        for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
          coordinates[axisIndex] = getCoordinate(vertexIndex, axisIndex);
        }
        final GeometryFactory geometryFactory = getGeometryFactory();
        return geometryFactory.point(coordinates);
      }
    }
  }

  @Override
  default Point getPointWithin() {
    final GeometryFactory geometryFactory = this.getGeometryFactory();
    if (this.isEmpty()) {
      return geometryFactory.point();
    } else {
      final int numPoints = getVertexCount();
      if (numPoints > 1) {
        final double totalLength = getLength();
        final double midPointLength = totalLength / 2;
        double currentLength = 0;
        for (int i = 1; i < numPoints && currentLength < midPointLength; i++) {
          final Point p1 = getPoint(i - 1);
          final Point p2 = getPoint(i);
          final double segmentLength = p1.distancePoint(p2);
          if (segmentLength + currentLength >= midPointLength) {
            final Point midPoint = LineSegmentUtil.project(geometryFactory, p1, p2,
              (midPointLength - currentLength) / segmentLength);
            return geometryFactory.point(midPoint);

          }
          currentLength += segmentLength;
        }
        return geometryFactory.point();
      } else {
        return this.getPoint(0);
      }
    }
  }

  @Override
  default LineStringSegment getSegment(final int... segmentId) {
    if (segmentId.length == 1) {
      int segmentIndex = segmentId[0];
      final int vertexCount = getSegmentCount();
      if (segmentIndex < vertexCount) {
        while (segmentIndex < 0) {
          segmentIndex += vertexCount;
        }
        return new LineStringSegment(this, segmentIndex);
      }
    }
    return null;
  }

  @Override
  default int getSegmentCount() {
    if (isEmpty()) {
      return 0;
    } else {
      return getVertexCount() - 1;
    }
  }

  default Side getSide(final double x, final double y) {
    Side side = null;
    if (!isEmpty()) {
      final GeometryFactory geometryFactory = getGeometryFactory();
      double closestDistance = Double.MAX_VALUE;
      final double resolutionXy;
      if (geometryFactory.isGeographics()) {
        resolutionXy = 0.0000001;
      } else {
        resolutionXy = 0.001;
      }
      final int vertexCount = getVertexCount();
      final int lastVertexIndex = vertexCount - 1;
      if (vertexCount > 1) {
        final double x1 = getX(0);
        final double y1 = getY(0);
        for (int vertexIndex = 1; vertexIndex < vertexCount; vertexIndex++) {
          final double x2 = getX(vertexIndex);
          final double y2 = getY(vertexIndex);

          final double distance = LineSegmentUtil.distanceLinePoint(x1, y1, x2, y2, x, y);
          final double projectionFactor = LineSegmentUtil.projectionFactor(x1, y1, x2, y2, x, y);
          final boolean isEnd = vertexCount == lastVertexIndex;
          if (vertexIndex == 1) {
            if (isEnd || projectionFactor <= 1) {
              if (distance < resolutionXy) {
                side = null;
              } else {
                side = Side.getSide(x1, y1, x2, y2, x, y);
              }
              closestDistance = distance;
            }
          } else if (distance < closestDistance) {
            if (isEnd || projectionFactor <= 1) {
              closestDistance = distance;
              if (distance == 0 || distance < resolutionXy) {
                side = null;
              } else {
                side = Side.getSide(x1, y1, x2, y2, x, y);
              }
            }
          }
        }
      }
    }
    return side;
  }

  default Side getSide(Point point) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    point = point.convertGeometry(geometryFactory, 2);
    if (point.isEmpty()) {
      return null;
    } else {
      final double x = point.getX();
      final double y = point.getY();
      return getSide(x, y);
    }
  }

  default Point getToPoint() {
    if (isEmpty()) {
      return null;
    } else {
      final int vertexCount = getVertexCount();
      return getPoint(vertexCount - 1);
    }
  }

  @Override
  default AbstractVertex getToVertex(final int... vertexId) {
    if (vertexId.length == 1) {
      int vertexIndex = vertexId[0];
      final int vertexCount = getVertexCount();
      vertexIndex = vertexCount - vertexIndex - 1;
      if (vertexIndex < vertexCount) {
        while (vertexIndex < 0) {
          vertexIndex += vertexCount;
        }
        return new LineStringVertex(this, vertexIndex);
      }
    }
    return null;
  }

  @Override
  default AbstractVertex getVertex(final int... vertexId) {
    if (vertexId.length == 1) {
      final int vertexIndex = vertexId[0];
      return getVertex(vertexIndex);
    } else {
      return null;
    }
  }

  default AbstractVertex getVertex(int vertexIndex) {
    final int vertexCount = getVertexCount();
    if (vertexIndex < vertexCount) {
      while (vertexIndex < 0) {
        vertexIndex += vertexCount;
      }
      return new LineStringVertex(this, vertexIndex);
    } else {
      return null;
    }
  }

  default double getX(final int vertexIndex) {
    return getCoordinate(vertexIndex, X);
  }

  default double getY(final int vertexIndex) {
    return getCoordinate(vertexIndex, Y);
  }

  default double getZ(final int vertexIndex) {
    return getCoordinate(vertexIndex, Z);
  }

  @Override
  default boolean hasInvalidXyCoordinates() {
    final int vertexCount = getVertexCount();
    for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
      final double x = getX(vertexIndex);
      if (!Double.isFinite(x)) {
        return true;
      }
      final double y = getY(vertexIndex);

      if (!Double.isFinite(y)) {
        return true;
      }
    }
    return false;
  }

  default boolean hasVertex(final double x, final double y) {
    final int vertexCount = getVertexCount();
    for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
      if (equalsVertex2d(vertexIndex, x, y)) {
        return true;
      }
    }
    return false;
  }

  default boolean hasVertex(final Point point) {
    final int vertexCount = getVertexCount();
    for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
      if (equalsVertex(2, vertexIndex, point)) {
        return true;
      }
    }
    return false;
  }

  @Override
  default <G extends Geometry> G insertVertex(final Point newPoint, final int... vertexId) {
    if (vertexId.length == 1) {
      final int vertexIndex = vertexId[0];
      return insertVertex(newPoint, vertexIndex);
    } else {
      throw new IllegalArgumentException("Geometry id's for " + getGeometryType()
        + " must have length 1. " + Arrays.toString(vertexId));
    }
  }

  @SuppressWarnings("unchecked")
  default <G extends Geometry> G insertVertex(Point newPoint, final int vertexIndex) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    if (newPoint == null || newPoint.isEmpty()) {
      return (G)this;
    } else if (isEmpty()) {
      return newPoint.convertGeometry(geometryFactory);
    } else {
      newPoint = newPoint.convertGeometry(geometryFactory);
      final int vertexCount = getVertexCount();
      final double[] coordinates = getCoordinates();
      final int axisCount = getAxisCount();
      final double[] newCoordinates = new double[axisCount * (vertexCount + 1)];

      final int beforeLength = vertexIndex * axisCount;
      System.arraycopy(coordinates, 0, newCoordinates, 0, beforeLength);

      CoordinatesListUtil.setCoordinates(newCoordinates, axisCount, vertexIndex, newPoint);

      final int afterSourceIndex = vertexIndex * axisCount;
      final int afterNewIndex = (vertexIndex + 1) * axisCount;
      final int afterLength = (vertexCount - vertexIndex) * axisCount;
      System.arraycopy(coordinates, afterSourceIndex, newCoordinates, afterNewIndex, afterLength);

      return (G)newLineString(newCoordinates);
    }
  }

  @Override
  default boolean intersects(final BoundingBox boundingBox) {
    if (isEmpty() || boundingBox.isEmpty()) {
      return false;
    } else {
      final double minX = boundingBox.getMinX();
      final double maxX = boundingBox.getMaxX();
      final double minY = boundingBox.getMinY();
      final double maxY = boundingBox.getMaxY();
      final int vertexCount = getVertexCount();
      final GeometryFactory geometryFactory = boundingBox.getGeometryFactory();
      final CoordinatesOperation coordinatesOperation = getCoordinatesOperation(geometryFactory);
      if (coordinatesOperation == null) {
        double previousX = getX(0);
        double previousY = getY(0);

        for (int vertexIndex = 1; vertexIndex < vertexCount; vertexIndex++) {
          final double x = getX(vertexIndex);
          final double y = getY(vertexIndex);
          if (BoundingBoxUtil.intersectsLine(minX, minY, maxX, maxY, //
            previousX, previousY, x, y)) {
            return true;
          }
          previousX = x;
          previousY = y;
        }
      } else {
        final double[] coordinates = new double[] {
          getX(0), getY(0)
        };
        coordinatesOperation.perform(2, coordinates, 2, coordinates);
        double previousX = coordinates[X];
        double previousY = coordinates[Y];

        for (int vertexIndex = 1; vertexIndex < vertexCount; vertexIndex++) {
          coordinates[X] = getX(vertexIndex);
          coordinates[Y] = getY(vertexIndex);
          coordinatesOperation.perform(2, coordinates, 2, coordinates);
          final double x = coordinates[X];
          final double y = coordinates[Y];
          if (BoundingBoxUtil.intersectsLine(minX, minY, maxX, maxY, //
            previousX, previousY, x, y)) {
            return true;
          }
          previousX = x;
          previousY = y;
        }
      }

      return false;
    }
  }

  default boolean isClockwise() {
    final ClockDirection clockDirection = getClockDirection();
    return clockDirection.isClockwise();
  }

  @Override
  default boolean isClosed() {
    if (isEmpty()) {
      return false;
    } else {
      final int lastIndex = getVertexCount() - 1;
      final double x1 = getX(0);
      final double xn = getX(lastIndex);
      if (x1 == xn) {
        final double y1 = getY(0);
        final double yn = getY(lastIndex);
        if (y1 == yn) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Tests if a linestring is completely contained in the boundary of the target rectangle.
   * @param boundingBox TODO
    * @return true if the linestring is contained in the boundary
   */
  @Override
  default boolean isContainedInBoundary(final BoundingBox boundingBox) {
    final int vertexCount = getVertexCount();
    if (vertexCount > 0) {
      final double minX = boundingBox.getMinX();
      final double minY = boundingBox.getMinY();
      final double maxX = boundingBox.getMaxX();
      final double maxY = boundingBox.getMaxY();
      double previousX = getX(0);
      double previousY = getY(0);
      boolean hadSegment = false;
      for (int vertexIndex = 1; vertexIndex < vertexCount; vertexIndex++) {
        final double x = getX(vertexIndex);
        final double y = getY(vertexIndex);
        if (!(x == previousX && y == previousY)) {
          hadSegment = true;

          // we already know that the segment is contained in the rectangle
          // envelope
          if (previousX == x) {
            if (previousX == minX || previousX == maxX) {
              return true;
            }
          } else if (previousY == y) {
            if (previousY == minY || previousY == maxY) {
              return true;
            }
          }
          previousX = x;
          previousY = y;
        }
      }
      if (!hadSegment) {
        if (previousX == minX || previousX == maxX || previousY == minY || previousY == maxY) {
          return true;
        }

      }
    }
    return false;
  }

  default boolean isCounterClockwise() {
    final ClockDirection clockDirection = getClockDirection();
    return clockDirection.isCounterClockwise();
  }

  @Override
  default boolean isEmpty() {
    return getVertexCount() == 0;
  }

  @Override
  default boolean isEquivalentClass(final Geometry other) {
    return other instanceof LineString;
  }

  default boolean isLeft(final Point point) {
    final double x = point.getX();
    final double y = point.getY();
    final int vertexCount = getVertexCount();
    if (vertexCount > 1) {
      double x1 = getX(0);
      double y1 = getY(0);
      for (int vertexIndex = 1; vertexIndex < vertexCount; vertexIndex++) {
        final double x2 = getX(vertexIndex);
        final double y2 = getY(vertexIndex);
        if (!crosses(new LineSegmentDouble(2, x1, y1, x, y))
          && !crosses(new LineSegmentDouble(2, x2, y2, x, y))) {
          final int orientation = LineSegmentUtil.orientationIndex(x1, y1, x2, y2, x, y);
          if (orientation == 1) {
            return true;
          } else {
            return false;
          }
        }
        x1 = x2;
        y1 = y2;
      }
      return true;
    } else {
      return false;
    }
  }

  default boolean isOnLine(final double x, final double y) {
    final LineIntersector lineIntersector = new RobustLineIntersector();
    double x1 = getX(0);
    double y1 = getY(0);
    final int vertexCount = getVertexCount();
    for (int i = 1; i < vertexCount; i++) {
      final double x2 = getX(i);
      final double y2 = getY(i);
      if (lineIntersector.computeIntersection(x, y, x1, y1, x2, y2)) {
        return true;
      } else {
        x1 = x2;
        y1 = y2;
      }
    }
    return false;
  }

  /**
   * Tests whether a point lies on the line segments defined by a list of
   * coordinates.
   *
   * @return true if the point is a vertex of the line or lies in the interior
   *         of a line segment in the linestring
   */
  default boolean isOnLine(final Point point) {
    final double x = point.getX();
    final double y = point.getY();
    return isOnLine(x, y);
  }

  default boolean isPointInRing(final double x, final double y) {
    return RayCrossingCounter.locatePointInRing(this, x, y) != Location.EXTERIOR;
  }

  default boolean isPointInRing(final Point point) {
    return RayCrossingCounter.locatePointInRing(point, this) != Location.EXTERIOR;
  }

  default boolean isRing() {
    return isClosed() && isSimple();
  }

  @Override
  default Iterable<LineString> lineStrings() {
    return Collections.singletonList(this);
  }

  @Override
  default Location locate(final double x, final double y) {
    // bounding-box check
    final BoundingBox boundingBox = getBoundingBox();
    if (boundingBox.covers(x, y)) {
      if (!isClosed()) {
        if (equalsVertex2d(0, x, y) || equalsVertex2d(getLastVertexIndex(), x, y)) {
          return Location.BOUNDARY;
        }
      }
      if (isOnLine(x, y)) {
        return Location.INTERIOR;
      }
    }
    return Location.EXTERIOR;
  }

  @Override
  default Location locate(final Point point) {
    // bounding-box check
    if (point.intersects(getBoundingBox())) {
      if (!isClosed()) {
        if (equals(2, 0, point) || equals(2, getVertexCount() - 1, point)) {
          return Location.BOUNDARY;
        }
      }
      if (isOnLine(point)) {
        return Location.INTERIOR;
      }
    }
    return Location.EXTERIOR;
  }

  /**
   * Merge two lines that share common coordinates at either the start or end.
   * If the lines touch only at their start coordinates, the line2 will be
   * reversed and joined before the start of line1. If the two lines touch only
   * at their end coordinates, the line2 will be reversed and joined after the
   * end of line1.
   *
   * @param line1 The first line.
   * @param line2 The second line.
   * @return The new line string
   */
  default LineString merge(final LineString line2) {
    final int axisCount = Math.max(getAxisCount(), line2.getAxisCount());
    final int vertexCount1 = getVertexCount();
    final int vertexCount2 = line2.getVertexCount();
    final int vertexCount = vertexCount1 + vertexCount2 - 1;
    final double[] coordinates = new double[vertexCount * axisCount];

    int newVertexCount = 0;
    final Point line1From = getVertex(0);
    final Point line1To = getVertex(getVertexCount() - 1);
    final Point line2From = line2.getVertex(0);
    final Point line2To = line2.getVertex(line2.getVertexCount() - 1);
    if (line1From.equals(2, line2To)) {
      newVertexCount = CoordinatesListUtil.append(axisCount, line2, 0, coordinates, 0,
        vertexCount2);
      newVertexCount = CoordinatesListUtil.append(axisCount, this, 1, coordinates, newVertexCount,
        vertexCount1 - 1);
    } else if (line2From.equals(2, line1To)) {
      newVertexCount = CoordinatesListUtil.append(axisCount, this, 0, coordinates, 0, vertexCount1);
      newVertexCount = CoordinatesListUtil.append(axisCount, line2, 1, coordinates, newVertexCount,
        vertexCount2 - 1);
    } else if (line1From.equals(2, line2From)) {
      newVertexCount = CoordinatesListUtil.appendReverse(axisCount, line2, 0, coordinates, 0,
        vertexCount2);
      newVertexCount = CoordinatesListUtil.append(axisCount, this, 1, coordinates, newVertexCount,
        vertexCount1 - 1);
    } else if (line1To.equals(2, line2To)) {
      newVertexCount = CoordinatesListUtil.append(axisCount, this, 0, coordinates, newVertexCount,
        vertexCount1);
      newVertexCount = CoordinatesListUtil.appendReverse(axisCount, line2, 1, coordinates,
        newVertexCount, vertexCount2 - 1);
    } else {
      throw new IllegalArgumentException("lines don't touch\n" + this + "\n" + line2);

    }
    final LineString newLine = newLineString(newVertexCount, coordinates);
    return newLine;
  }

  default LineString merge(final Point point, final LineString line2) {
    if (isEmpty() || Property.isEmpty(line2) || Property.isEmpty(point)) {
      return newLineStringEmpty();
    } else {
      final int axisCount = Math.max(getAxisCount(), line2.getAxisCount());
      final int vertexCount1 = getVertexCount();
      final int vertexCount2 = line2.getVertexCount();
      final int vertexCount = vertexCount1 + vertexCount2 - 1;
      final double[] coordinates = new double[vertexCount * axisCount];

      int newVertexCount = 0;
      final Point line1From = getVertex(0);
      final Point line1To = getVertex(getVertexCount() - 1);
      final Point line2From = line2.getVertex(0);
      final Point line2To = line2.getVertex(line2.getVertexCount() - 1);
      if (line1To.equals(2, line2From) && line1To.equals(2, point)) {
        // -->*--> = ---->
        newVertexCount = CoordinatesListUtil.append(axisCount, this, 0, coordinates, 0,
          vertexCount1);
        newVertexCount = CoordinatesListUtil.append(axisCount, line2, 1, coordinates,
          newVertexCount, vertexCount2 - 1);
      } else if (line1From.equals(2, line2To) && line1From.equals(2, point)) {
        // <--*<-- = <----
        newVertexCount = CoordinatesListUtil.append(axisCount, line2, 0, coordinates, 0,
          vertexCount2);
        newVertexCount = CoordinatesListUtil.append(axisCount, this, 1, coordinates, newVertexCount,
          vertexCount1 - 1);
      } else if (line1From.equals(2, line2From) && line1From.equals(2, point)) {
        // <--*--> = <----
        newVertexCount = CoordinatesListUtil.appendReverse(axisCount, line2, 0, coordinates, 0,
          vertexCount2);
        newVertexCount = CoordinatesListUtil.append(axisCount, this, 1, coordinates, newVertexCount,
          vertexCount1 - 1);
      } else if (line1To.equals(2, line2To) && line1To.equals(2, point)) {
        // -->*<-- = ---->
        newVertexCount = CoordinatesListUtil.append(axisCount, this, 0, coordinates, newVertexCount,
          vertexCount1);
        newVertexCount = CoordinatesListUtil.appendReverse(axisCount, line2, 1, coordinates,
          newVertexCount, vertexCount2 - 1);
      } else {
        throw new IllegalArgumentException("lines don't touch\n" + this + "\n" + line2);
      }
      final LineString newLine = newLineString(newVertexCount, coordinates);
      return newLine;
    }
  }

  @Override
  default Lineal mergeLines() {
    return this;
  }

  @Override
  default LineString move(final double... deltas) {
    if (deltas == null || isEmpty()) {
      return this;
    } else {
      final double[] coordinates = moveCoordinates(deltas);
      return newLineString(coordinates);
    }
  }

  default double[] moveCoordinates(final double... deltas) {
    final double[] coordinates = getCoordinates();
    final int vertexCount = getVertexCount();
    final int axisCount = getAxisCount();
    final int deltaCount = Math.min(deltas.length, getAxisCount());
    for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
      for (int axisIndex = 0; axisIndex < deltaCount; axisIndex++) {
        coordinates[vertexIndex * axisCount + axisIndex] += deltas[axisIndex];
      }
    }
    return coordinates;
  }

  @Override
  @SuppressWarnings("unchecked")
  default <V extends Geometry> V moveVertex(final Point newPoint, final int... vertexId) {
    if (vertexId.length == 1) {
      final int vertexIndex = vertexId[0];
      return (V)moveVertex(newPoint, vertexIndex);
    } else {
      throw new IllegalArgumentException("Vertex id's for " + getGeometryType()
        + " must have length 1. " + Arrays.toString(vertexId));
    }
  }

  default LineString moveVertex(Point newPoint, final int vertexIndex) {
    if (newPoint == null || newPoint.isEmpty()) {
      return this;
    } else if (isEmpty()) {
      throw new IllegalArgumentException("Cannot move vertex for empty LineString");
    } else {
      final int vertexCount = getVertexCount();
      if (vertexIndex >= 0 && vertexIndex < vertexCount) {
        final GeometryFactory geometryFactory = getGeometryFactory();
        newPoint = newPoint.convertGeometry(geometryFactory);

        final double[] coordinates = getCoordinates();
        final int axisCount = getAxisCount();
        CoordinatesListUtil.setCoordinates(coordinates, axisCount, vertexIndex, newPoint);
        return newLineString(coordinates);
      } else {
        throw new IllegalArgumentException("Vertex index must be between 0 and " + vertexCount);
      }
    }
  }

  @Override
  default LineString newGeometry(final GeometryFactory geometryFactory) {
    if (geometryFactory == null) {
      return this.clone();
    } else if (isEmpty()) {
      return newLineStringEmpty(geometryFactory);
    } else {
      final double[] coordinates = convertCoordinates(geometryFactory);
      final int axisCount = geometryFactory.getAxisCount();
      final int vertexCount = getVertexCount();
      return newLineString(geometryFactory, axisCount, vertexCount, coordinates);
    }
  }

  @Override
  default LineStringEditor newGeometryEditor() {
    return new LineStringEditor(this);
  }

  @Override
  default LineStringEditor newGeometryEditor(final int axisCount) {
    final LineStringEditor geometryEditor = newGeometryEditor();
    geometryEditor.setAxisCount(axisCount);
    return geometryEditor;
  }

  /**
   * Create a new {@link LinearRing} of this {@link LineString} using this geometry's geometry factory.
   *
   * @return The new linear ring.
   */
  default LinearRing newLinearRing() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    return geometryFactory.linearRing(this);
  }

  default LineString newLineString() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    return geometryFactory.lineString(this);
  }

  default LineString newLineString(final double... coordinates) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    final int axisCount = getAxisCount();
    final int vertexCount = coordinates.length / axisCount;
    return newLineString(geometryFactory, axisCount, vertexCount, coordinates);
  }

  default LineString newLineString(final GeometryFactory geometryFactory) {
    return geometryFactory.lineString(this);
  }

  default LineString newLineString(final GeometryFactory geometryFactory, final int axisCount,
    final int vertexCount, final double... coordinates) {
    final GeometryFactory geometryFactoryAxisCount = geometryFactory.convertAxisCount(axisCount);
    return geometryFactoryAxisCount.lineString(axisCount, vertexCount, coordinates);
  }

  default LineString newLineString(final int vertexCount, final double... coordinates) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    final int axisCount = getAxisCount();
    return newLineString(geometryFactory, axisCount, vertexCount, coordinates);
  }

  /**
   * Create a new {@link LineString} of this {@link LineString} using this geometry's geometry factory.
   *
   * @return The new line string.
   */
  default LineString newLineStringEmpty() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    return newLineStringEmpty(geometryFactory);
  }

  default LineString newLineStringEmpty(final GeometryFactory geometryFactory) {
    return geometryFactory.lineString();
  }

  @SuppressWarnings("unchecked")
  @Override
  default <G> G newUsingGeometryFactory(final GeometryFactory factory) {
    if (factory == getGeometryFactory()) {
      return (G)this;
    } else if (isEmpty()) {
      return (G)newLineStringEmpty();
    } else {
      final double[] coordinates = getCoordinates();
      return (G)newLineString(coordinates);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  default <G extends Geometry> G newValidGeometry() {
    if (isEmpty()) {
      return (G)this;
    } else if (isValid()) {
      return (G)normalize();
    } else {
      final LineMerger lines = new LineMerger(this);
      return (G)lines.getLineal();
    }
  }

  /**
   * Normalizes a LineString. A normalized linestring has the first point which
   * is not equal to it's reflected point less than the reflected point.
   */
  @Override
  default LineString normalize() {
    final int vertexCount = getVertexCount();
    for (int i = 0; i < vertexCount / 2; i++) {
      final int j = vertexCount - 1 - i;
      final Vertex point1 = getVertex(i);
      final Vertex point2 = getVertex(j);
      // skip equal points on both ends
      if (!point1.equals(2, point2)) {
        if (point1.compareTo(point2) > 0) {
          return reverse();
        }
        return this;
      }
    }
    return this;
  }

  default int orientationIndex(final int index1, final int index2, final int index) {
    final double x1 = getX(index1);
    final double y1 = getY(index1);
    final double x2 = getX(index2);
    final double y2 = getY(index2);
    final double x = getX(index);
    final double y = getY(index);
    return CoordinatesListUtil.orientationIndex(x1, y1, x2, y2, x, y);
  }

  default Iterable<Point> points() {
    final List<Point> points = new ArrayList<>();
    for (int i = 0; i < getVertexCount(); i++) {
      final Point point = getPoint(i);
      points.add(point);
    }
    return points;
  }

  @Override
  @Deprecated
  default LineString prepare() {
    return new PreparedLineString(this);
  }

  @Override
  default LineString removeDuplicatePoints() {
    if (isEmpty()) {
      return this;
    } else {
      final int vertexCount = getVertexCount();
      if (vertexCount < 2) {
        return this;
      } else {
        final int axisCount = getAxisCount();
        final double[] coordinates = new double[vertexCount * axisCount];
        double previousX = getX(0);
        double previousY = getY(0);
        CoordinatesListUtil.setCoordinates(coordinates, axisCount, 0, this, 0);
        int j = 1;
        for (int i = 1; i < vertexCount; i++) {
          final double x = getX(i);
          final double y = getY(i);
          if (x != previousX || y != previousY) {
            CoordinatesListUtil.setCoordinates(coordinates, axisCount, j++, this, i);
          }
          previousX = x;
          previousY = y;
        }

        if (j < 2) {
          return newLineStringEmpty();
        } else {
          return newLineString(j, coordinates);
        }
      }
    }
  }

  /**
   * Creates a {@link LineString} whose coordinates are in the reverse order of
   * this objects
   *
   * @return a {@link LineString} with coordinates in the reverse order
   */
  @Override
  default LineString reverse() {
    final int vertexCount = getVertexCount();
    final int axisCount = getAxisCount();
    final double[] coordinates = new double[vertexCount * axisCount];
    for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
      for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
        final int coordinateIndex = (vertexCount - 1 - vertexIndex) * axisCount + axisIndex;
        coordinates[coordinateIndex] = getCoordinate(vertexIndex, axisIndex);
      }
    }
    final LineString reverseLine = newLineString(coordinates);
    return reverseLine;
  }

  @Override
  default Iterable<Segment> segments() {
    return new LineStringSegment(this, -1);
  }

  default List<LineString> split(final Iterable<LineStringLocation> splitLocations) {
    final Set<LineStringLocation> locations = new TreeSet<>();
    for (final LineStringLocation location : splitLocations) {
      if (location.getLine() == this) {
        if (location.isFromVertex()) {
          // Don't split at the start
        } else if (location.isToVertex()) {
          // Don't split at the end
        } else {
          locations.add(location);
        }
      }
    }
    if (locations.isEmpty()) {
      return Collections.singletonList(this);
    } else {
      final List<LineString> newLines = new ArrayList<>();
      LineStringLocation previousLocation = null;
      for (final LineStringLocation location : locations) {
        final LineString newLine = subLine(previousLocation, location);
        if (!newLine.isEmpty()) {
          newLines.add(newLine);
        }
        previousLocation = location;
      }
      final LineString newLine = subLine(previousLocation, null);
      if (!newLine.isEmpty()) {
        newLines.add(newLine);
      }
      return newLines;
    }
  }

  default List<LineString> split(final LineStringLocation location) {
    if (location == null || location.isFromVertex() || location.isToVertex()) {
      return Collections.singletonList(this);
    } else {
      final List<LineString> newLines = new ArrayList<>(2);

      final LineString newLine1 = subLine(null, location);
      if (!newLine1.isEmpty()) {
        newLines.add(newLine1);
      }
      final LineString newLine2 = subLine(location, null);
      if (!newLine2.isEmpty()) {
        newLines.add(newLine2);
      }
      return newLines;
    }
  }

  default List<LineString> split(Point point) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    point = point.convertGeometry(geometryFactory);
    final double x = point.getX();
    final double y = point.getY();
    final Pair<GeometryComponent, Double> result = findClosestGeometryComponent(x, y);
    if (result.isEmpty()) {
      return Collections.<LineString> singletonList(this);
    } else {
      final int vertexCount = getVertexCount();
      final GeometryComponent geometryComponent = result.getValue1();
      final double distance = result.getValue2();
      if (geometryComponent instanceof Vertex) {
        final Vertex vertex = (Vertex)geometryComponent;
        final int vertexIndex = vertex.getVertexIndex();
        if (distance == 0) {
          if (vertexIndex <= 0 || vertexIndex >= vertexCount - 1) {
            return Collections.<LineString> singletonList(this);
          } else {
            final LineString line1 = subLine(vertexIndex + 1);
            final LineString line2 = subLine(vertexIndex, vertexCount - vertexIndex);
            return Arrays.asList(line1, line2);
          }
        } else {
          final LineString line1 = subLine(vertexIndex + 1, point);
          final LineString line2 = subLine(point, vertexIndex, vertexCount - vertexIndex, null);
          return Arrays.asList(line1, line2);
        }
      } else if (geometryComponent instanceof Segment) {
        final Segment segment = (Segment)geometryComponent;
        final int segmentIndex = segment.getSegmentIndex();
        final LineString line1 = subLine(segmentIndex + 1, point);
        final LineString line2 = subLine(point, segmentIndex + 1, vertexCount - segmentIndex - 1,
          null);
        return Arrays.asList(line1, line2);
      } else {
        return Collections.<LineString> singletonList(this);
      }
    }
  }

  default LineString subLine(final int vertexCount) {
    return subLine(null, 0, vertexCount, null);
  }

  default LineString subLine(final int fromVertexIndex, final int vertexCount) {
    return subLine(null, fromVertexIndex, vertexCount, null);
  }

  default LineString subLine(final int vertexCount, final Point toPoint) {
    return subLine(null, 0, vertexCount, toPoint);
  }

  default LineString subLine(final LineStringLocation fromLocation,
    final LineStringLocation toLocation) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    final LineStringEditor lineBuilder = new LineStringEditor(geometryFactory);

    int vertexIndexFrom;
    if (fromLocation == null || fromLocation.getLine() != this) {
      vertexIndexFrom = 0;
    } else {
      vertexIndexFrom = fromLocation.getSegmentIndex();
      if (fromLocation.getSegmentFraction() > 0.0) {
        vertexIndexFrom += 1;
      }
      if (!fromLocation.isVertex()) {
        final Point point = fromLocation.getPoint();
        lineBuilder.appendVertex(point, false);
      }
    }

    Point toPoint;
    int vertexIndexTo;
    if (toLocation == null) {
      vertexIndexTo = getVertexCount() - 1;
      toPoint = null;
    } else {
      vertexIndexTo = toLocation.getSegmentIndex();
      if (toLocation.getSegmentFraction() >= 1.0) {
        vertexIndexTo += 1;
      }
      if (toLocation.isVertex()) {
        toPoint = null;
      } else {
        toPoint = toLocation.getPoint();
      }
    }

    for (int vertexIndex = vertexIndexFrom; vertexIndex <= vertexIndexTo; vertexIndex++) {
      final Point point = getPoint(vertexIndex);
      lineBuilder.appendVertex(point, false);
    }
    if (toPoint != null) {
      lineBuilder.appendVertex(toPoint, false);
    }
    if (lineBuilder.getVertexCount() < 2) {
      return lineBuilder.newLineStringEmpty();
    } else {
      return lineBuilder.newLineString();
    }
  }

  default LineString subLine(final Point fromPoint, final int fromVertexIndex, int vertexCount,
    final Point toPoint) {
    if (fromVertexIndex + vertexCount > getVertexCount()) {
      vertexCount = getVertexCount() - fromVertexIndex;
    }
    int newVertexCount = vertexCount;
    final boolean hasFromPoint = fromPoint != null && !fromPoint.isEmpty();
    if (hasFromPoint) {
      newVertexCount++;
    }
    final boolean hasToPoint = toPoint != null && !toPoint.isEmpty();
    if (hasToPoint) {
      newVertexCount++;
    }
    if (newVertexCount < 2) {
      return newLineStringEmpty();
    } else {
      final int axisCount = getAxisCount();
      final double[] coordinates = new double[newVertexCount * axisCount];
      int vertexIndex = 0;
      if (hasFromPoint) {
        CoordinatesListUtil.setCoordinates(coordinates, axisCount, vertexIndex++, fromPoint);
      }
      CoordinatesListUtil.setCoordinates(coordinates, axisCount, vertexIndex, this, fromVertexIndex,
        vertexCount);
      vertexIndex += vertexCount;
      if (hasToPoint) {
        CoordinatesListUtil.setCoordinates(coordinates, axisCount, vertexIndex++, toPoint);
      }
      final LineString newLine = newLineString(coordinates);
      return newLine;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  default <G extends Geometry> G toClockwise() {
    if (isClockwise()) {
      return (G)this;
    } else {
      return (G)this.reverse();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  default <G extends Geometry> G toCounterClockwise() {
    if (isClockwise()) {
      return (G)this.reverse();
    } else {
      return (G)this;
    }
  }

  /**
   * Get the end of this line that touches an end of the other line. If the
   * lines don't touch at the ends then null will be returned.
   *
   * @return The end that touches.
   */
  default End touchingEnd(final LineString line) {
    if (isEmpty() || Property.isEmpty(line)) {
      return null;
    } else {
      for (final End end : End.VALUES) {
        final Point point = line.getPoint(end);
        final End touchingEnd = touchingEnd(point);
        if (touchingEnd != null) {
          return touchingEnd;
        }
      }
      return null;
    }
  }

  /**
   * Get the end of this line that touches the other point. If the point and
   * line don't touch at the end then null will be returned.
   *
   * @return The end that touches.
   */
  default End touchingEnd(final Point point) {
    if (isEmpty() || Property.isEmpty(point)) {
      return null;
    } else if (equalsVertex(0, point)) {
      return End.FROM;
    } else if (equalsVertex(getVertexCount() - 1, point)) {
      return End.TO;
    } else {
      return null;
    }
  }

  /**
   * Get the end of this line that touches an end of the other line. If the
   * lines don't touch at the ends then null will be returned.
   *
   * @return An array with the end of this line and then end of the other that
   *         touches, or null if they don't touch.
   */
  default End[] touchingEnds(final LineString line) {
    if (isEmpty() || Property.isEmpty(line)) {
      return null;
    } else {
      for (final End end : End.VALUES) {
        final Point point = line.getPoint(end);
        final End touchingEnd = touchingEnd(point);
        if (touchingEnd != null) {
          return new End[] {
            touchingEnd, end
          };
        }
      }
      return null;
    }
  }

  @Override
  default LineStringVertex vertices() {
    return new LineStringVertex(this, -1);
  }
}
