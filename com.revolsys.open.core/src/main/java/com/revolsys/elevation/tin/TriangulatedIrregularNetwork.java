package com.revolsys.elevation.tin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.revolsys.collection.map.MapEx;
import com.revolsys.geometry.index.BoundingBoxSpatialIndex;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.GeometryFactoryProxy;
import com.revolsys.geometry.model.LineString;
import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.Triangle;
import com.revolsys.geometry.model.impl.PointDouble;
import com.revolsys.geometry.model.segment.LineSegment;
import com.revolsys.geometry.model.segment.LineSegmentDoubleGF;
import com.revolsys.io.IoFactory;
import com.revolsys.spring.resource.Resource;

public interface TriangulatedIrregularNetwork extends GeometryFactoryProxy {
  static final int[] OPPOSITE_INDEXES = {
    2, 1, 0
  };

  static final String GEOMETRY_FACTORY = "geometryFactory";

  /**
   * Get the index of the corner or a triangle opposite corners i1 -> i2. i1 and
   * i2 must have different values in the range 0..2.
   *
   * @param i1
   * @param i2
   * @return
   */
  public static int getOtherIndex(final int i1, final int i2) {
    return OPPOSITE_INDEXES[i1 + i2 - 1];
  }

  static TriangulatedIrregularNetwork newTriangulatedIrregularNetwork(final Object source) {
    final Map<String, Object> properties = Collections.emptyMap();
    return newTriangulatedIrregularNetwork(source, properties);
  }

  static TriangulatedIrregularNetwork newTriangulatedIrregularNetwork(final Object source,
    final Map<String, ? extends Object> properties) {
    final TriangulatedIrregularNetworkReadFactory factory = IoFactory
      .factory(TriangulatedIrregularNetworkReadFactory.class, source);
    if (factory == null) {
      return null;
    } else {
      final Resource resource = factory.getZipResource(source);
      final TriangulatedIrregularNetwork dem = factory.newTriangulatedIrregularNetwork(resource,
        properties);
      return dem;
    }
  }

  default void cancelChanges() {
  }

  BoundingBox getBoundingBox();

  default LineString getElevation(final LineString line) {
    final GeometryFactory geometryFactory = line.getGeometryFactory();
    final int vertexCount = line.getVertexCount();
    final int axisCount = line.getAxisCount();
    final double[] newCoordinates = new double[vertexCount * axisCount];

    boolean modified = false;
    int i = 0;
    for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
      for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
        double value = line.getCoordinate(vertexIndex, axisIndex);
        if (axisIndex == 2) {
          final double newZ = getElevation(line.getPoint(vertexIndex));
          if (!Double.isNaN(newZ)) {
            if (value != newZ) {
              value = newZ;
              modified = true;
            }
          }
        }
        newCoordinates[i] = value;
        i++;
      }
    }
    if (modified) {
      return geometryFactory.lineString(axisCount, newCoordinates);
    } else {
      return line;
    }
  }

  default double getElevation(Point point) {
    point = convertGeometry(point);
    final List<Triangle> triangles = getTriangles(point);
    for (final Triangle triangle : triangles) {
      final Point t0 = triangle.getP0();
      if (t0.equals(point)) {
        return t0.getZ();
      }
      final Point t1 = triangle.getP1();
      if (t1.equals(point)) {
        return t1.getZ();
      }
      final Point t2 = triangle.getP2();
      if (t2.equals(point)) {
        return t2.getZ();
      }
      Point closestCorner = t0;
      LineSegment oppositeEdge = new LineSegmentDoubleGF(t1, t2);
      double closestDistance = point.distance(closestCorner);
      final double t1Distance = point.distance(t1);
      if (closestDistance > t1Distance) {
        closestCorner = t1;
        oppositeEdge = new LineSegmentDoubleGF(t2, t0);
        closestDistance = t1Distance;
      }
      if (closestDistance > point.distance(t2)) {
        closestCorner = t2;
        oppositeEdge = new LineSegmentDoubleGF(t0, t1);
      }
      LineSegment segment = new LineSegmentDoubleGF(closestCorner, point).extend(0,
        t0.distance(t1) + t1.distance(t2) + t0.distance(t2));
      final Geometry intersectCoordinates = oppositeEdge.getIntersection(segment);
      if (intersectCoordinates.getVertexCount() > 0) {
        final Point intersectPoint = intersectCoordinates.getVertex(0);
        final double z = oppositeEdge.getElevation(intersectPoint);
        if (!Double.isNaN(z)) {
          final double x = intersectPoint.getX();
          final double y = intersectPoint.getY();
          final Point end = new PointDouble(x, y, z);
          segment = new LineSegmentDoubleGF(t0, end);
          return segment.getElevation(point);
        }
      }
    }
    return Double.NaN;
  }

  Set<Point> getNodes();

  Resource getResource();

  default int getSize() {
    final BoundingBoxSpatialIndex<Triangle> index = getTriangleIndex();
    if (index == null) {
      return 0;
    } else {
      return index.getSize();
    }
  }

  BoundingBoxSpatialIndex<Triangle> getTriangleIndex();

  default List<Triangle> getTriangles() {
    final BoundingBoxSpatialIndex<Triangle> index = getTriangleIndex();
    if (index == null) {
      return Collections.emptyList();
    } else {
      return index.findAll();
    }
  }

  default List<Triangle> getTriangles(BoundingBox boundingBox) {
    boundingBox = boundingBox.convert(getGeometryFactory());
    final BoundingBoxSpatialIndex<Triangle> index = getTriangleIndex();
    return index.find(boundingBox);
  }

  default List<Triangle> getTriangles(final LineSegment segment) {
    final BoundingBox boundingBox = segment.getBoundingBox();
    return getTriangles(boundingBox);
  }

  default List<Triangle> getTriangles(final Point point) {
    final BoundingBox boundingBox = point.getBoundingBox();
    final Predicate<Triangle> filter = (triangle) -> {
      return triangle.hasVertex(point);
    };
    final BoundingBoxSpatialIndex<Triangle> index = getTriangleIndex();
    return index.find(boundingBox, filter);
  }

  default boolean writeTriangulatedIrregularNetwork() {
    return writeTriangulatedIrregularNetwork(MapEx.EMPTY);
  }

  default boolean writeTriangulatedIrregularNetwork(
    final Map<String, ? extends Object> properties) {
    final Resource resource = getResource();
    if (resource == null) {
      return false;
    } else {
      writeTriangulatedIrregularNetwork(resource, properties);
      return true;
    }
  }

  default void writeTriangulatedIrregularNetwork(final Object target) {
    final Map<String, ? extends Object> properties = Collections.emptyMap();
    writeTriangulatedIrregularNetwork(target, properties);
  }

  default void writeTriangulatedIrregularNetwork(final Object target,
    final Map<String, ? extends Object> properties) {
    try (
      TriangulatedIrregularNetworkWriter writer = TriangulatedIrregularNetworkWriter
        .newTriangulatedIrregularNetworkWriter(target, properties)) {
      if (writer == null) {
        throw new IllegalArgumentException(
          "No triangulated irregular network writer exists for " + target);
      }
      writer.write(this);
    }
  }
}