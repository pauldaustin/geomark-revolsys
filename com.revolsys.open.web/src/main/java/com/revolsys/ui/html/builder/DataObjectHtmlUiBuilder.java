package com.revolsys.ui.html.builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

import com.revolsys.collection.ResultPager;
import com.revolsys.gis.data.io.DataObjectStore;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectState;
import com.revolsys.gis.data.query.Query;
import com.revolsys.gis.model.data.equals.EqualsRegistry;
import com.revolsys.io.Reader;
import com.revolsys.io.Writer;
import com.revolsys.ui.html.serializer.key.KeySerializer;
import com.revolsys.ui.web.utils.HttpRequestUtils;

public class DataObjectHtmlUiBuilder extends HtmlUiBuilder<DataObject> {

  private DataObjectStore dataStore;

  private String tableName;

  public DataObjectHtmlUiBuilder() {
  }

  public DataObjectHtmlUiBuilder(final String typePath, final String title) {
    super(typePath, title);
  }

  public DataObjectHtmlUiBuilder(final String typePath, final String title,
    final String pluralTitle) {
    super(typePath, title, pluralTitle);
  }

  public DataObjectHtmlUiBuilder(final String typePath, String tableName,
    String idPropertyName, final String title, final String pluralTitle) {
    super(typePath, title, pluralTitle);
    this.tableName = tableName;
    setIdPropertyName(idPropertyName);
  }

  @Override
  protected DataObject createObject() {
    return dataStore.create(tableName);
  }

  public void deleteObject(final Object id) {
    final DataObject object = loadObject(id);
    if (object != null) {
      final Writer<DataObject> writer = dataStore.createWriter();
      object.setState(DataObjectState.Deleted);

      writer.write(object);
      writer.close();
    }
  }

  @Override
  @PreDestroy
  public void destroy() {
    super.destroy();
    dataStore = null;
    tableName = null;
  }

  public DataObjectStore getDataStore() {
    return dataStore;
  }

  public ResultPager<DataObject> getResultPager(final Query query) {
    return dataStore.page(query);
  }

  public String getTableName() {
    return tableName;
  }

  @Override
  protected void insertObject(final DataObject object) {
    final Writer<DataObject> writer = dataStore.createWriter();
    try {
      if (object.getIdValue() == null) {
        object.setIdValue(dataStore.createPrimaryIdValue(tableName));
      }
      writer.write(object);
    } finally {
      writer.close();
    }
  }

  protected boolean isPropertyUnique(final DataObject object,
    final String attributeName) {
    final Query query = new Query(tableName);
    final String value = object.getValue(attributeName);
    final Map<String, String> filter = Collections.singletonMap(attributeName,
      value);
    query.setFilter(filter);
    final DataObjectStore dataStore = getDataStore();
    final Reader<DataObject> results = dataStore.query(query);
    final List<DataObject> objects = results.read();
    if (object.getState() == DataObjectState.New) {
      return objects.isEmpty();
    } else {
      final Object id = object.getIdValue();
      for (final Iterator<DataObject> iterator = objects.iterator(); iterator.hasNext();) {
        final DataObject matchedObject = iterator.next();
        final Object matchedId = matchedObject.getIdValue();
        if (EqualsRegistry.INSTANCE.equals(id, matchedId)) {
          iterator.remove();
        }
      }
      return objects.isEmpty();
    }

  }

  @Override
  public DataObject loadObject(final Object id) {
    final DataObject object = dataStore.load(tableName, id);
    return object;
  }

  public void setDataStore(final DataObjectStore dataStore) {
    this.dataStore = dataStore;
  }

  public void setTableName(final String tableName) {
    this.tableName = tableName;
  }

  @Override
  protected void updateObject(final DataObject object) {
    final Writer<DataObject> writer = dataStore.createWriter();
    try {
      writer.write(object);
    } finally {
      writer.close();
    }
  }

  public Object createDataTableHandler(final HttpServletRequest request,
    String pageName) {
    Map<String, Object> parameters = Collections.emptyMap();
    return createDataTableHandler(request, pageName, parameters);
  }

  public Object createDataTableHandler(final HttpServletRequest request,
    String pageName, Map<String, Object> parameters) {
    String search = request.getParameter("sSearch");
    if (search == null) {
      return createDataTable(request, pageName, 400, parameters);
    } else {
      final Query query = new Query(getTableName());
      if (StringUtils.hasText(search)) {
        List<KeySerializer> serializers = getSerializers(pageName, "list");
        StringBuffer whereClause = new StringBuffer();
        int numSortColumns = HttpRequestUtils.getIntegerParameter(request,
          "iColumns");
        for (int i = 0; i < numSortColumns; i++) {
          if (HttpRequestUtils.getBooleanParameter(request, "bSearchable_" + i)) {
            if (whereClause.length() > 0) {
              whereClause.append(" OR ");
            }
            KeySerializer serializer = serializers.get(i);
            String columnName = serializer.getKey();
            whereClause.append(columnName);
            whereClause.append(" LIKE ?");
            query.addParameter("%" + search + "%");
          }
          if (whereClause.length() > 0) {
            query.setWhereClause(whereClause.toString());
          }
        }
      }

      final Map<String, Boolean> orderBy = getDataTableSortOrder(request);
      query.setOrderBy(orderBy);

      final ResultPager<DataObject> pager = getResultPager(query);
      try {
        return createDataTableMap(request, pager, pageName);
      } finally {
        pager.close();
      }

    }
  }

}
