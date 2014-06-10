package com.revolsys.swing.map.overlay;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.revolsys.awt.WebColors;
import com.revolsys.converter.string.BooleanStringConverter;
import com.revolsys.famfamfam.silk.SilkIconLoader;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.Envelope;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.LineString;
import com.revolsys.jts.geom.Point;
import com.revolsys.jts.geom.Polygon;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.map.MapPanel;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.dataobject.renderer.GeometryStyleRenderer;
import com.revolsys.swing.map.layer.dataobject.renderer.MarkerStyleRenderer;
import com.revolsys.swing.map.layer.dataobject.style.GeometryStyle;
import com.revolsys.swing.map.layer.dataobject.style.MarkerStyle;
import com.revolsys.swing.map.layer.raster.GeoReferencedImage;
import com.revolsys.swing.map.layer.raster.GeoReferencedImageLayer;
import com.revolsys.swing.map.layer.raster.GeoReferencedImageLayerRenderer;
import com.revolsys.swing.map.layer.raster.MappedLocation;
import com.revolsys.swing.map.layer.raster.filter.WarpFilter;
import com.revolsys.swing.undo.ListAddUndo;
import com.revolsys.swing.undo.SetObjectProperty;

public class EditGeoReferencedImageOverlay extends AbstractOverlay {
  private static final Cursor CURSOR_MOVE_IMAGE = SilkIconLoader.getCursor(
    "cursor_move", 8, 7);

  private static final Color COLOR_OUTLINE = WebColors.Black;

  private static final Color COLOR_SELECT = WebColors.Cyan;

  private static final long serialVersionUID = 1L;

  private static final GeometryStyle STYLE_MAPPED_LINE = GeometryStyle.line(
    COLOR_SELECT, 3);

  private static final GeometryStyle STYLE_IMAGE_LINE = GeometryStyle.line(
    COLOR_SELECT, 1);

  private static final MarkerStyle STYLE_VERTEX_FIRST_POINT = MarkerStyle.marker(
    SelectedRecordsRenderer.firstVertexShape(), 9, COLOR_OUTLINE, 1,
    COLOR_SELECT);

  private static final MarkerStyle STYLE_VERTEX_LAST_POINT = MarkerStyle.marker(
    SelectedRecordsRenderer.lastVertexShape(), 9, COLOR_OUTLINE, 1,
    COLOR_SELECT);

  static {
    STYLE_VERTEX_FIRST_POINT.setMarkerOrientationType("auto");
    STYLE_VERTEX_FIRST_POINT.setMarkerPlacement("point(0)");
    STYLE_VERTEX_FIRST_POINT.setMarkerHorizontalAlignment("center");

    STYLE_VERTEX_LAST_POINT.setMarkerOrientationType("auto");
    STYLE_VERTEX_LAST_POINT.setMarkerPlacement("point(n)");
    STYLE_VERTEX_LAST_POINT.setMarkerHorizontalAlignment("right");
  }

  private Point addTiePointFirstPoint;

  private GeoReferencedImage image;

  private GeoReferencedImageLayer layer;

  private Point moveCornerOppositePoint;

  private Point moveCornerPoint;

  private Point moveImageFirstPoint;

  private BoundingBox moveImageBoundingBox;

  private Point addTiePointMove;

  private BufferedImage cachedImage;

  private int moveTiePointIndex = -1;

  private java.awt.Point moveTiePointEventPoint;

  private final List<Integer> closeSourcePixelIndexes = new ArrayList<Integer>();

  private final List<Integer> closeTargetPointIndexes = new ArrayList<Integer>();

  private Cursor moveCornerCursor;

  private boolean moveTiePointStarted;

  private boolean moveTiePointSource;

  private Point moveTiePointLocation;

  public static final String ACTION_MOVE_IMAGE = "moveImage";

  private static final String ACTION_MOVE_IMAGE_CORNER = "moveImageCorner";

  private boolean moveImageEnabled;

  public EditGeoReferencedImageOverlay(final MapPanel map) {
    super(map);
  }

  protected void addTiePointClear() {
    this.addTiePointFirstPoint = null;
    this.addTiePointMove = null;
    clearMapCursor();
    clearCachedImage();
    clearMouseOverGeometry();
  }

