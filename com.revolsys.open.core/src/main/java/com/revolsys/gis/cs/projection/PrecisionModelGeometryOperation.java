package com.revolsys.gis.cs.projection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.revolsys.gis.jts.JtsGeometryUtil;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.gis.model.coordinates.list.DoubleCoordinatesList;
import com.revolsys.jts.geom.CoordinatesList;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.LineString;
import com.revolsys.jts.geom.LinearRing;
import com.revolsys.jts.geom.MultiLineString;
import com.revolsys.jts.geom.MultiPoint;
import com.revolsys.jts.geom.MultiPolygon;
import com.revolsys.jts.geom.Point;
import com.revolsys.jts.geom.Polygon;

public class PrecisionModelGeometryOperation implements GeometryOperation {
  private final GeometryFactory geometryFactory;

  public PrecisionModelGeometryOperation(final GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }

  private void addUserData(final Geometry oldGeometry,
    final Geometry newGeometry) {
    final Object userData = oldGeometry.getUserData();
    if (userData != null) {
      if (userData instanceof Map) {
        JtsGeometryUtil.copyUserData(oldGeometry, newGeometry);
      } else {
        newGeometry.setUserData(userData);
      }
    }
  }

  public CoordinatesList perform(final CoordinatesList coordinates) {
    final int numAxis = geometryFactory.getNumAxis();
    final CoordinatesList newCoordinates = new DoubleCoordinatesList(numAxis,
      coordinates);
    newCoordinates.makePrecise(geometryFactory);
    return newCoordinates;
  }

  public LinearRing perform(final LinearRing ring) {
    if (ring != null) {
      final CoordinatesList newCoordinates = perform(CoordinatesListUtil.get(ring));
      final LinearRing newRing = geometryFactory.createLinearRing(newCoordinates);
      addUserData(ring, newRing);
      return newRing;
    } else {
      return null;
    }
  }

  public LineString perform(final LineString line) {
    if (line != null) {
      final CoordinatesList newCoordinates = perform(CoordinatesListUtil.get(line));
      final LineString newLine = geometryFactory.lineString(newCoordinates);
      addUserData(line, newLine);
      return newLine;
    } else {
      return null;
    }
  }

  public MultiLineString perform(final MultiLineString multiLineString) {
    if (multiLineString != null) {
      final LineString[] newLineStrings = new LineString[multiLineString.getNumGeometries()];
      for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
        final LineString line = (LineString)multiLineString.getGeometry(i);
        final LineString newLineString = perform(line);
        addUserData(line, newLineString);
        newLineStrings[i] = newLineString;
      }
      final MultiLineString newMultiLineString = geometryFactory.createMultiLineString(newLineStrings);
      addUserData(multiLineString, newMultiLineString);
      return newMultiLineString;
    } else {
      return null;
    }
  }

  public Geometry perform(final MultiPoint multiPoint) {
    if (multiPoint != null) {
      final Point[] newPoints = new Point[multiPoint.getNumGeometries()];
      for (int i = 0; i < multiPoint.getNumGeometries(); i++) {
        final Point point = (Point)multiPoint.getGeometry(i);
        final Point newPoint = perform(point);
        addUserData(point, newPoint);
        newPoints[i] = newPoint;
      }
      final MultiPoint newMultiPoint = geometryFactory.createMultiPoint(newPoints);
      addUserData(multiPoint, newMultiPoint);
      return newMultiPoint;
    } else {
      return null;
    }
  }

  public MultiPolygon perform(final MultiPolygon multiPolygon) {
    if (multiPolygon != null) {
      final Polygon[] newPolygons = new Polygon[multiPolygon.getNumGeometries()];
      for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
        final Polygon polygon = (Polygon)multiPolygon.getGeometry(i);
        final Polygon newPolygon = perform(polygon);
        addUserData(polygon, newPolygon);
        newPolygons[i] = newPolygon;
      }
      final MultiPolygon newMultiPolygon = geometryFactory.createMultiPolygon(newPolygons);
      addUserData(multiPolygon, newMultiPolygon);
      return newMultiPolygon;
    } else {
      return null;
    }
  }

  public Point perform(final Point point) {
    if (point != null) {
      final CoordinatesList newCoordinate = perform(point.getCoordinatesList());
      final Point newPoint = geometryFactory.point(newCoordinate);
      addUserData(point, newPoint);
      return newPoint;
    } else {
      return null;
    }
  }

  public Polygon perform(final Polygon polygon) {

    final LinearRing shell = (LinearRing)polygon.getExteriorRing();
    final LinearRing newShell = perform(shell);
    final LinearRing[] newHoles = new LinearRing[polygon.getNumInteriorRing()];
    for (int i = 0; i < newHoles.length; i++) {
      final LinearRing hole = (LinearRing)polygon.getInteriorRingN(i);
      newHoles[i] = perform(hole);
    }
    final Polygon newPolygon = geometryFactory.createPolygon(newShell, newHoles);
    addUserData(newPolygon, polygon);
    return newPolygon;

  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Geometry> T perform(final T geometry) {
    try {
      final Method method = getClass().getMethod("perform", geometry.getClass());
      return (T)method.invoke(this, geometry);
    } catch (final NoSuchMethodException e) {
      return geometry;
    } catch (final IllegalAccessException e) {
      throw new RuntimeException("Unable to invoke method", e);
    } catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException)cause;
      } else if (cause instanceof Error) {
        throw (Error)cause;
      } else {
        throw new RuntimeException(cause);
      }
    }
  }

}
