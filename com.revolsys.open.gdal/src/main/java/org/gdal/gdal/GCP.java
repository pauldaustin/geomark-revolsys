/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.40
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.gdal.gdal;

public class GCP {
  protected static long getCPtr(final GCP obj) {
    return obj == null ? 0 : obj.swigCPtr;
  }

  private long swigCPtr;

  protected boolean swigCMemOwn;

  public GCP(final double x, final double y, final double pixel, final double line) {
    this(x, y, 0.0, pixel, line, "", "");
  }

  public GCP(final double x, final double y, final double z, final double pixel,
    final double line) {
    this(x, y, z, pixel, line, "", "");
  }

  public GCP(final double x, final double y, final double z, final double pixel, final double line,
    final String info, final String id) {
    this(gdalJNI.new_GCP(x, y, z, pixel, line, info, id), true);
  }

  public GCP(final double x, final double y, final double pixel, final double line,
    final String info, final String id) {
    this(x, y, 0.0, pixel, line, info, id);
  }

  protected GCP(final long cPtr, final boolean cMemoryOwn) {
    if (cPtr == 0) {
      throw new RuntimeException();
    }
    this.swigCMemOwn = cMemoryOwn;
    this.swigCPtr = cPtr;
  }

  public synchronized void delete() {
    if (this.swigCPtr != 0) {
      if (this.swigCMemOwn) {
        this.swigCMemOwn = false;
        gdalJNI.delete_GCP(this.swigCPtr);
      }
      this.swigCPtr = 0;
    }
  }

  @Override
  protected void finalize() {
    delete();
  }

  public double getGCPLine() {
    return gdalJNI.GCP_GCPLine_get(this.swigCPtr, this);
  }

  public double getGCPPixel() {
    return gdalJNI.GCP_GCPPixel_get(this.swigCPtr, this);
  }

  public double getGCPX() {
    return gdalJNI.GCP_GCPX_get(this.swigCPtr, this);
  }

  public double getGCPY() {
    return gdalJNI.GCP_GCPY_get(this.swigCPtr, this);
  }

  public double getGCPZ() {
    return gdalJNI.GCP_GCPZ_get(this.swigCPtr, this);
  }

  public String getId() {
    return gdalJNI.GCP_Id_get(this.swigCPtr, this);
  }

  public String getInfo() {
    return gdalJNI.GCP_Info_get(this.swigCPtr, this);
  }

  public void setGCPLine(final double value) {
    gdalJNI.GCP_GCPLine_set(this.swigCPtr, this, value);
  }

  public void setGCPPixel(final double value) {
    gdalJNI.GCP_GCPPixel_set(this.swigCPtr, this, value);
  }

  public void setGCPX(final double value) {
    gdalJNI.GCP_GCPX_set(this.swigCPtr, this, value);
  }

  public void setGCPY(final double value) {
    gdalJNI.GCP_GCPY_set(this.swigCPtr, this, value);
  }

  public void setGCPZ(final double value) {
    gdalJNI.GCP_GCPZ_set(this.swigCPtr, this, value);
  }

  public void setId(final String value) {
    gdalJNI.GCP_Id_set(this.swigCPtr, this, value);
  }

  public void setInfo(final String value) {
    gdalJNI.GCP_Info_set(this.swigCPtr, this, value);
  }

}