  private boolean addTiePointFinish(final MouseEvent event) {
    if (event.getButton() == MouseEvent.BUTTON1) {
      if (this.addTiePointFirstPoint != null) {
        try {
          Point mapPoint = getViewportPoint(event);
          final Point snapPoint = getSnapPoint();
          if (snapPoint != null) {
            mapPoint = snapPoint;
          }
          final Point sourcePoint = this.addTiePointFirstPoint;
          final WarpFilter warpFilter = layer.getWarpFilter();
          final Point sourcePixel = warpFilter.targetPointToSourcePixel(sourcePoint);
          final GeometryFactory geometryFactory = getImageGeometryFactory();
          final Point targetPoint = mapPoint.copy(geometryFactory);
          final MappedLocation mappedLocation = new MappedLocation(sourcePixel,
            targetPoint);
          addUndo(new ListAddUndo(image.getTiePoints(), mappedLocation));
        } finally {
          addTiePointClear();
        }
        return true;
      }
    }
    return false;
  }

  private boolean addTiePointMove(final MouseEvent event) {
    if (this.addTiePointFirstPoint != null) {
      final BoundingBox boundingBox = getHotspotBoundingBox(event);
      hasSnapPoint(event, boundingBox);

      if (getSnapPoint() == null) {
        addTiePointMove = getViewportPoint(event);
      } else {
        addTiePointMove = getSnapPoint();
      }
      repaint();
      event.consume();
      return true;
    }
    return false;
  }

  private boolean addTiePointStart(final MouseEvent event) {
    if (this.layer != null) {
      if (addTiePointFinish(event)) {
      } else if (SwingUtil.isLeftButtonAndAltDown(event)) {
        final Point mousePoint = getViewportPoint(event);
        if (getImageBoundingBox().covers(mousePoint)) {
          this.addTiePointFirstPoint = mousePoint;
          event.consume();
          return true;
        }
      }
    }
    return false;
  }

  protected void adjustBoundingBoxAspectRatio() {
    if (moveImageBoundingBox != null && moveCornerPoint != null) {
      final double imageAspectRatio = this.image.getImageAspectRatio();
      final BoundingBox boundingBox = moveImageBoundingBox;
      final double aspectRatio = boundingBox.getAspectRatio();
      double minX = boundingBox.getMinX();
      double maxX = boundingBox.getMaxX();
      double minY = boundingBox.getMinY();
      double maxY = boundingBox.getMaxY();
      final double width = boundingBox.getWidth();
      final double height = boundingBox.getHeight();
      if (aspectRatio < imageAspectRatio) {
        if (minX == this.moveCornerOppositePoint.getX()) {
          maxX = minX + height * imageAspectRatio;
        } else {
          minX = maxX - height * imageAspectRatio;
        }
      } else if (aspectRatio > imageAspectRatio) {
        if (minY == this.moveCornerOppositePoint.getY()) {
          maxY = minY + width / imageAspectRatio;
        } else {
          minY = maxY - width / imageAspectRatio;
        }
      }
      final GeometryFactory geometryFactory = getGeometryFactory();
      moveImageBoundingBox = new Envelope(geometryFactory, 2, minX, minY, maxX,
        maxY);
    }
  }

  protected void appendTiePointLocation(final StringBuffer toolTip,
    final List<MappedLocation> tiePoints, final List<Integer> indices,
    final int startNumber, final boolean source) {
    if (!indices.isEmpty()) {
      int i = startNumber - 1;
      toolTip.append("<div style=\"border-bottom: solid black 1px; font-weight:bold;padding: 1px 3px 1px 3px\">");
      if (source) {
        toolTip.append("Move source pixel");
      } else {
        toolTip.append("Move target point");
      }
      toolTip.append("</div>");
      toolTip.append("<div style=\"padding: 1px 3px 1px 3px\">");
      toolTip.append("<ol start=\"");
      toolTip.append(startNumber);
      toolTip.append("\" style=\"margin: 1px 3px 1px 15px\">");
      for (final Integer index : indices) {
        final MappedLocation tiePoint = tiePoints.get(index);
        final Object value;
        if (source) {
          value = tiePoint.getSourcePixel();
        } else {
          value = tiePoint.getTargetPoint();
        }
        toolTip.append("<li");
        if (i == moveTiePointIndex) {
          toolTip.append(" style=\"border: 1px solid red; padding: 2px; background-color:#FFC0CB\"");
        }
        toolTip.append(">#");
        toolTip.append(index + 1);
        toolTip.append(" ");
        toolTip.append(value);
        toolTip.append("</li>");
      }
      toolTip.append("</ol></div>");
      i++;
    }
  }

