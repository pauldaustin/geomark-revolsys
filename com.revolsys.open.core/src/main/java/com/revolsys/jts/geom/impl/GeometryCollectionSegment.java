package com.revolsys.jts.geom.impl;

import java.util.NoSuchElementException;

import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.segment.AbstractSegment;
import com.revolsys.jts.geom.segment.Segment;

public class GeometryCollectionSegment extends AbstractSegment {

  private int partIndex = -1;

  private Segment segment;

  public GeometryCollectionSegment(final Geometry geometry,
    final int... segmentId) {
    super(geometry);
    setSegmentId(segmentId);
  }

  public Geometry getGeometryCollection() {
    return getGeometry();
  }

  @Override
  public int getPartIndex() {
    return super.getPartIndex();
  }

  @Override
  public int[] getSegmentId() {
    if (partIndex < 0) {
      return new int[] {
        -1
      };
    } else if (segment == null) {
      return new int[] {
        partIndex
      };
    } else {
      final int[] partSegmentId = segment.getSegmentId();
      final int[] segmentId = new int[partSegmentId.length + 1];
      segmentId[0] = partIndex;
      System.arraycopy(partSegmentId, 0, segmentId, 1, partSegmentId.length);
      return segmentId;
    }
  }

  @Override
  public double getValue(final int index, final int axisIndex) {
    if (segment == null) {
      return Double.NaN;
    } else {
      return segment.getValue(index, axisIndex);
    }
  }

  @Override
  public boolean hasNext() {
    if (this.partIndex == -2) {
      return false;
    } else {
      final Geometry geometryCollection = getGeometryCollection();
      int partIndex = this.partIndex;
      Segment segment = this.segment;
      if (segment != null && !segment.hasNext()) {
        partIndex++;
        segment = null;
      }
      while (segment == null
        && partIndex < geometryCollection.getGeometryCount()) {
        if (partIndex >= 0) {
          final Geometry part = geometryCollection.getGeometry(partIndex);
          if (part != null) {
            segment = (Segment)part.segments().iterator();
            if (segment.hasNext()) {
              return true;
            } else {
              segment = null;
            }
          }
        }
        if (partIndex > -2) {
          partIndex++;
        }
      }
      if (segment == null) {
        return false;
      } else {
        return segment.hasNext();
      }
    }
  }

  @Override
  public Segment next() {
    if (this.partIndex == -2) {
      throw new NoSuchElementException();
    } else {
      final Geometry geometryCollection = getGeometryCollection();
      if (segment != null && !segment.hasNext()) {
        partIndex++;
        segment = null;
      }
      while (segment == null
        && partIndex < geometryCollection.getGeometryCount()) {
        if (partIndex >= 0) {
          final Geometry part = geometryCollection.getGeometry(partIndex);
          if (part != null) {
            segment = (Segment)part.segments().iterator();
            if (segment.hasNext()) {
              return segment.next();
            } else {
              segment = null;
            }
          }
        }
        if (partIndex > -2) {
          this.partIndex++;
        }
      }
      if (segment != null && segment.hasNext()) {
        return segment.next();
      } else {
        throw new NoSuchElementException();
      }
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Removing vertices not supported");
  }

  public void setSegmentId(final int[] segmentId) {
    this.segment = null;
    if (segmentId.length > 0) {
      this.partIndex = segmentId[0];
      final Geometry geometryCollection = getGeometryCollection();
      if (partIndex >= 0 && partIndex < geometryCollection.getGeometryCount()) {
        final Geometry part = geometryCollection.getGeometry(partIndex);
        if (part != null) {
          final int[] partSegmentId = new int[segmentId.length - 1];
          System.arraycopy(segmentId, 1, partSegmentId, 0, partSegmentId.length);
          this.segment = part.getSegment(partSegmentId);
        }
      }
    } else {
      this.partIndex = -2;
    }
  }
}