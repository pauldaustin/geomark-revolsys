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

package com.revolsys.jts.operation.distance;

import com.revolsys.gis.model.coordinates.AbstractCoordinates;
import com.revolsys.jts.algorithm.CGAlgorithms;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.Coordinate;
import com.revolsys.jts.geom.Coordinates;
import com.revolsys.jts.geom.CoordinatesList;
import com.revolsys.jts.geom.Envelope;
import com.revolsys.jts.geom.Geometry;

/**
 * Represents a sequence of facets (points or line segments)
 * of a {@link Geometry}
 * specified by a subsequence of a {@link CoordinatesList}.
 * 
 * @author Martin Davis
 *
 */
public class FacetSequence
{
  private CoordinatesList pts;
  private int start;
  private int end;
  
  // temporary Coordinates to materialize points from the CoordinatesList
  private Coordinates pt = new Coordinate();
  private Coordinates seqPt = new Coordinate();
  
  /**
   * Creates a new section based on a CoordinatesList.
   * 
   * @param pts the sequence holding the points in the section
   * @param start the index of the start point
   * @param end the index of the end point + 1
   */
  public FacetSequence(CoordinatesList pts, int start, int end) 
  {
    this.pts = pts;
    this.start = start;
    this.end = end;
  }
  
  /**
   * Creates a new sequence for a single point from a CoordinatesList.
   * 
   * @param pts the sequence holding the points in the facet sequence
   * @param start the index of the point
   */
  public FacetSequence(CoordinatesList pts, int start) 
  {
    this.pts = pts;
    this.start = start;
    this.end = start + 1;
  }
  
  public BoundingBox getEnvelope()
  {
    Envelope env = new Envelope();
    for (int i = start; i < end; i++) {
      env.expandToInclude(pts.getX(i), pts.getY(i));
    }
    return env;
  }
  
  public int size()
  {
    return end - start;
  }
  
  public Coordinates getCoordinate(int index)
  {
    return pts.getCoordinate(start + index);
  }
  
  public boolean isPoint()
  {
    return end - start == 1;
  }
  

  public double distance(FacetSequence facetSeq)
  {
    boolean isPoint = isPoint();
    boolean isPointOther = facetSeq.isPoint();
    
    if (isPoint && isPointOther) {
      pts.getCoordinate(start, pt);
      facetSeq.pts.getCoordinate(facetSeq.start, seqPt);
      return pt.distance(seqPt);
    }
    else if (isPoint) {
      pts.getCoordinate(start, pt);      
      return computePointLineDistance(pt, facetSeq);
    }
    else if (isPointOther) {
      facetSeq.pts.getCoordinate(facetSeq.start, seqPt);
      return computePointLineDistance(seqPt, this);
    }
    return computeLineLineDistance(facetSeq);
    
  }
  
  // temporary Coordinates to materialize points from the CoordinatesList
  private Coordinates p0 = new Coordinate();
  private Coordinates p1 = new Coordinate();
  private Coordinates q0 = new Coordinate();
  private Coordinates q1 = new Coordinate();

  private double computeLineLineDistance(FacetSequence facetSeq)
  {
    // both linear - compute minimum segment-segment distance
    double minDistance = Double.MAX_VALUE;

    for (int i = start; i < end - 1; i++) {
      for (int j = facetSeq.start; j < facetSeq.end - 1; j++) {
        pts.getCoordinate(i, p0);
        pts.getCoordinate(i + 1, p1);
        facetSeq.pts.getCoordinate(j, q0);
        facetSeq.pts.getCoordinate(j + 1, q1);
        
        double dist = CGAlgorithms.distanceLineLine(p0, p1, q0, q1);
        if (dist == 0.0) 
          return 0.0;
        if (dist < minDistance) {
          minDistance = dist;
        }
      }
    }
    return minDistance;
  }

  private double computePointLineDistance(Coordinates pt, FacetSequence facetSeq) 
  {
    double minDistance = Double.MAX_VALUE;

    for (int i = facetSeq.start; i < facetSeq.end - 1; i++) {
      facetSeq.pts.getCoordinate(i, q0);
      facetSeq.pts.getCoordinate(i + 1, q1);
      double dist = CGAlgorithms.distancePointLine(pt, q0, q1);
      if (dist == 0.0) return 0.0;
      if (dist < minDistance) {
        minDistance = dist;
      }
    }
    return minDistance;
  }
  
  public String toString()
  {
    StringBuffer buf = new StringBuffer();
    buf.append("LINESTRING ( ");
    Coordinates p = new Coordinate();
    for (int i = start; i < end; i++) {
      if (i > start)
        buf.append(", ");
      pts.getCoordinate(i, p);
      buf.append(p.getX() + " " + p.getY());
    }
    buf.append(" )");
    return buf.toString();
  }
}