  protected void cancel() {
    moveImageClear();
    moveCornerClear();
    this.moveImageFirstPoint = null;
    this.moveTiePointEventPoint = null;
    this.moveTiePointLocation = null;
    addTiePointClear();
    closeSourcePixelIndexes.clear();
    closeTargetPointIndexes.clear();
    clearCachedImage();
    repaint();
  }

  protected void clear() {
    this.image = null;
    this.layer = null;
    clearUndoHistory();
    cancel();
  }

  protected void clearCachedImage() {
    if (this.cachedImage != null) {
      this.cachedImage.flush();
    }
    this.cachedImage = null;
    System.gc();
  }

  protected BufferedImage getCachedImage(final BoundingBox boundingBox) {
    final Viewport2D viewport = getViewport();
    if (cachedImage == null) {
      BufferedImage originalImage;
      if (layer.isShowOriginalImage()) {
        originalImage = this.image.getOriginalImage();
      } else {
        originalImage = this.image.getWarpedImage();
      }
      final int newWidth = Math.min(
        originalImage.getWidth(),
        (int)Math.ceil(Viewport2D.toDisplayValue(viewport,
          boundingBox.getWidthLength())));
      final int newHeight = Math.min(
        originalImage.getHeight(),
        (int)Math.ceil(Viewport2D.toDisplayValue(viewport,
          boundingBox.getHeightLength())));

      final BufferedImage newImage = new BufferedImage(newWidth, newHeight,
        BufferedImage.TYPE_INT_ARGB);
      final Graphics2D imageGraphics = (Graphics2D)newImage.getGraphics();
      imageGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      final Composite composite = AlphaComposite.getInstance(
        AlphaComposite.SRC, .6f);
      imageGraphics.setComposite(composite);
      imageGraphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
      imageGraphics.dispose();
      this.cachedImage = newImage;
    }
    return cachedImage;
  }

  public BoundingBox getImageBoundingBox() {
    if (image == null) {
      return new Envelope();
    } else {
      return this.layer.getBoundingBox();
    }
  }

  public GeometryFactory getImageGeometryFactory() {
    if (image == null) {
      return getGeometryFactory();
    } else {
      return this.layer.getGeometryFactory();
    }
  }

  public GeoReferencedImageLayer getLayer() {
    return this.layer;
  }

  protected BoundingBox getMoveBoundingBox(final MouseEvent event) {
    BoundingBox boundingBox = getImageBoundingBox();
    final Point mousePoint = getViewportPoint(event);
    final GeometryFactory imageGeometryFactory = getImageGeometryFactory();
    final Point imagePoint = mousePoint.convert(imageGeometryFactory);

    final double deltaX = imagePoint.getX() - moveImageFirstPoint.getX();
    final double deltaY = imagePoint.getY() - moveImageFirstPoint.getY();
    boundingBox = boundingBox.move(deltaX, deltaY);
    return boundingBox;
  }

  private MappedLocation getMoveTiePoint() {
    if (moveTiePointIndex > -1) {
      int tiePointIndex;
      final int targetSize = closeTargetPointIndexes.size();
      if (moveTiePointIndex < targetSize) {
        tiePointIndex = closeTargetPointIndexes.get(moveTiePointIndex);
        moveTiePointSource = false;
      } else if (moveTiePointIndex - targetSize < closeSourcePixelIndexes.size()) {
        tiePointIndex = closeSourcePixelIndexes.get(moveTiePointIndex
          - targetSize);
        moveTiePointSource = true;
      } else {
        return null;
      }
      return image.getTiePoints().get(tiePointIndex);
    }
    return null;
  }

  protected boolean isApplicable(final MouseEvent event) {
    final BoundingBox imageBoundingBox = getImageBoundingBox();
    final Point point = getPoint(event);
    final double distance = getDistance(event);

    return imageBoundingBox.distance(point) < distance * 2;
  }

  private boolean isInImage() {
    final Point mousePoint = getMousePoint();
    return isInImage(mousePoint);
  }

