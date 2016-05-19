package com.revolsys.swing.map.layer.mapguide;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.revolsys.beans.Classes;
import com.revolsys.collection.map.MapEx;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.io.PathName;
import com.revolsys.io.map.MapObjectFactoryRegistry;
import com.revolsys.logging.Logs;
import com.revolsys.record.Record;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.io.format.esri.rest.ArcGisRestCatalog;
import com.revolsys.record.io.format.mapguide.FeatureLayer;
import com.revolsys.record.query.Query;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.swing.map.layer.AbstractLayer;
import com.revolsys.swing.map.layer.LayerGroup;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.layer.arcgisrest.ArcGisRestServerRecordLayer;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.menu.Menus;
import com.revolsys.util.OS;
import com.revolsys.util.Property;

public class MapGuideWebServerRecordLayer extends AbstractRecordLayer {
  private static final String J_TYPE = "mapGuideWebServerRecordLayer";

  private static void actionAddLayer(final FeatureLayer layerDescription) {
    final Project project = Project.get();
    if (project != null) {

      LayerGroup layerGroup = project;
      final PathName layerPath = layerDescription.getPathName();
      for (final String groupName : layerPath.getParent().getElements()) {
        layerGroup = layerGroup.addLayerGroup(groupName);
      }
      final MapGuideWebServerRecordLayer layer = new MapGuideWebServerRecordLayer(layerDescription);
      layerGroup.addLayer(layer);
      if (OS.getPreferenceBoolean("com.revolsys.gis", AbstractLayer.PREFERENCE_PATH,
        AbstractLayer.PREFERENCE_NEW_LAYERS_SHOW_TABLE_VIEW, false)) {
        layer.showTableView();
      }
    }
  }

  public static void mapObjectFactoryInit() {
    MapObjectFactoryRegistry.newFactory(J_TYPE, "Map Guide Web Server Record Layer",
      ArcGisRestServerRecordLayer::new);

    final MenuFactory recordLayerDescriptionMenu = MenuFactory.getMenu(FeatureLayer.class);

    Menus.addMenuItem(recordLayerDescriptionMenu, "default", "Add Layer", "map_add",
      MapGuideWebServerRecordLayer::actionAddLayer);
  }

  private FeatureLayer webServiceLayer;

  private String url;

  private PathName layerPath;

  public MapGuideWebServerRecordLayer() {
    super(J_TYPE);
    setReadOnly(true);
  }

  public MapGuideWebServerRecordLayer(final FeatureLayer layerDescription) {
    this();
    setWebServiceLayer(layerDescription);
    setProperties(Collections.emptyMap());
  }

  public MapGuideWebServerRecordLayer(final Map<String, ? extends Object> properties) {
    this();
    setProperties(properties);
  }

  @Override
  protected void forEachRecord(final Query query, final Consumer<? super LayerRecord> consumer) {
    try (
      RecordReader reader = this.webServiceLayer.newRecordReader(this::newLayerRecord, query)) {
      for (final Record record : reader) {
        consumer.accept((LayerRecord)record);
      }
    }
  }

  public PathName getLayerPath() {
    return this.layerPath;
  }

  @Override
  public int getRecordCount(final Query query) {
    if (this.webServiceLayer == null) {
      return 0;
    } else {
      return this.webServiceLayer.getRecordCount(query);
    }
  }

  @Override
  public List<LayerRecord> getRecords(BoundingBox boundingBox) {
    if (hasGeometryField()) {
      boundingBox = convertBoundingBox(boundingBox);
      if (Property.hasValue(boundingBox)) {
        final List<LayerRecord> records = this.webServiceLayer.getRecords(this::newLayerRecord,
          boundingBox);
        return records;
      }
    }
    return Collections.emptyList();
  }

  public String getUrl() {
    return this.url;
  }

  public FeatureLayer getWebServiceLayer() {
    return this.webServiceLayer;
  }

  @Override
  protected boolean initializeDo() {
    FeatureLayer webServiceLayer = getWebServiceLayer();
    if (webServiceLayer == null) {
      final String url = getUrl();
      final PathName layerPath = getLayerPath();

      if (url == null) {
        Logs.error(this, Classes.className(this) + " requires a url: " + getPath());
        return false;
      }
      if (layerPath == null) {
        Logs.error(this, Classes.className(this) + " requires a layerPath: " + getPath());
        return false;
      }
      ArcGisRestCatalog server;
      try {
        server = ArcGisRestCatalog.newArcGisRestCatalog(url);
      } catch (final Throwable e) {
        Logs.error(this, "Unable to connect to server: " + url + " for " + getPath(), e);
        return false;
      }
      try {
        webServiceLayer = server.getWebServiceResource(layerPath, FeatureLayer.class);
      } catch (final IllegalArgumentException e) {
        Logs.error(this, "Layer is not valide: " + getPath(), e);
        return false;
      }
      if (webServiceLayer == null) {
        Logs.error(this, "Layer does not exist: " + layerPath + " for " + getPath());
        return false;
      } else {
        setWebServiceLayer(webServiceLayer);
      }
    }

    if (webServiceLayer != null) {
      final RecordDefinition recordDefinition = webServiceLayer.getRecordDefinition();
      if (recordDefinition != null) {
        setRecordDefinition(recordDefinition);
        setBoundingBox(webServiceLayer.getBoundingBox());
        // initRenderer();
        return super.initializeDo();
      }
    }
    return false;
  }

  public void setLayerPath(final PathName layerPath) {
    this.layerPath = layerPath;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public void setWebServiceLayer(final FeatureLayer layerDescription) {
    this.webServiceLayer = layerDescription;
  }

  @Override
  public MapEx toMap() {
    final MapEx map = super.toMap();
    addToMap(map, "url", this.url);
    addToMap(map, "layerPath", this.layerPath);
    return map;
  }
}