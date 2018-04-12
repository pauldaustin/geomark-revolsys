package com.revolsys.geometry.cs.gridshift.gsb;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.grid.FloatArrayGrid;

public class BinaryGridShiftGrid {

  private final List<BinaryGridShiftGrid> grids = new ArrayList<>();

  private final FloatArrayGrid latAccuracies;

  private final FloatArrayGrid latShifts;

  private final FloatArrayGrid lonAccuracies;

  private final FloatArrayGrid lonShifts;

  private final String name;

  private final String parentName;

  private final double minY;

  private final double maxY;

  private final double minX;

  private final double maxX;

  private final BinaryGridShiftFile file;

  @SuppressWarnings("unused")
  public BinaryGridShiftGrid(final BinaryGridShiftFile file, final boolean loadAccuracy) {
    this.file = file;
    this.name = file.readRecordString();
    this.parentName = file.readRecordString();
    final String created = file.readRecordString();
    final String updated = file.readRecordString();
    this.minY = file.readRecordDouble();
    this.maxY = file.readRecordDouble();
    this.minX = file.readRecordDouble();
    this.maxX = file.readRecordDouble();
    final double gridCellSizeY = file.readRecordDouble();
    final double gridCellSizeX = file.readRecordDouble();
    if (gridCellSizeX != gridCellSizeY) {
      throw new IllegalStateException(
        "latInterval=" + gridCellSizeY + " != lonInterval=" + gridCellSizeX);
    }
    final int gridWidth = 1 + (int)((this.maxX - this.minX) / gridCellSizeY);
    final int gridHeight = 1 + (int)((this.maxY - this.minY) / gridCellSizeX);
    final int nodeCount = file.readRecordInt();
    if (nodeCount != gridWidth * gridHeight) {
      throw new IllegalStateException(
        "BinaryGridShiftGrid " + this.name + " has inconsistent grid dimensions");
    }
    final float[] latShifts = new float[nodeCount];
    final float[] lonShifts = new float[nodeCount];
    if (loadAccuracy) {
      final float[] latAccuracies = new float[nodeCount];
      final float[] lonAccuracies = new float[nodeCount];
      for (int i = 0; i < nodeCount; i++) {
        latShifts[i] = file.readFloat();
        lonShifts[i] = file.readFloat();
        latAccuracies[i] = file.readFloat();
        lonAccuracies[i] = file.readFloat();
      }
      this.lonAccuracies = new FloatArrayGrid(this.minX, this.minY, gridWidth, gridHeight,
        gridCellSizeY, lonAccuracies);
      this.latAccuracies = new FloatArrayGrid(this.minX, this.minY, gridWidth, gridHeight,
        gridCellSizeY, latAccuracies);
    } else {
      for (int i = 0; i < nodeCount; i++) {
        latShifts[i] = file.readFloat();
        lonShifts[i] = file.readFloat();
        final float latAccuracy = file.readFloat();
        final float lonAccuracy = file.readFloat();
      }
      this.lonAccuracies = null;
      this.latAccuracies = null;
    }
    this.lonShifts = new FloatArrayGrid(this.minX, this.minY, gridWidth, gridHeight, gridCellSizeX,
      lonShifts);
    this.latShifts = new FloatArrayGrid(this.minX, this.minY, gridWidth, gridHeight, gridCellSizeX,
      latShifts);
  }

  public void addGrid(final BinaryGridShiftGrid grid) {
    this.grids.add(grid);
  }

  public boolean covers(final double x, final double y) {
    return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY;
  }

  public BoundingBox getBoundingBox() {
    final GeometryFactory geometryFactory = this.file.getFromCoordinateSystem()
      .getGeometryFactoryFloating(2);
    return geometryFactory.newBoundingBox(-this.minX / 3600, this.minY / 3600, -this.maxX / 3600,
      this.maxY / 3600);
  }

  public BinaryGridShiftGrid getGrid(final double lonSeconds, final double latSeconds) {
    if (covers(lonSeconds, latSeconds)) {
      for (final BinaryGridShiftGrid grid : this.grids) {
        final BinaryGridShiftGrid childGrid = grid.getGrid(lonSeconds, latSeconds);
        if (childGrid != null) {
          return childGrid;
        }
      }
      return this;
    } else {
      return null;
    }
  }

  public double getLatAccuracy(final double lon, final double lat) {
    return this.latAccuracies.getValueBilinear(lon, lat);
  }

  public double getLatShift(final double lon, final double lat) {
    return this.latShifts.getValueBilinear(lon, lat);
  }

  public double getLonAccuracy(final double lon, final double lat) {
    return this.lonAccuracies.getValueBilinear(lon, lat);
  }

  public double getLonShift(final double lon, final double lat) {
    return this.lonShifts.getValueBilinear(lon, lat);
  }

  public String getName() {
    return this.name;
  }

  public String getParentName() {
    return this.parentName;
  }

  public boolean hasParent() {
    return !this.parentName.equalsIgnoreCase("NONE");
  }

  @Override
  public String toString() {
    return this.name;
  }

}