  private boolean isInImage(final MouseEvent event) {
    final Point mousePoint = getPoint(event);
    final boolean inImage = isInImage(mousePoint);
    return inImage;
  }

  private boolean isInImage(final Point mousePoint) {
    if (mousePoint == null) {
      return false;
    } else {
      final BoundingBox imageBoundingBox = getImageBoundingBox();
      final boolean inImage = imageBoundingBox.covers(mousePoint);
      return inImage;
    }
  }

  @Override
  public void keyPressed(final KeyEvent event) {
    if (this.layer != null) {
      final int keyCode = event.getKeyCode();
      if (this.moveCornerPoint != null) {
        if (keyCode == KeyEvent.VK_SHIFT) {
          adjustBoundingBoxAspectRatio();
          repaint();
          event.consume();
        } else if (keyCode == KeyEvent.VK_CONTROL
          || keyCode == KeyEvent.VK_META) {
          event.consume();
        }
      } else {
        if (keyCode == KeyEvent.VK_CONTROL || keyCode == KeyEvent.VK_META) {
          if (isInImage()) {
            setMapCursor(CURSOR_MOVE_IMAGE);
            event.consume();
          }
        }
      }
    }
  }

  @Override
  public void keyReleased(final KeyEvent event) {
    if (this.layer != null) {
      final int keyCode = event.getKeyCode();
      if (moveTiePointIndex > -1 || addTiePointFirstPoint != null
        || moveTiePointStarted) {
        final char keyChar = event.getKeyChar();
        if (keyChar >= '1' && keyChar <= '9') {
          event.consume();
        }
      }
      if (keyCode == KeyEvent.VK_ESCAPE) {
        cancel();
        repaint();
      } else if (keyCode == KeyEvent.VK_CONTROL) {
        clearMapCursor(CURSOR_MOVE_IMAGE);
      }

    }
  }

  @Override
  public void keyTyped(final KeyEvent e) {
    final char keyChar = e.getKeyChar();
    if (keyChar >= '1' && keyChar <= '9') {
      final int index = keyChar - '1';
      if (!moveTiePointStarted && moveTiePointIndex > -1) {
        if (index < this.closeSourcePixelIndexes.size()
          + this.closeTargetPointIndexes.size()) {

          this.moveTiePointIndex = index;
          setMoveTiePointToolTip();
        }
        e.consume();
      } else if (index < getSnapPointLocationMap().size()) {
        setSnapPointIndex(index);
        setSnapLocations(getSnapPointLocationMap());
        if (moveTiePointStarted) {
          if (!moveTiePointSource) {
            moveTiePointFinish(null);
            e.consume();
          }
        } else if (addTiePointFirstPoint != null) {
          addTiePointFinish(null);
          e.consume();
        }
        getMap().repaint();
      }

    }
  }

  @Override
  public void mouseClicked(final MouseEvent event) {
    if (this.layer != null) {
      if (isApplicable(event)) {
        if (addTiePointFinish(event)) {
        } else if (addTiePointStart(event)) {
        }
      }
    }
  }

  @Override
  public void mouseDragged(final MouseEvent event) {
    if (this.layer != null) {
      if (moveTiePointDrag(event)) {
      } else if (moveCornerDrag(event)) {
      } else if (moveImageDrag(event)) {
      }
    }
  }

  @Override
  public void mouseMoved(final MouseEvent event) {
    if (this.layer != null) {
      if (moveTiePointMove(event)) {
      } else if (moveCornerMove(event)) {
      } else if (moveImageMove(event)) {
      } else if (addTiePointMove(event)) {
      } else if (!hasOverlayAction()) {
        clearMapCursor();
      }
    }
  }

  @Override
  public void mousePressed(final MouseEvent event) {
    if (this.layer != null) {
      if (moveTiePointStart(event)) {
      } else if (isInImage() && SwingUtil.isLeftButtonAndAltDown(event)) {
        event.consume();
      } else if (moveCornerStart(event)) {
      } else if (moveImageStart(event)) {
      }
    }
  }

  @Override
  public void mouseReleased(final MouseEvent event) {
    if (this.layer != null) {
      if (event.getButton() == MouseEvent.BUTTON1) {
        if (moveTiePointFinish(event)) {
        } else if (moveCornerFinish(event)) {
        } else if (moveCornerFinish(event)) {
        } else if (moveImageFinish(event)) {
        }
      }
    }
  }

