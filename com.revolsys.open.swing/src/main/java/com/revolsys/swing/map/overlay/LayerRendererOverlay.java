package com.revolsys.swing.map.overlay;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JComponent;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.raster.BufferedGeoreferencedImage;
import com.revolsys.raster.GeoreferencedImage;
import com.revolsys.swing.map.MapPanel;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.BaseMapLayerGroup;
import com.revolsys.swing.map.layer.Layer;
import com.revolsys.swing.map.layer.LayerRenderer;
import com.revolsys.swing.map.layer.NullLayer;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.layer.raster.GeoreferencedImageLayerRenderer;
import com.revolsys.swing.map.layer.raster.TiledImageLayerRenderer;
import com.revolsys.swing.parallel.Invoke;
import com.revolsys.util.Property;

/**
 * <p>A lightweight component that users the {@link Layer}'s {@link LayerRenderer} to render the layer.</p>
 */
public class LayerRendererOverlay extends JComponent implements PropertyChangeListener {
  private static final Collection<String> IGNORE_PROPERTY_NAMES = new HashSet<>(Arrays
    .asList("selectionCount", "hasHighlightedRecords", "highlightedCount", "scale", "loaded"));

  private static final long serialVersionUID = 1L;

  private GeoreferencedImage image;

  private LayerRendererOverlaySwingWorker imageWorker;

  private Layer layer;

  private boolean loadImage = true;

  private final Object loadSync = new Object();

  private Viewport2D viewport;

  public LayerRendererOverlay(final MapPanel mapPanel) {
    this(mapPanel, null);
  }

  public LayerRendererOverlay(final MapPanel mapPanel, final Layer layer) {
    this.viewport = mapPanel.getViewport();
    setLayer(layer);
    Property.addListener(this.viewport, this);
    Property.addListener(this, mapPanel);
  }

  public void dispose() {
    if (this.layer != null) {
      Property.removeListener(this.layer, this);
      this.layer = null;
    }
    Property.removeAllListeners(this);
    this.image = null;
    this.imageWorker = null;
    this.viewport = null;
  }

  public Layer getLayer() {
    return this.layer;
  }

  public Project getProject() {
    return this.layer.getProject();
  }

  public Viewport2D getViewport() {
    return this.viewport;
  }

  @Override
  public void paintComponent(final Graphics g) {
    if (!(this.layer instanceof NullLayer)) {
      GeoreferencedImage image;
      synchronized (this.loadSync) {
        image = this.image;

        if ((image == null || this.loadImage) && this.imageWorker == null) {
          final BoundingBox boundingBox = this.viewport.getBoundingBox();
          final int viewWidthPixels = this.viewport.getViewWidthPixels();
          final int viewHeightPixels = this.viewport.getViewHeightPixels();
          final GeoreferencedImage loadImage = new BufferedGeoreferencedImage(boundingBox,
            viewWidthPixels, viewHeightPixels);
          this.imageWorker = new LayerRendererOverlaySwingWorker(this, loadImage);
          Invoke.worker(this.imageWorker);
        }
      }
      if (image != null) {
        render((Graphics2D)g);
      }
    }
  }

  @Override
  public void propertyChange(final PropertyChangeEvent e) {
    if (!(e.getSource() instanceof MapPanel)) {
      final String propertyName = e.getPropertyName();
      if (!IGNORE_PROPERTY_NAMES.contains(propertyName)) {
        if (this.layer instanceof Project) {
          if (TiledImageLayerRenderer.TILES_LOADED.equals(propertyName)) {
            return;
          }
        }
        redraw();
      }
    }
  }

  public void redraw() {
    final Container parent = getParent();
    if (getWidth() > 0 && getHeight() > 0) {
      if (parent != null && parent.isVisible()) {
        if (this.layer != null && this.layer.isExists() && this.layer.isVisible()) {
          synchronized (this.loadSync) {
            this.loadImage = true;
            if (this.imageWorker != null) {
              this.imageWorker.cancel(true);
              this.imageWorker = null;
            }
            firePropertyChange("imageLoaded", true, false);
          }
        }
      }
    }
  }

  public void refresh() {
    if (this.layer != null) {
      this.layer.refresh();
    }
  }

  private void render(final Graphics2D graphics) {
    GeoreferencedImageLayerRenderer.render(this.viewport, graphics, this.image, false);
  }

  public void setImage(final LayerRendererOverlaySwingWorker imageWorker) {
    synchronized (this.loadSync) {
      if (this.imageWorker == imageWorker) {
        this.image = imageWorker.getReferencedImage();
        if (this.image != null) {
          this.loadImage = false;
          this.imageWorker = null;
        }
        firePropertyChange("imageLoaded", false, true);
      }
    }
  }

  public void setLayer(final Layer layer) {
    final Layer old = this.layer;
    if (old != layer) {
      if (old != null) {
        if (old.getParent() instanceof BaseMapLayerGroup) {
          old.setVisible(false);
        }
        Property.removeListener(old, this);
      }
      this.layer = layer;
      if (layer != null) {
        Property.addListener(layer, this);
        if (layer.getParent() instanceof BaseMapLayerGroup) {
          layer.setVisible(true);
        }
        if (layer.isInitialized()) {
          layer.refresh();
        }
      }
      this.image = null;
      redraw();
      firePropertyChange("layer", old, layer);
    }
  }
}
