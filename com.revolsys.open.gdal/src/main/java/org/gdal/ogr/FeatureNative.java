/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.40
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.gdal.ogr;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/* This class enables to finalize native resources associated with the object */
/* without needing a finalize() method */

class FeatureNative extends WeakReference {
  static private ReferenceQueue refQueue = new ReferenceQueue();

  static private Set refList = Collections.synchronizedSet(new HashSet());

  static private Thread cleanupThread = null;

  /* We start a cleanup thread in daemon mode */
  /* If we can't, we'll cleanup garbaged features at creation time */
  static {
    cleanupThread = new Thread() {
      @Override
      public void run() {
        while (true) {
          try {
            final FeatureNative nativeObject = (FeatureNative)refQueue.remove();
            if (nativeObject != null) {
              nativeObject.delete();
            }
          } catch (final InterruptedException ie) {
          }
        }
      }
    };
    try {
      cleanupThread.setName("Feature" + "NativeObjectsCleaner");
      cleanupThread.setDaemon(true);
      cleanupThread.start();
    } catch (final SecurityException se) {
      // System.err.println("could not start daemon thread");
      cleanupThread = null;
    }
  }

  private long swigCPtr;

  public FeatureNative(final Feature javaObject, final long cPtr) {
    super(javaObject, refQueue);

    if (cleanupThread == null) {
      /* We didn't manage to have a daemon cleanup thread */
      /* so let's clean manually */
      while (true) {
        final FeatureNative nativeObject = (FeatureNative)refQueue.poll();
        if (nativeObject != null) {
          nativeObject.delete();
        } else {
          break;
        }
      }
    }

    refList.add(this);

    this.swigCPtr = cPtr;
  }

  public void delete() {
    refList.remove(this);
    if (this.swigCPtr != 0) {
      ogrJNI.delete_Feature(this.swigCPtr);
    }
    this.swigCPtr = 0;
  }

  public void dontDisposeNativeResources() {
    refList.remove(this);
    this.swigCPtr = 0;
  }

}