  protected void moveCornerClear() {
    clearOverlayAction(ACTION_MOVE_IMAGE_CORNER);
    clearMapCursor(this.moveCornerCursor);
    this.moveImageBoundingBox = null;
    this.moveCornerCursor = null;
    this.moveCornerOppositePoint = null;
    this.moveCornerPoint = null;
  }

  private boolean moveCornerDrag(final MouseEvent event) {
    if (this.moveCornerPoint == null) {
      return false;
    } else {
      final GeometryFactory viewportGeometryFactory = getViewportGeometryFactory();
      final Point mousePoint = getViewportPoint(event);
      moveImageBoundingBox = new Envelope(viewportGeometryFactory, mousePoint,
        this.moveCornerOppositePoint);

      if (SwingUtil.isShiftDown(event)) {
        adjustBoundingBoxAspectRatio();
      }
      setMapCursor(moveCornerCursor);
      repaint();
      event.consume();
      return true;
    }
  }

  private boolean moveCornerFinish(final MouseEvent event) {
    if (event.getButton() == MouseEvent.BUTTON1) {
      if (clearOverlayAction(ACTION_MOVE_IMAGE_CORNER)) {
        try {
          final SetObjectProperty setBBox = new SetObjectProperty(this,
            "imageBoundingBox", getImageBoundingBox(), moveImageBoundingBox);
          addUndo(setBBox);
          event.consume();
        } finally {
          moveCornerClear();
        }
        repaint();
        return true;
      }
    }
    return false;
  }

  private boolean moveCornerMove(final MouseEvent event) {
    if (this.layer != null) {
      final Point oldPoint = this.moveCornerPoint;

      final Point mousePoint = getViewportPoint(event);
      final GeometryFactory viewportGeometryFactory = getViewportGeometryFactory();

      Point closestPoint = null;
      final double maxDistance = getDistance(event);
      double closestDistance = Double.MAX_VALUE;
      if (oldPoint != null) {
        final double distance = oldPoint.distance(mousePoint);
        if (distance < maxDistance) {
          closestPoint = oldPoint;
          closestDistance = distance;
        }
      }
      int closestIndex = -1;
      final BoundingBox imageBoundingBox = getImageBoundingBox();
      if (imageBoundingBox.isEmpty()) {
        return false;
      } else {
        for (int i = 0; i < 4; i++) {
          final Point point = imageBoundingBox.getCornerPoint(i);
          final Point mapPoint = point.convert(viewportGeometryFactory, 2);
          final double distance = mapPoint.distance(mousePoint);
          if (distance < maxDistance && distance < closestDistance) {
            closestPoint = point;
            closestDistance = distance;
            closestIndex = i;
          }
        }

        if (closestPoint != oldPoint) {
          clearMapCursor(moveCornerCursor);
          this.moveCornerPoint = closestPoint;
          if (closestIndex == -1) {
            this.moveCornerOppositePoint = null;
          } else {
            this.moveCornerOppositePoint = imageBoundingBox.getCornerPoint(closestIndex + 2);
          }
          switch (closestIndex) {
            case 0:
              moveCornerCursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
            break;
            case 1:
              moveCornerCursor = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
            break;
            case 2:
              moveCornerCursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            break;
            case 3:
              moveCornerCursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            break;
            default:
              moveCornerCursor = null;
            break;
          }
          if (moveCornerCursor != null) {
            setMapCursor(moveCornerCursor);
          }
        }
      }
    }
    if (this.moveCornerPoint == null) {
      return false;
    } else {
      event.consume();
      return true;
    }
  }

  private boolean moveCornerStart(final MouseEvent event) {
    if (this.layer != null) {
      if (event.getButton() == MouseEvent.BUTTON1) {
        if (this.moveCornerPoint != null) {
          if (setOverlayAction(ACTION_MOVE_IMAGE_CORNER)) {
            event.consume();
            return true;
          }
        }
      }
    }
    return false;
  }

  protected void moveImageClear() {
    this.moveImageFirstPoint = null;
    this.moveImageBoundingBox = null;
    clearOverlayAction(ACTION_MOVE_IMAGE);
    clearMapCursor(CURSOR_MOVE_IMAGE);
  }

