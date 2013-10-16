package com.revolsys.gis.data.io;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;

import com.revolsys.collection.ResultPager;
import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectFactory;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectMetaDataFactory;
import com.revolsys.gis.data.model.codes.CodeTable;
import com.revolsys.gis.data.query.Query;
import com.revolsys.gis.io.StatisticsMap;
import com.revolsys.io.Reader;
import com.revolsys.io.Writer;
import com.vividsolutions.jts.geom.Geometry;

public interface DataObjectStore extends DataObjectMetaDataFactory {
  void addCodeTable(CodeTable codeTable);

  void addCodeTables(Collection<CodeTable> codeTables);

  void addStatistic(String name, DataObject object);

  void addStatistic(String name, String typePath, int count);

  void close();

  DataObject create(DataObjectMetaData metaData);

  DataObject create(String typePath);

  <T> T createPrimaryIdValue(String typePath);

  Query createQuery(final String typePath, String whereClause,
    final BoundingBox boundingBox);

  DataObject createWithId(DataObjectMetaData objectMetaData);

  Writer<DataObject> createWriter();

  void delete(DataObject object);

  int delete(Query query);

  void deleteAll(Collection<DataObject> objects);

  CodeTable getCodeTable(String typePath);

  CodeTable getCodeTableByColumn(String columnName);

  Map<String, CodeTable> getCodeTableByColumnMap();

  DataObjectFactory getDataObjectFactory();

  String getLabel();

  DataObjectMetaData getMetaData(DataObjectMetaData metaData);

  /**
   * Get the meta data for the specified type.
   * 
   * @param typePath The type name.
   * @return The meta data.
   */
  @Override
  DataObjectMetaData getMetaData(String typePath);

  int getRowCount(Query query);

  DataObjectStoreSchema getSchema(final String schemaName);

  /**
   * Get the list of name space names provided by the data store.
   * 
   * @return The name space names.
   */
  List<DataObjectStoreSchema> getSchemas();

  StatisticsMap getStatistics();

  PlatformTransactionManager getTransactionManager();

  /**
   * Get the list of type names (including name space) in the name space.
   * 
   * @param namespace The name space.
   * @return The type names.
   */
  List<String> getTypeNames(String namespace);

  List<DataObjectMetaData> getTypes(String namespace);

  Writer<DataObject> getWriter();

  boolean hasSchema(String name);

  void initialize();

  void insert(DataObject object);

  void insertAll(Collection<DataObject> objects);

  boolean isEditable(String typePath);

  DataObject load(String typePath, Object id);

  DataObject lock(String typePath, Object id);

  ResultPager<DataObject> page(Query query);

  Reader<DataObject> query(DataObjectFactory dataObjectFactory,
    String typePath, BoundingBox boundingBox);

  Reader<DataObject> query(DataObjectFactory dataObjectFactory,
    String typePath, Geometry geometry);

  Reader<DataObject> query(DataObjectFactory dataObjectFactory,
    String typePath, Geometry geometry, double distance);

  Reader<DataObject> query(List<Query> queries);

  Reader<DataObject> query(Query... queries);

  Reader<DataObject> query(String typePath);

  DataObject queryFirst(Query query);

  void setDataObjectFactory(DataObjectFactory dataObjectFactory);

  void setLabel(String label);

  void update(DataObject object);

  void updateAll(Collection<DataObject> objects);
}
