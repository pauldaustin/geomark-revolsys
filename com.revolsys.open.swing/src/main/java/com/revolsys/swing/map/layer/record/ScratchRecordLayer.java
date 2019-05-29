package com.revolsys.swing.map.layer.record;

import java.util.Map;

import org.jeometry.common.data.type.DataType;
import org.jeometry.common.data.type.DataTypes;

import com.revolsys.collection.map.MapEx;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.menu.Menus;

public class ScratchRecordLayer extends ListRecordLayer {

  static {
    MenuFactory.addMenuInitializer(ScratchRecordLayer.class, menu -> {
      menu.deleteMenuItem("edit", "Save Changes");
      menu.deleteMenuItem("edit", "Cancel Changes");

      Menus.addMenuItem(menu, "edit", "Delete All Records", "table:delete",
        ScratchRecordLayer::isCanDeleteRecords, ScratchRecordLayer::clearRecords, true);
    });
  }

  public static final String TYPE_NAME = "scratchRecordLayer";

  public static ScratchRecordLayer newLayer(final Map<String, ? extends Object> config) {
    return new ScratchRecordLayer(config);
  }

  private DataType geometryType;

  public ScratchRecordLayer(final GeometryFactory geometryFactory, final DataType geometryType) {
    super(TYPE_NAME);
    setName(geometryType.getName());
    setGeometryFactory(geometryFactory);
    this.geometryType = geometryType;
  }

  public ScratchRecordLayer(final Map<String, ? extends Object> config) {
    super(TYPE_NAME);
    this.geometryType = DataTypes.GEOMETRY;
    setProperties(config);
  }

  @Override
  public DataType getGeometryType() {
    return this.geometryType;
  }

  @Override
  protected boolean initializeDo() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    final RecordDefinition recordDefinition = RecordDefinition.newRecordDefinition(geometryFactory,
      this.geometryType);
    setRecordDefinition(recordDefinition);

    return super.initializeDo();
  }

  public void setGeometryTypeName(final String geometryTypeName) {
    this.geometryType = DataTypes.getDataType(geometryTypeName);
    if (this.geometryType == null
      || !Geometry.class.isAssignableFrom(this.geometryType.getJavaClass())) {
      this.geometryType = DataTypes.GEOMETRY;
    }
  }

  @Override
  public MapEx toMap() {
    final MapEx map = super.toMap();
    final GeometryFactory geometryFactory = getGeometryFactory();
    addToMap(map, "geometryFactory", geometryFactory);
    addToMap(map, "geometryTypeName", this.geometryType.getName());

    return map;
  }
}