  private boolean moveImageDrag(final MouseEvent event) {
    if (this.moveImageFirstPoint == null) {
      return false;
    } else {
      setMapCursor(CURSOR_MOVE_IMAGE);
      moveImageBoundingBox = getMoveBoundingBox(event);
      repaint();
      event.consume();
      return true;
    }
  }

  private boolean moveImageFinish(final MouseEvent event) {
    if (event.getButton() == MouseEvent.BUTTON1) {
      if (clearOverlayAction(ACTION_MOVE_IMAGE)) {
        final BoundingBox boundingBox = getMoveBoundingBox(event);
        final SetObjectProperty setBBox = new SetObjectProperty(this,
          "imageBoundingBox", getImageBoundingBox(), boundingBox);
        addUndo(setBBox);
        moveImageClear();
        event.consume();
        repaint();
        return true;
      }
    }
    return false;
  }

  private boolean moveImageMove(final MouseEvent event) {
    if (isInImage(event)) {
      if (SwingUtil.isControlOrMetaDown(event)) {
        setMapCursor(CURSOR_MOVE_IMAGE);
        event.consume();
        this.moveImageEnabled = true;
        return true;
      }
    }
    this.moveImageEnabled = false;
    clearMapCursor(CURSOR_MOVE_IMAGE);
    return false;
  }

