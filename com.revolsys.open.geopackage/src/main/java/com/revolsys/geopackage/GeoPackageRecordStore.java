package com.revolsys.geopackage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.DelegatingConnection;
import org.sqlite.core.CoreConnection;
import org.sqlite.core.DB;

import com.revolsys.collection.map.Maps;
import com.revolsys.datatype.DataTypes;
import com.revolsys.identifier.Identifier;
import com.revolsys.io.PathName;
import com.revolsys.jdbc.JdbcConnection;
import com.revolsys.jdbc.io.AbstractJdbcRecordStore;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordDefinitionImpl;
import com.revolsys.record.schema.RecordStoreSchema;
import com.revolsys.record.schema.RecordStoreSchemaElement;

public class GeoPackageRecordStore extends AbstractJdbcRecordStore {

  private boolean initialized;

  public GeoPackageRecordStore(final DataSource dataSource) {
    super(dataSource);
  }

  public GeoPackageRecordStore(final GeoPackage geoPackage,
    final Map<String, ? extends Object> connectionProperties) {
    super(geoPackage, connectionProperties);
  }

  @Override
  protected Set<String> getDatabaseSchemaNames() {
    return Collections.emptySet();
  }

  @Override
  public Identifier getNextPrimaryKey(final String typePath) {
    return null;
  }

  @Override
  public String getRecordStoreType() {
    return "GeoPackage";
  }

  @Override
  public String getSequenceName(final RecordDefinition recordDefinition) {
    return null;
  }

  @Override
  @PostConstruct
  public void initialize() {
    setUsesSchema(false);

    final String filter = "WHERE NOT (NAME LIKE 'GPKG%' OR NAME LIKE 'RTREE%' OR NAME LIKE 'SQLITE%')";
    setSchemaTablePermissionsSql(
      "select  '/' \"SCHEMA_NAME\", name \"TABLE_NAME\", 'ALL' \"PRIVILEGE\", '' \"REMARKS\"  from sqlite_master "
        + filter + " union all "
        + "select  '/' \"SCHEMA_NAME\", name \"TABLE_NAME\", 'ALL' \"PRIVILEGE\", '' \"REMARKS\"  from sqlite_temp_master "
        + filter);

    addFieldAdder("BOOLEAN", DataTypes.BOOLEAN);
    addFieldAdder("TINYINT", DataTypes.BYTE);
    addFieldAdder("SMALLINT", DataTypes.SHORT);
    addFieldAdder("MEDIUMINT", DataTypes.INTEGER);
    addFieldAdder("INT", DataTypes.LONG);
    addFieldAdder("INTEGER", DataTypes.LONG);
    addFieldAdder("FLOAT", DataTypes.FLOAT);
    addFieldAdder("DOUBLE", DataTypes.DOUBLE);
    addFieldAdder("REAL", DataTypes.DOUBLE);
    addFieldAdder("TEXT", DataTypes.STRING);
    addFieldAdder("BLOB", DataTypes.BLOB);
    addFieldAdder("DATE", DataTypes.SQL_DATE);
    addFieldAdder("DATETIME", DataTypes.DATE_TIME);

    final GeoPackageGeometryFieldAdder geometryAdder = new GeoPackageGeometryFieldAdder();
    addFieldAdder("GEOMETRY", geometryAdder);
    addFieldAdder("POINT", geometryAdder);
    addFieldAdder("LINESTRING", geometryAdder);
    addFieldAdder("POLYGON", geometryAdder);
    addFieldAdder("GEOMETRYCOLLECTION", geometryAdder);
    addFieldAdder("MULTIPOINT", geometryAdder);
    addFieldAdder("MULTILINESTRING", geometryAdder);
    addFieldAdder("MULTIPOLYGON", geometryAdder);

    super.initialize();
    try (
      JdbcConnection connection = getJdbcConnection(true)) {
      final CoreConnection sqliteConnection = (CoreConnection)((DelegatingConnection<?>)connection
        .getConnection()).getInnermostDelegate();
      final DB db = sqliteConnection.db();
      db.enable_load_extension(true);
      try (
        Statement statement = connection.createStatement()) {
        statement.execute("select load_extension('libgpkg')");
      } finally {
        db.enable_load_extension(false);
      }
    } catch (final SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if (!this.initialized) {
      this.initialized = true;

    }
  }

  @Override
  public boolean isSchemaExcluded(final String schemaName) {
    return false;
  }

  @Override
  protected Map<PathName, ? extends RecordStoreSchemaElement> refreshSchemaElementsDo(
    final RecordStoreSchema schema, final PathName schemaPath) {
    final String schemaName = schema.getPath();
    final Map<String, String> tableDescriptionMap = new HashMap<>();
    final Map<String, List<String>> tablePermissionsMap = new TreeMap<>();
    loadSchemaTablePermissions("", tablePermissionsMap, tableDescriptionMap);

    final Map<PathName, RecordStoreSchemaElement> elementsByPath = new TreeMap<>();
    final Map<PathName, RecordDefinitionImpl> recordDefinitionMap = new TreeMap<>();
    try {
      try (
        final Connection connection = getJdbcConnection()) {
        final Set<String> tableNames = tablePermissionsMap.keySet();
        for (final String dbTableName : tableNames) {
          final String tableName = dbTableName.toUpperCase();
          final PathName typePath = schemaPath.newChild(tableName);
          setDbSchemaAndTableName(typePath, null, dbTableName);
          final RecordDefinitionImpl recordDefinition = newRecordDefinition(schema, typePath);
          final String description = tableDescriptionMap.get(dbTableName);
          recordDefinition.setDescription(description);
          final List<String> permissions = Maps.get(tablePermissionsMap, dbTableName,
            DEFAULT_PERMISSIONS);
          recordDefinition.setProperty("permissions", permissions);
          recordDefinitionMap.put(typePath, recordDefinition);
          elementsByPath.put(typePath, recordDefinition);
        }
        for (final RecordDefinitionImpl recordDefinition : recordDefinitionMap.values()) {
          final PathName pathName = recordDefinition.getPathName();
          final String tableName = getDatabaseTableName(pathName);
          final List<String> idFieldNames = new ArrayList<>();
          try (
            PreparedStatement columnStatement = connection
              .prepareStatement("PRAGMA table_info(" + tableName + ")")) {
            try (
              final ResultSet columnsRs = columnStatement.executeQuery()) {
              while (columnsRs.next()) {
                final String dbColumnName = columnsRs.getString("name");
                final String fieldName = dbColumnName.toUpperCase();
                final int sqlType = Types.OTHER;
                String dataType = columnsRs.getString("type");
                int length = -1;
                final int scale = -1;
                if (dataType.startsWith("TEXT(")) {
                  length = Integer.parseInt(dataType.substring(5, dataType.length() - 1));
                  dataType = "TEXT";
                }
                final boolean required = columnsRs.getString("notnull").equals("1");
                final boolean primaryKey = columnsRs.getString("pk").equals("1");
                if (primaryKey) {
                  idFieldNames.add(fieldName);
                }
                final Object defaultValue = columnsRs.getString("dflt_value");
                final FieldDefinition field = addField(recordDefinition, dbColumnName, fieldName,
                  dataType, sqlType, length, scale, required, null);
                field.setDefaultValue(defaultValue);
              }
            }
          }
          recordDefinition.setIdFieldNames(idFieldNames);
        }
      }
    } catch (final Throwable e) {
      throw new IllegalArgumentException("Unable to load metadata for schema " + schemaName, e);
    }

    return elementsByPath;
  }

}
