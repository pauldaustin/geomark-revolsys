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

package com.revolsys.jts.planargraph;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.revolsys.jts.geom.Point;

/**
 * A map of {@link Node}s, indexed by the coordinate of the node.
 *
 * @version 1.7
 */
public class NodeMap

{

  private final Map<Point, Node> nodeMap = new TreeMap<>();

  /**
   * Constructs a NodeMap without any Nodes.
   */
  public NodeMap() {
  }

  /**
   * Adds a node to the map, replacing any that is already at that location.
   * @return the added node
   */
  public Node add(final Node n) {
    this.nodeMap.put(n.getCoordinate(), n);
    return n;
  }

  /**
   * Returns the Node at the given location, or null if no Node was there.
   */
  public Node find(final Point coord) {
    return this.nodeMap.get(coord);
  }

  /**
   * Returns an Iterator over the Nodes in this NodeMap, sorted in ascending order
   * by angle with the positive x-axis.
   */
  public Iterator<Node> iterator() {
    return this.nodeMap.values().iterator();
  }

  public Collection<Node> nodes() {
    return this.nodeMap.values();
  }

  /**
   * Removes the Node at the given location, and returns it (or null if no Node was there).
   */
  public Node remove(final Point pt) {
    return this.nodeMap.remove(pt);
  }

  /**
   * Returns the Nodes in this NodeMap, sorted in ascending order
   * by angle with the positive x-axis.
   */
  public Collection<Node> values() {
    return this.nodeMap.values();
  }

}