  private boolean moveImageStart(final MouseEvent event) {
    if (this.layer != null) {
      if (moveImageEnabled) {
        if (event.getButton() == MouseEvent.BUTTON1
          && SwingUtil.isControlOrMetaDown(event)) {
          if (setOverlayAction(ACTION_MOVE_IMAGE)) {
            final Point mousePoint = getViewportPoint(event);
            final GeometryFactory imageGeometryFactory = getImageGeometryFactory();
            final Point imagePoint = mousePoint.convert(imageGeometryFactory);
            final boolean inImage = isInImage(imagePoint);
            if (inImage) {
              setMapCursor(CURSOR_MOVE_IMAGE);
              this.moveImageFirstPoint = imagePoint;
              event.consume();
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private boolean moveTiePointDrag(final MouseEvent event) {
    if (moveTiePointStarted) {
      if (moveTiePointSource) {
        moveTiePointLocation = getViewportPoint(event);
      } else {
        final BoundingBox boundingBox = getHotspotBoundingBox(event);
        if (hasSnapPoint(event, boundingBox)) {
          moveTiePointLocation = getSnapPoint();
        } else {
          moveTiePointLocation = getViewportPoint(event);
        }
      }
      event.consume();
      repaint();
      return true;
    }
    return false;
  }

  private boolean moveTiePointFinish(final MouseEvent event) {
    if (moveTiePointStarted) {
      final MappedLocation tiePoint = getMoveTiePoint();
      if (tiePoint != null) {
        Point point = getPoint(event);
        if (moveTiePointSource) {
          final Point sourcePoint = point;
          final WarpFilter warpFilter = layer.getWarpFilter();
          final Point sourcePixel = warpFilter.targetPointToSourcePixel(sourcePoint);

          final SetObjectProperty setSourcePixel = new SetObjectProperty(
            tiePoint, "sourcePixel", tiePoint.getSourcePixel(), sourcePixel);
          addUndo(setSourcePixel);
        } else {
          final Point snapPoint = getSnapPoint();
          if (snapPoint != null) {
            point = snapPoint;
          }
          final GeometryFactory imageGeometryFactory = getImageGeometryFactory();
          point = point.copy(imageGeometryFactory);
          tiePoint.setTargetPoint(point);
          final SetObjectProperty setTargetPoint = new SetObjectProperty(
            tiePoint, "targetPoint", tiePoint.getTargetPoint(), point);
          addUndo(setTargetPoint);
        }
        closeSourcePixelIndexes.clear();
        closeTargetPointIndexes.clear();
        moveTiePointLocation = null;
        moveTiePointStarted = false;
        moveTiePointIndex = -1;
        clearCachedImage();
        clearMapCursor();
        clearMouseOverGeometry();
        if (event != null) {
          event.consume();
        }
        repaint();
        return true;
      }
    }
    return false;
  }

  private boolean moveTiePointMove(final MouseEvent event) {
    if (image != null) {
      final List<MappedLocation> tiePoints = image.getTiePoints();
      if (!tiePoints.isEmpty()) {
        if (!closeSourcePixelIndexes.isEmpty()
          && !closeTargetPointIndexes.isEmpty()) {
          clearMapCursor(CURSOR_NODE_EDIT);
        }
        closeSourcePixelIndexes.clear();
        closeTargetPointIndexes.clear();
        final WarpFilter filter = layer.getWarpFilter();
        final BoundingBox hotSpot = getHotspotBoundingBox(event);
        int i = 0;

        for (final MappedLocation tiePoint : tiePoints) {
          final Point targetPoint = tiePoint.getTargetPoint();
          if (filter != null) {
            final Point sourcePoint = filter.sourcePixelToTargetPoint(tiePoint);
            if (hotSpot.covers(sourcePoint)) {
              closeSourcePixelIndexes.add(i);
            }
            if (hotSpot.covers(targetPoint)) {
              closeTargetPointIndexes.add(i);
            }
          }
          i++;
        }
        moveTiePointIndex = 0;
        moveTiePointEventPoint = event.getPoint();
        if (setMoveTiePointToolTip()) {
          setMapCursor(CURSOR_NODE_EDIT);
          event.consume();
          return true;
        } else {
          moveTiePointEventPoint = null;
          moveTiePointIndex = -1;
          getMap().clearToolTipText();
          return false;
        }
      }
    }
    return false;
  }

  // TODO escape and undo for move tie point
  private boolean moveTiePointStart(final MouseEvent event) {
    if (moveTiePointIndex > -1) {
      moveTiePointStarted = true;
      getMap().clearToolTipText();
      event.consume();
      repaint();
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected void paintComponent(final Graphics2D graphics) {
    if (this.layer != null && this.layer.isVisible() && layer.isExists()
      && this.image != null) {
      final boolean showOriginalImage = layer.isShowOriginalImage();
      BoundingBox boundingBox = getImageBoundingBox();
      BoundingBox outlineBoundingBox = boundingBox;
      if (moveImageBoundingBox != null) {
        if (showOriginalImage) {
          boundingBox = moveImageBoundingBox;
        }
        outlineBoundingBox = moveImageBoundingBox;
      }
      final Viewport2D viewport = getViewport();

      // final BufferedImage renderImage = getCachedImage(boundingBox);
      try {
        final GeometryFactory viewportGeometryFactory = getViewportGeometryFactory();
        final BoundingBox renderBoundingBox = boundingBox.convert(viewportGeometryFactory);
        GeoReferencedImageLayerRenderer.render(viewport, graphics, this.image,
          renderBoundingBox, !showOriginalImage);
      } catch (final Throwable e) {
        LoggerFactory.getLogger(getClass()).error("Unable to render image", e);
      }

      if (outlineBoundingBox != null && !outlineBoundingBox.isEmpty()) {
        final Polygon imageBoundary = outlineBoundingBox.toPolygon(1);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_OFF);

        GeometryStyleRenderer.renderOutline(viewport, graphics, imageBoundary,
          GeometryStyle.line(Color.GREEN, 3));

        MarkerStyleRenderer.renderMarkerVertices(viewport, graphics,
          imageBoundary,
          MarkerStyle.marker("cross", 11, WebColors.Black, 1, WebColors.Lime));

        final int tiePointCount = image.getTiePoints().size();
        final GeometryFactory viewGeometryFactory = getGeometryFactory();
        if (this.image != null && tiePointCount > 0) {
          final MappedLocation moveTiePoint = getMoveTiePoint();
          for (int i = 0; i < tiePointCount; i++) {
            final MappedLocation mappedLocation = image.getTiePoints().get(i);
            if (!moveTiePointStarted || mappedLocation != moveTiePoint) {
              final LineString line = mappedLocation.getSourceToTargetLine(
                image, boundingBox, !showOriginalImage);
              renderTiePointLine(graphics, viewport, line);
            }
          }
          if (moveTiePointStarted && moveTiePoint != null
            && moveTiePointLocation != null) {
            Point sourcePoint = null;
            Point targetPoint = null;
            final GeometryFactory imageGeometryFactory = getImageGeometryFactory();

            if (moveTiePointSource) {
              sourcePoint = moveTiePointLocation.convert(imageGeometryFactory,
                2);
              targetPoint = moveTiePoint.getTargetPoint();
            } else {
              sourcePoint = moveTiePoint.getSourcePoint(image, boundingBox,
                !showOriginalImage);
              targetPoint = moveTiePointLocation.convert(imageGeometryFactory,
                2);
            }
            if (sourcePoint != null && targetPoint != null) {
              final LineString line = imageGeometryFactory.lineString(
                sourcePoint, targetPoint);
              renderTiePointLine(graphics, viewport, line);
            }
          }

        }
        if (!showOriginalImage) {

          final double width = image.getImageWidth() - 1;
          final double height = image.getImageHeight() - 1;
          final double[] targetCoordinates = MappedLocation.toModelCoordinates(
            image, boundingBox, true, 0, height, width, height, width, 0, 0, 0,
            0, height);
          final LineString line = viewGeometryFactory.lineString(2,
            targetCoordinates);
          GeometryStyleRenderer.renderLineString(viewport, graphics, line,
            STYLE_IMAGE_LINE);
        }
        if (addTiePointFirstPoint != null) {
          final LineString line = viewGeometryFactory.lineString(
            addTiePointFirstPoint, addTiePointMove);
          renderTiePointLine(graphics, viewport, line);
        }
      }
    }
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    super.propertyChange(event);
    final Object source = event.getSource();
    final String propertyName = event.getPropertyName();
    if (source instanceof GeoReferencedImageLayer) {
      final GeoReferencedImageLayer layer = (GeoReferencedImageLayer)source;
      if ("editable".equals(propertyName)) {
        if (!BooleanStringConverter.getBoolean(event.getNewValue())) {
          if (this.layer == layer) {
            setLayer(null);
          }
        } else {
          setLayer(layer);
        }
      } else if (this.layer == layer) {
        clearCachedImage();
        if ("boundingBox".equals(propertyName)) {
          final BoundingBox boundingBox = layer.getBoundingBox();
          setImageBoundingBox(boundingBox);
        } else if ("hasChanges".equals(propertyName)) {
          clear();
          setLayer(layer);
        } else if ("deleted".equals(propertyName)) {
          clear();
        }
      }
    } else if (source == image) {
      clearCachedImage();
    } else if ("scale".equals(propertyName)) {
      clearCachedImage();
    }
  }

  protected void renderTiePointLine(final Graphics2D graphics,
    final Viewport2D viewport, LineString line) {
    if (line != null) {
      GeometryStyleRenderer.renderLineString(viewport, graphics, line,
        STYLE_MAPPED_LINE);
      line = line.convert(viewport.getGeometryFactory());
      MarkerStyleRenderer.renderMarkers(viewport, graphics, line,
        STYLE_VERTEX_FIRST_POINT, STYLE_VERTEX_LAST_POINT, null);
    }
  }

  public void setImageBoundingBox(BoundingBox boundingBox) {
    if (boundingBox == null) {
      boundingBox = new Envelope(getGeometryFactory());
    }
    if (image != null) {
      image.setBoundingBox(boundingBox);
    }
    setGeometryFactory(boundingBox.getGeometryFactory());
    clearCachedImage();
  }

  public void setLayer(final GeoReferencedImageLayer layer) {
    final GeoReferencedImageLayer oldLayer = this.layer;
    if (oldLayer != layer) {
      clear();
      this.layer = layer;
      final Viewport2D viewport = getViewport();
      setGeometryFactory(viewport.getGeometryFactory());
      setEnabled(layer != null);
      if (layer != null) {
        this.image = layer.getImage();
        setImageBoundingBox(layer.getBoundingBox());
      }
      if (oldLayer != null) {
        oldLayer.setEditable(false);
      }
    }
    firePropertyChange("layer", oldLayer, layer);
  }

  protected boolean setMoveTiePointToolTip() {
    moveTiePointStarted = false;
    if (!closeSourcePixelIndexes.isEmpty()
      || !closeTargetPointIndexes.isEmpty()) {
      final List<MappedLocation> tiePoints = image.getTiePoints();
      final StringBuffer toolTip = new StringBuffer();
      toolTip.append("<html>");

      appendTiePointLocation(toolTip, tiePoints, closeTargetPointIndexes, 1,
        false);
      appendTiePointLocation(toolTip, tiePoints, closeSourcePixelIndexes,
        closeTargetPointIndexes.size() + 1, true);
      toolTip.append("</html>");
      getMap().setToolTipText(moveTiePointEventPoint, toolTip);
      return true;
    }
    return false;
  }

}
