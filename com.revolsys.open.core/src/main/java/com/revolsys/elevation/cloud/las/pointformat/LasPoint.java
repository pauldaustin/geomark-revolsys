package com.revolsys.elevation.cloud.las.pointformat;

import com.revolsys.collection.map.LinkedHashMapEx;
import com.revolsys.collection.map.MapEx;
import com.revolsys.elevation.cloud.las.LasPointCloud;
import com.revolsys.geometry.model.Point;
import com.revolsys.io.channels.ChannelReader;
import com.revolsys.io.channels.ChannelWriter;
import com.revolsys.io.map.MapSerializer;

public interface LasPoint extends Point, MapSerializer {

  @Override
  LasPoint clone();

  default int getBlue() {
    return 0;
  }

  short getClassification();

  byte getClassificationByte();

  default double getGpsTime() {
    return 315964800;
  }

  default int getGreen() {
    return 0;
  }

  int getIntensity();

  byte getNumberOfReturns();

  LasPointFormat getPointFormat();

  default int getPointFormatId() {
    return getPointFormat().getId();
  }

  int getPointSourceID();

  default int getRed() {
    return 0;
  }

  byte getReturnByte();

  byte getReturnNumber();

  double getScanAngleDegrees();

  byte getScanAngleRank();

  byte getScannerChannel();

  short getUserData();

  int getXInt();

  int getYInt();

  int getZInt();

  boolean isEdgeOfFlightLine();

  boolean isKeyPoint();

  default boolean isPointFormat(final LasPointFormat pointFormat) {
    return pointFormat.equals(getPointFormat());
  }

  boolean isScanDirectionFlag();

  boolean isSynthetic();

  boolean isWithheld();

  void read(LasPointCloud pointCloud, ChannelReader reader);

  default LasPoint setBlue(final int blue) {
    return this;
  }

  LasPoint setClassification(short classification);

  LasPoint setClassificationByte(byte classificationByte);

  LasPoint setEdgeOfFlightLine(boolean edgeOfFlightLine);

  default LasPoint setGpsTime(final double gpsTime) {
    return this;
  }

  default LasPoint setGreen(final int green) {
    return this;
  }

  LasPoint setIntensity(int intensity);

  LasPoint setKeyPoint(boolean keyPoint);

  LasPoint setNumberOfReturns(byte numberOfReturns);

  LasPoint setPointSourceID(int pointSourceID);

  default LasPoint setRed(final int red) {
    return this;
  }

  LasPoint setReturnByte(byte returnByte);

  LasPoint setReturnNumber(byte returnNumber);

  LasPoint setScanAngleRank(byte scanAngleRank);

  LasPoint setScanDirectionFlag(boolean scanDirectionFlag);

  LasPoint setScannerChannel(byte scannerChannel);

  LasPoint setSynthetic(boolean synthetic);

  LasPoint setUserData(short userData);

  LasPoint setWithheld(boolean withheld);

  void setXYZ(int x, int y, int z);

  @Override
  default MapEx toMap() {
    final MapEx map = new LinkedHashMapEx();
    return map;
  }

  void writeLasPoint(ChannelWriter out);
}
