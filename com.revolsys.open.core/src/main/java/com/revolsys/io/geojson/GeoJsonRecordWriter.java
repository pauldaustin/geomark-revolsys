package com.revolsys.io.geojson;

import java.io.BufferedWriter;
import java.io.Writer;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.io.AbstractWriter;
import com.revolsys.io.IoConstants;
import com.revolsys.io.json.JsonWriter;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryCollection;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.LineString;
import com.revolsys.jts.geom.MultiLineString;
import com.revolsys.jts.geom.MultiPoint;
import com.revolsys.jts.geom.MultiPolygon;
import com.revolsys.jts.geom.Point;
import com.revolsys.jts.geom.Polygon;
import com.revolsys.math.Angle;
import com.revolsys.util.MathUtil;

public class GeoJsonRecordWriter extends AbstractWriter<Record>
  implements GeoJsonConstants {

  boolean initialized = false;

  private int srid = -1;

  /** The writer */
  private JsonWriter out;

  private boolean singleObject;

  private final boolean cogo;

  public GeoJsonRecordWriter(final Writer out) {
    this(out, false);
  }

  public GeoJsonRecordWriter(final Writer out, final boolean cogo) {
    this.out = new JsonWriter(new BufferedWriter(out));
    this.out.setIndent(true);
    this.cogo = cogo;
  }

  /**
   * Closes the underlying reader.
   */
  @Override
  public void close() {
    if (out != null) {
      try {
        writeFooter();
      } finally {
        out.close();
        out = null;
      }
    }
  }

  private void coordinate(final Point coordinates) {
    out.print('[');
    for (int axisIndex = 0; axisIndex < coordinates.getAxisCount(); axisIndex++) {
      if (axisIndex > 0) {
        out.print(',');
      }
      final double value = coordinates.getCoordinate(axisIndex);
      out.value(value);
    }
    out.print(']');
  }

  private void coordinates(final LineString line) {
    out.startList(false);
    out.indent();
    for (int i = 0; i < line.getVertexCount(); i++) {
      if (i > 0) {
        out.endAttribute();
        out.indent();
      }
      double x = line.getX(i);
      double y = line.getY(i);

      if (cogo && i > 0) {
        final double currentX = x;
        final double previousX = line.getX(i - 1);
        final double previousY = line.getY(i - 1);
        x = MathUtil.distance(previousX, previousY, currentX, y);
        y = Angle.angleNorthDegrees(previousX, previousY, currentX, y);
      }

      out.print('[');
      out.value(x);
      out.print(',');
      out.value(y);

      for (int axisIndex = 2; axisIndex < line.getAxisCount(); axisIndex++) {
        out.print(',');
        final double value = line.getCoordinate(i, axisIndex);
        out.value(value);
      }
      out.print(']');
    }
    out.endList();
  }

  public void coordinates(final Point point) {
    coordinate(point);
  }

  public void coordinates(final Polygon polygon) {
    out.startList(false);
    out.indent();

    final LineString exteriorRing = polygon.getExteriorRing();
    coordinates(exteriorRing);
    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
      final LineString interiorRing = polygon.getInteriorRing(i);
      out.endAttribute();
      out.indent();
      coordinates(interiorRing);
    }

    out.endList();
  }

  @Override
  public void flush() {
    out.flush();
  }

  private void geometry(final Geometry geometry) {
    out.startObject();
    if (geometry instanceof Point) {
      final Point point = (Point)geometry;
      point(point);
    } else if (geometry instanceof LineString) {
      final LineString line = (LineString)geometry;
      line(line);
    } else if (geometry instanceof Polygon) {
      final Polygon polygon = (Polygon)geometry;
      polygon(polygon);
    } else if (geometry instanceof MultiPoint) {
      final MultiPoint multiPoint = (MultiPoint)geometry;
      multiPoint(multiPoint);
    } else if (geometry instanceof MultiLineString) {
      final MultiLineString multiLine = (MultiLineString)geometry;
      multiLineString(multiLine);
    } else if (geometry instanceof MultiPolygon) {
      final MultiPolygon multiPolygon = (MultiPolygon)geometry;
      multiPolygon(multiPolygon);
    } else if (geometry instanceof GeometryCollection) {
      final GeometryCollection geometryCollection = (GeometryCollection)geometry;
      geometryCollection(geometryCollection);
    }
    out.endObject();
  }

  private void geometryCollection(final GeometryCollection geometryCollection) {
    type(GEOMETRY_COLLECTION);

    out.endAttribute();
    out.label(GEOMETRIES);
    out.startList();
    final int numGeometries = geometryCollection.getGeometryCount();
    if (numGeometries > 0) {
      geometry(geometryCollection.getGeometry(0));
      for (int i = 1; i < numGeometries; i++) {
        final Geometry geometry = geometryCollection.getGeometry(i);
        out.endAttribute();
        geometry(geometry);
      }
    }
    out.endList();
  }

  public boolean isCogo() {
    return cogo;
  }

  private void line(final LineString line) {
    if (cogo) {
      type(COGO_LINE_STRING);
    } else {
      type(LINE_STRING);
    }
    out.endAttribute();
    out.label(COORDINATES);
    if (line.isEmpty()) {
      out.startList();
      out.endList();
    } else {
      coordinates(line);
    }
  }

  private void multiLineString(final MultiLineString multiLineString) {
    if (cogo) {
      type(COGO_LINE_STRING);
    } else {
      type(MULTI_LINE_STRING);
    }

    out.endAttribute();
    out.label(COORDINATES);
    out.startList();
    out.indent();
    final int numGeometries = multiLineString.getGeometryCount();
    if (numGeometries > 0) {
      coordinates((LineString)multiLineString.getGeometry(0));
      for (int i = 1; i < numGeometries; i++) {
        final LineString lineString = (LineString)multiLineString.getGeometry(i);
        out.endAttribute();
        out.indent();
        coordinates(lineString);
      }
    }
    out.endList();
  }

  private void multiPoint(final MultiPoint multiPoint) {
    type(MULTI_POINT);

    out.endAttribute();
    out.label(COORDINATES);
    out.startList();
    out.indent();
    final int numGeometries = multiPoint.getGeometryCount();
    if (numGeometries > 0) {
      coordinates((Point)multiPoint.getGeometry(0));
      for (int i = 1; i < numGeometries; i++) {
        final Point point = (Point)multiPoint.getGeometry(i);
        out.endAttribute();
        out.indent();
        coordinates(point);
      }
    }
    out.endList();
  }

  private void multiPolygon(final MultiPolygon multiPolygon) {
    if (cogo) {
      type(COGO_MULTI_POLYGON);
    } else {
      type(MULTI_POLYGON);
    }

    out.endAttribute();
    out.label(COORDINATES);
    out.startList();
    out.indent();
    final int numGeometries = multiPolygon.getGeometryCount();
    if (numGeometries > 0) {
      coordinates((Polygon)multiPolygon.getGeometry(0));
      for (int i = 1; i < numGeometries; i++) {
        final Polygon polygon = (Polygon)multiPolygon.getGeometry(i);
        out.endAttribute();
        out.indent();
        coordinates(polygon);
      }
    }
    out.endList();
  }

  private void point(final Point point) {
    type(POINT);
    out.endAttribute();
    out.label(COORDINATES);
    if (point.isEmpty()) {
      out.startList();
      out.endList();
    } else {
      coordinates(point);
    }
  }

  private void polygon(final Polygon polygon) {
    if (cogo) {
      type(COGO_POLYGON);
    } else {
      type(POLYGON);
    }

    out.endAttribute();
    out.label(COORDINATES);
    if (polygon.isEmpty()) {
      out.startList();
      out.endList();
    } else {
      coordinates(polygon);
    }
  }

  private void srid(final int srid) {
    final String urn = URN_OGC_DEF_CRS_EPSG + srid;
    out.label(CRS);
    out.startObject();
    type(NAME);
    out.endAttribute();
    out.label(PROPERTIES);
    out.startObject();
    out.label(NAME);
    out.value(urn);
    out.endObject();
    out.endObject();
  }

  private void type(final String type) {
    out.label(TYPE);
    out.value(type);
  }

  @Override
  public void write(final Record object) {
    if (initialized) {
      out.endAttribute();
    } else {
      writeHeader();
      initialized = true;
    }
    out.startObject();
    type(FEATURE);
    Geometry mainGeometry = object.getGeometryValue();
    final GeometryFactory geometryFactory = getProperty(IoConstants.GEOMETRY_FACTORY);
    if (geometryFactory != null) {
      mainGeometry = mainGeometry.convert(geometryFactory);
    }
    writeSrid(mainGeometry);
    final RecordDefinition recordDefinition = object.getRecordDefinition();
    final int geometryIndex = recordDefinition.getGeometryAttributeIndex();
    boolean geometryWritten = false;
    out.endAttribute();
    out.label(GEOMETRY);
    if (mainGeometry != null) {
      geometryWritten = true;
      geometry(mainGeometry);
    }
    if (!geometryWritten) {
      out.value(null);
    }
    out.endAttribute();
    out.label(PROPERTIES);
    out.startObject();
    final int numAttributes = recordDefinition.getAttributeCount();
    if (numAttributes > 1 || numAttributes == 1 && geometryIndex == -1) {
      int lastIndex = numAttributes - 1;
      if (lastIndex == geometryIndex) {
        lastIndex--;
      }
      for (int i = 0; i < numAttributes; i++) {
        if (i != geometryIndex) {
          final String name = recordDefinition.getAttributeName(i);
          final Object value = object.getValue(i);
          out.label(name);
          if (value instanceof Geometry) {
            final Geometry geometry = (Geometry)value;
            geometry(geometry);
          } else {
            out.value(value);
          }
          if (i < lastIndex) {
            out.endAttribute();
          }
        }
      }
    }
    out.endObject();
    out.endObject();
  }

  private void writeFooter() {
    if (!singleObject) {
      out.endList();
      out.endObject();
    }
    final String callback = getProperty(IoConstants.JSONP_PROPERTY);
    if (callback != null) {
      out.print(");");
    }
  }

  private void writeHeader() {
    final String callback = getProperty(IoConstants.JSONP_PROPERTY);
    if (callback != null) {
      out.print(callback);
      out.print('(');
    }
    singleObject = Boolean.TRUE.equals(getProperty(IoConstants.SINGLE_OBJECT_PROPERTY));
    if (!singleObject) {
      out.startObject();
      type(FEATURE_COLLECTION);
      srid = writeSrid();
      out.endAttribute();
      out.label(FEATURES);
      out.startList();
    }
  }

  private int writeSrid() {
    final GeometryFactory geometryFactory = getProperty(IoConstants.GEOMETRY_FACTORY);
    return writeSrid(geometryFactory);
  }

  protected int writeSrid(
    final GeometryFactory geometryFactory) {
    if (geometryFactory != null) {
      final int srid = geometryFactory.getSrid();
      if (srid != 0 && srid != this.srid) {
        out.endAttribute();
        srid(srid);
        return srid;
      }
    }
    return -1;
  }

  private void writeSrid(final Geometry geometry) {
    if (geometry != null) {
      final GeometryFactory geometryFactory = geometry.getGeometryFactory();
      writeSrid(geometryFactory);
    }
  }
}