package com.revolsys.gis.graph.visitor;

import java.util.List;

import com.revolsys.collection.Visitor;
import com.revolsys.filter.Filter;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.visitor.CreateListVisitor;
import com.revolsys.gis.data.visitor.NestedVisitor;
import com.revolsys.gis.graph.Edge;
import com.revolsys.gis.graph.Graph;
import com.revolsys.gis.graph.Node;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class EdgeWithinDistance<T> extends NestedVisitor<Edge<T>> implements
  Filter<Edge<T>> {
  public static <T> List<Edge<T>> edgesWithiDistance(
    final Graph<T> graph,
    final Geometry geometry,
    final double maxDistance) {
    final CreateListVisitor<Edge<T>> results = new CreateListVisitor<Edge<T>>();
    final Envelope env = new Envelope(geometry.getEnvelopeInternal());
    env.expandBy(maxDistance);
    graph.getEdgeIndex().query(env,
      new EdgeWithinDistance<T>(geometry, maxDistance, results));
    return results.getList();
  }

  public static <T> List<Edge<T>> edgesWithinDistance(
    final Graph<T> graph,
    final Coordinates point,
    final double maxDistance) {
    final GeometryFactory geometryFactory = GeometryFactory.getFactory();
    final Geometry geometry = geometryFactory.createPoint(point);
    return edgesWithiDistance(graph, geometry, maxDistance);

  }

  public static <T> List<Edge<T>> edgesWithinDistance(
    final Graph<T> graph,
    final Node<T> node,
    final double maxDistance) {
    final GeometryFactory geometryFactory = GeometryFactory.getFactory();
    final Coordinates coordinate = node;
    final Geometry geometry = geometryFactory.createPoint(coordinate);
    return edgesWithiDistance(graph, geometry, maxDistance);

  }

  private final Geometry geometry;

  private final double maxDistance;

  public EdgeWithinDistance(final Geometry geometry, final double maxDistance) {
    this.geometry = geometry;
    this.maxDistance = maxDistance;
  }

  public EdgeWithinDistance(final Geometry geometry, final double maxDistance,
    final Visitor<Edge<T>> matchVisitor) {
    super(matchVisitor);
    this.geometry = geometry;
    this.maxDistance = maxDistance;
  }

  public boolean accept(final Edge<T> edge) {
    final LineString line = edge.getLine();
    final double distance = line.distance(geometry);
    if (distance <= maxDistance) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean visit(final Edge<T> edge) {
    if (accept(edge)) {
      super.visit(edge);
    }
    return true;
  }
}
