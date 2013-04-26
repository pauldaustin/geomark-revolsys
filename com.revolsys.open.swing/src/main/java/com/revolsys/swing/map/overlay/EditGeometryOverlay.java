package com.revolsys.swing.map.overlay;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.SwingUtilities;

import com.revolsys.famfamfam.silk.SilkIconLoader;
import com.revolsys.gis.algorithm.index.PointQuadTree;
import com.revolsys.gis.algorithm.index.quadtree.QuadTree;
import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.CoordinatesUtil;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.gis.model.coordinates.list.ListCoordinatesList;
import com.revolsys.gis.model.geometry.LineSegment;
import com.revolsys.gis.model.geometry.util.GeometryEditUtil;
import com.revolsys.gis.model.geometry.util.IndexedLineSegment;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.map.MapPanel;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.Layer;
import com.revolsys.swing.map.layer.LayerGroup;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.layer.dataobject.DataObjectLayer;
import com.revolsys.swing.map.layer.dataobject.renderer.GeometryStyleRenderer;
import com.revolsys.swing.map.layer.dataobject.renderer.MarkerStyleRenderer;
import com.revolsys.swing.map.layer.dataobject.style.GeometryStyle;
import com.revolsys.swing.map.layer.dataobject.style.MarkerStyle;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

@SuppressWarnings("serial")
public class EditGeometryOverlay extends SelectFeaturesOverlay implements
  PropertyChangeListener, MouseListener, MouseMotionListener {

  private final Project project;

  private final Viewport2D viewport;

  private final ListCoordinatesList points = new ListCoordinatesList(2);

  private Geometry geometry;

  private Point firstPoint;

  private Point previousPoint;

  private GeometryFactory geometryFactory;

  private Point mouseOverVertexPoint;

  private static final MarkerStyle XOR_POINT_STYLE = MarkerStyle.marker(
    "ellipse", 6, new Color(0, 0, 255), 1, new Color(0, 0, 255));

  private static final GeometryStyle XOR_LINE_STYLE = GeometryStyle.line(
    new Color(0, 0, 255), 2);

  private DataType geometryDataType;

  private DataObjectLayer layer;

  private String mode;

  private int actionId = 0;

  private Geometry xorGeometry;

  private DataObject object;

  private final Cursor addNodeCursor = SilkIconLoader.getCursor(
    "cursor_new_node", 8, 7);

  private ActionListener completedAction;

  private int[] mouseOverVertexId;

  private boolean movingNode;

  private PointQuadTree<int[]> vertices;

  private QuadTree<IndexedLineSegment> lineSegments;

  public EditGeometryOverlay(final MapPanel map) {
    super(map, new Color(0, 255, 255));

    this.viewport = map.getViewport();
    this.project = map.getProject();
    this.geometryFactory = viewport.getGeometryFactory();
  }

  protected void actionGeometryCompleted() {
    if (isGeometryValid()) {
      try {
        firstPoint = null;
        previousPoint = null;
        xorGeometry = null;
        if ("add".equals(mode)) {
          if (layer != null) {
            final DataObject object = layer.createObject();
            if (object != null) {
              object.setGeometryValue(geometry);
            }
            this.object = object;
            fireActionPerformed(completedAction, "Geometry Complete");
            this.object = null;
            setEditingObject(layer, object);
          }
        } else if ("edit".equals(mode)) {
          if (object != null) {
            object.setGeometryValue(geometry);
            fireActionPerformed(completedAction, "Geometry Complete");
          }
        }

      } finally {
        clearMapCursor();
      }
    }
  }

  /**
   * Set the layer that a new feature is to be added to.
   * 
   * @param layer 
   */
  public void addObject(final DataObjectLayer layer,
    final ActionListener completedAction) {
    setEditingObject(null, null);
    if (layer != null) {
      final DataObjectMetaData metaData = layer.getMetaData();
      final Attribute geometryAttribute = metaData.getGeometryAttribute();
      if (geometryAttribute != null) {
        mode = "add";
        this.layer = layer;
        this.completedAction = completedAction;
        this.geometryFactory = metaData.getGeometryFactory();
        this.geometryDataType = geometryAttribute.getType();
        setMapCursor(addNodeCursor);
      }
    }
  }

  private void clearEditingObjects(final LayerGroup layerGroup) {
    for (final Layer layer : layerGroup.getLayers()) {
      if (layer instanceof LayerGroup) {
        final LayerGroup childGroup = (LayerGroup)layer;
        clearEditingObjects(childGroup);
      }
      if (layer instanceof DataObjectLayer) {
        final DataObjectLayer dataObjectLayer = (DataObjectLayer)layer;
        dataObjectLayer.clearEditingObjects();
      }
    }

  }

  protected void clearMouseOverVertex() {
    mouseOverVertexId = null;
    mouseOverVertexPoint = null;
    repaint();
  }

  protected Geometry createGeometry() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    Geometry geometry = null;
    final int size = points.size();
    if (size == 1) {
      geometry = geometryFactory.createPoint(points);
    } else if (size == 2 || DataTypes.LINE_STRING.equals(geometryDataType)
      || DataTypes.MULTI_LINE_STRING.equals(geometryDataType)) {
      geometry = geometryFactory.createLineString(points.clone());
    } else if (DataTypes.POLYGON.equals(geometryDataType)) {
      final Coordinates endPoint = points.get(0);
      final CoordinatesList ring = CoordinatesListUtil.subList(points, null, 0,
        size, endPoint);
      geometry = geometryFactory.createPolygon(ring);
    }
    vertices = GeometryEditUtil.createPointQuadTree(geometry);
    lineSegments = GeometryEditUtil.createLineSegmentQuadTree(geometry);
    return geometry;
  }

  protected LineString createXorLine(final Point p0, final Point p1) {
    final GeometryFactory viewportGeometryFactory = viewport.getGeometryFactory();
    final Coordinates c0 = CoordinatesUtil.get(viewportGeometryFactory.copy(p0));
    final Coordinates c1 = CoordinatesUtil.get(viewportGeometryFactory.copy(p1));
    final LineSegment line = new LineSegment(viewportGeometryFactory, c0, c1);
    final double length = line.getLength();
    final double cursorRadius = viewport.getModelUnitsPerViewUnit() * 6;
    final Coordinates newC1 = line.pointAlongOffset((length - cursorRadius)
      / length, 0);
    Point point = viewportGeometryFactory.createPoint(newC1);
    point = geometryFactory.copy(point);
    return geometryFactory.createLineString(p0, point);
  }

  protected void drawXorGeometry(final Graphics2D graphics,
    final Geometry geometry) {
    if (geometry != null) {
      final Paint paint = graphics.getPaint();
      try {
        graphics.setXORMode(Color.WHITE);
        if (geometry instanceof Point) {
          final Point point = (Point)geometry;
          MarkerStyleRenderer.renderMarker(viewport, graphics, point,
            XOR_POINT_STYLE);
        } else {
          GeometryStyleRenderer.renderGeometry(viewport, graphics, geometry,
            XOR_LINE_STYLE);
        }
      } finally {
        graphics.setPaint(paint);
      }
    }
  }

  protected void fireActionPerformed(final ActionListener listener,
    final String command) {
    if (listener != null) {
      final ActionEvent actionEvent = new ActionEvent(this, actionId++, command);
      listener.actionPerformed(actionEvent);
    }
  }

  public GeometryFactory getGeometryFactory() {
    return geometryFactory;
  }

  protected Graphics2D getGraphics2D() {
    return (Graphics2D)getGraphics();
  }

  public DataObjectLayer getLayer() {
    return layer;
  }

  public DataObject getObject() {
    return object;
  }

  protected Point getPoint(final MouseEvent event) {
    final Point point = getViewportPoint(event);
    return geometryFactory.copy(point);
  }

  @Override
  protected Collection<DataObject> getSelectedObjects(
    final DataObjectLayer layer) {
    if ("add".equals(mode)) {
      return Collections.emptyList();
    } else {
      return layer.getEditingObjects();
    }
  }

  protected Point getViewportPoint(final MouseEvent event) {
    final java.awt.Point eventPoint = event.getPoint();
    final Point point = viewport.toModelPoint(eventPoint);
    return point;
  }

  protected boolean isEditable(final DataObjectLayer dataObjectLayer) {
    return dataObjectLayer.isVisible() && dataObjectLayer.isCanEditObjects();
  }

  protected boolean isGeometryValid() {
    if (DataTypes.POINT.equals(geometryDataType)) {
      if (geometry instanceof Point) {
        return true;
      } else {
        return false;
      }
    } else if (DataTypes.MULTI_POINT.equals(geometryDataType)) {
      if ((geometry instanceof Point) || (geometry instanceof MultiPoint)) {
        return true;
      } else {
        return false;
      }
    } else if (DataTypes.LINE_STRING.equals(geometryDataType)) {
      if (geometry instanceof LineString) {
        return true;
      } else {
        return false;
      }
    } else if (DataTypes.MULTI_LINE_STRING.equals(geometryDataType)) {
      if ((geometry instanceof LineString)
        || (geometry instanceof MultiLineString)) {
        return true;
      } else {
        return false;
      }
    } else if (DataTypes.POLYGON.equals(geometryDataType)) {
      if (geometry instanceof Polygon) {
        return true;
      } else {
        return false;
      }
    } else if (DataTypes.MULTI_POLYGON.equals(geometryDataType)) {
      if ((geometry instanceof Polygon) || (geometry instanceof MultiPolygon)) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  protected boolean isSelectable(final DataObjectLayer dataObjectLayer) {
    return isEditable(dataObjectLayer);
  }

  @Override
  public boolean isSelectEvent(final MouseEvent event) {
    if (!"add".equals(mode) && SwingUtilities.isLeftMouseButton(event)) {
      final boolean keyPress = event.isAltDown();
      return keyPress;
    }
    return false;
  }

  @Override
  public void keyReleased(final KeyEvent e) {
    super.keyReleased(e);
    final int keyCode = e.getKeyCode();
    if (keyCode == KeyEvent.VK_BACK_SPACE) {
      if (mouseOverVertexId != null) {

        setGeometry(GeometryEditUtil.deleteVertex(geometry, mouseOverVertexId));
        clearMouseOverVertex();
        repaint();
      }
    } else if (keyCode == KeyEvent.VK_ESCAPE) {
      if (movingNode) {
        moveNodeFinish(null);
      }
    }
  }

  protected void modeAddMouseClick(final MouseEvent event) {
    if (SwingUtilities.isLeftMouseButton(event)) {
      if ("add".equals(mode)) {
        final Point point = getPoint(event);
        final int size = points.size();
        if (size == 0) {
          points.add(point);
          firstPoint = point;
        } else {
          final Coordinates lastPoint = points.get(size - 1);
          if (!CoordinatesUtil.get(point).equals(lastPoint)) {
            points.add(point);
            previousPoint = point;
          }
        }

        geometry = createGeometry();
        xorGeometry = null;
        event.consume();
        if (DataTypes.POINT.equals(geometryDataType)) {
          actionGeometryCompleted();
        }
        if (event.getClickCount() == 2) {
          actionGeometryCompleted();
        }
        repaint();
      }
    }
  }

  @Override
  public void mouseClicked(final MouseEvent event) {
    if ("add".equals(mode)) {
      modeAddMouseClick(event);
    }
  }

  @Override
  public void mouseDragged(final MouseEvent event) {
    if (movingNode) {
      final Graphics2D graphics = (Graphics2D)getGraphics();
      drawXorGeometry(graphics, xorGeometry);

      final Point point = getPoint(event);

      // TODO same clipping as is done on the add vertex geometry
      final List<LineString> pointsList = new ArrayList<LineString>();
      final Coordinates previousPoint = GeometryEditUtil.getCoordinatesOffset(
        geometry, mouseOverVertexId, -1);
      if (previousPoint != null) {
        pointsList.add(createXorLine(
          geometryFactory.createPoint(previousPoint), point));
      }
      final Coordinates nextPoint = GeometryEditUtil.getCoordinatesOffset(
        geometry, mouseOverVertexId, 1);
      if (nextPoint != null) {
        pointsList.add(createXorLine(geometryFactory.createPoint(nextPoint),
          point));
      }
      if (pointsList.isEmpty()) {
        xorGeometry = point;
      } else {
        xorGeometry = geometryFactory.createMultiLineString(pointsList);
      }

      drawXorGeometry(graphics, xorGeometry);
    } else {
      super.mouseDragged(event);
    }
  }

  @Override
  public void mouseEntered(final MouseEvent e) {
    super.mouseEntered(e);
  }

  @Override
  public void mouseExited(final MouseEvent e) {
    super.mouseExited(e);
  }

  @Override
  public void mouseMoved(final MouseEvent event) {
    if (geometry != null) {

      final Graphics2D graphics = (Graphics2D)getGraphics();

      final Point point = getPoint(event);

      updateMouseOverVertexPoint(graphics, event);

      if ("add".equals(mode)) {
        drawXorGeometry(graphics, xorGeometry);
        if (firstPoint == null) {
          xorGeometry = null;
        } else if (previousPoint == null) {
          xorGeometry = createXorLine(firstPoint, point);
        } else if (DataTypes.LINE_STRING.equals(geometryDataType)
          || DataTypes.MULTI_LINE_STRING.equals(geometryDataType)) {
          xorGeometry = createXorLine(previousPoint, point);
        } else {
          xorGeometry = geometryFactory.createLineString(previousPoint, point,
            firstPoint);
        }
        drawXorGeometry(graphics, xorGeometry);
      }
    }
  }

  @Override
  public void mousePressed(final MouseEvent event) {
    if ("add".equals(mode)) {
      if (SwingUtil.isLeftButtonAndNoModifiers(event)) {
        event.consume();
        return;
      }
    } else if ("edit".equals(mode)) {
      if (SwingUtil.isLeftButtonAndNoModifiers(event)) {
        if (mouseOverVertexId != null) {
          repaint();
          setMapCursor(addNodeCursor);
          movingNode = true;
          event.consume();
        }
        return;
      }
    }
    super.mousePressed(event);
  }

  @Override
  public void mouseReleased(final MouseEvent event) {
    if (movingNode) {
      moveNodeFinish(event);
    } else {
      super.mouseReleased(event);
    }
  }

  protected void moveNodeFinish(final MouseEvent event) {
    try {
      if (event != null) {
        final Point point = getPoint(event);
        final Geometry newGeometry = GeometryEditUtil.moveVertex(geometry,
          CoordinatesUtil.get(point), mouseOverVertexId);
        setGeometry(newGeometry);
      }
    } finally {
      clearMapCursor();
      movingNode = false;
    }
  }

  @Override
  public void paintComponent(final Graphics graphics) {
    final Graphics2D graphics2d = (Graphics2D)graphics;
    if (geometry != null) {
      final GeometryFactory viewGeometryFactory = viewport.getGeometryFactory();
      final Geometry mapGeometry = viewGeometryFactory.copy(geometry);
      if (!(geometry instanceof Point)) {
        GeometryStyleRenderer.renderGeometry(viewport, graphics2d, mapGeometry,
          getHighlightStyle());
        GeometryStyleRenderer.renderOutline(viewport, graphics2d, mapGeometry,
          getOutlineStyle());
      }
      MarkerStyleRenderer.renderMarkerVertices(viewport, graphics2d,
        mapGeometry, getVertexStyle());
    }
    paintSelectBox(graphics2d);
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    super.propertyChange(event);
    final String propertyName = event.getPropertyName();
    if ("preEditable".equals(propertyName)) {
      actionGeometryCompleted();
    } else if ("editable".equals(propertyName)) {
      if (event.getSource() == layer) {
        if (!isEditable(layer)) {
          setEditingObject(null, null);
        }
      }
    }
  }

  public void selectObjects(final BoundingBox boundingBox) {
    Project project = getProject();
    if (!selectObjects(project, boundingBox)) {
      setEditingObject(null, null);
    }
  }

  protected boolean selectObjects(final LayerGroup group,
    BoundingBox boundingBox) {
    boolean found = false;
    for (final Layer layer : group.getLayers()) {
      double scale = getViewport().getScale();
      if (layer instanceof LayerGroup) {
        final LayerGroup childGroup = (LayerGroup)layer;
        found |= selectObjects(childGroup, boundingBox);
      } else if (layer instanceof DataObjectLayer) {
        final DataObjectLayer dataObjectLayer = (DataObjectLayer)layer;
        if (dataObjectLayer.isEditable(scale)) {
          DataObjectMetaData metaData = dataObjectLayer.getMetaData();
          if (metaData != null) {
            if (metaData.getGeometryAttributeIndex() != -1) {
              final List<DataObject> objects = dataObjectLayer.getDataObjects(boundingBox);
              if (!objects.isEmpty()) {
                final DataObject selectedObject = objects.get(0);
                dataObjectLayer.setEditingObjects(Collections.singleton(selectedObject));
                setEditingObject(dataObjectLayer, selectedObject);
                return true;
              }
            }
          }
        }
      }
    }
    return found;
  }

  public void setEditingObject(final DataObjectLayer layer,
    final DataObject object) {
    clearEditingObjects(project);
    this.layer = layer;
    final DataObject oldValue = this.object;
    if (oldValue != null) {
      actionGeometryCompleted();
    }
    this.completedAction = null;
    this.object = object;
    Geometry geometry = null;

    if (object != null) {
      final DataObjectMetaData metaData = layer.getMetaData();
      final Attribute geometryAttribute = metaData.getGeometryAttribute();
      if (geometryAttribute != null) {
        geometryDataType = geometryAttribute.getType();
        geometry = object.getGeometryValue();
      }
      layer.setEditingObjects(Collections.singletonList(object));
    }
    setGeometry(geometry);

    mode = "edit";
    firstPoint = null;
    firePropertyChange("object", oldValue, object);
  }

  protected void setGeometry(final Geometry geometry) {
    this.geometry = geometry;
    points.clear();
    xorGeometry = null;
    mouseOverVertexPoint = null;
    mouseOverVertexId = null;
    if (geometry == null) {
      vertices = null;
      lineSegments = null;
      geometryFactory = null;
    } else {
      vertices = GeometryEditUtil.createPointQuadTree(geometry);
      lineSegments = GeometryEditUtil.createLineSegmentQuadTree(geometry);
      geometryFactory = GeometryFactory.getFactory(geometry);
    }
    repaint();
  }

  private void updateMouseOverVertexPoint(final Graphics2D graphics,
    final MouseEvent event) {
    Point currentPoint = null;
    Coordinates currentCoordinates = null;
    if (mouseOverVertexPoint != null) {
      currentCoordinates = CoordinatesUtil.get(mouseOverVertexPoint);
    }
    mouseOverVertexId = null;
    if (vertices != null) {
      final Point viewportPoint = getViewportPoint(event);
      final double maxViewDistance = viewport.getModelUnitsPerViewUnit() * 10;
      final BoundingBox boundingBox = BoundingBox.getBoundingBox(viewportPoint)
        .expand(maxViewDistance);

      final List<int[]> closeVertices = vertices.findWithin(boundingBox);
      Collections.sort(closeVertices, new Comparator<int[]>() {
        @Override
        public int compare(int[] object1, int[] object2) {
          for (int i = 0; i < Math.max(object1.length, object2.length); i++) {
            if (i >= object1.length) {
              return -1;
            } else if (i >= object2.length) {
              return 1;
            } else {
              int value1 = object1[i];
              int value2 = object2[i];
              if (value1 < value2) {
                return -1;
              } else if (value1 > value2) {
                return 1;
              }
            }
          }
          return 0;
        }
      });
      if (!closeVertices.isEmpty()) {
        final Point modelPoint = geometryFactory.copy(viewportPoint);
        double minDistance = Double.MAX_VALUE;
        for (final int[] vertexIndex : closeVertices) {
          final Coordinates vertex = GeometryEditUtil.getCoordinates(geometry,
            vertexIndex);
          final double distance = vertex.distance(CoordinatesUtil.get(modelPoint));
          if (distance < minDistance) {
            mouseOverVertexId = vertexIndex;
            minDistance = distance;
            if (currentPoint == null || !currentCoordinates.equals(vertex)) {
              currentPoint = geometryFactory.createPoint(vertex);
            }
          }
        }
      }
    }
    if (currentPoint == null) {
      // TODO mouse over for line segments
    }
    drawXorGeometry(graphics, mouseOverVertexPoint);
    mouseOverVertexPoint = currentPoint;
    drawXorGeometry(graphics, mouseOverVertexPoint);
  }
}
