package com.revolsys.gis.data.io;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.gis.data.model.ArrayDataObject;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.io.AbstractReader;
import com.revolsys.io.Reader;

public class MapReaderDataObjectReader extends AbstractReader<DataObject>
  implements DataObjectReader, Iterator<DataObject> {

  private final DataObjectMetaData metaData;

  private final Reader<Map<String, Object>> mapReader;

  private boolean open;

  private Iterator<Map<String, Object>> mapIterator;

  public MapReaderDataObjectReader(final DataObjectMetaData metaData,
    final Reader<Map<String, Object>> mapReader) {
    this.metaData = metaData;
    this.mapReader = mapReader;
  }

  @Override
  public void close() {
    mapReader.close();
  }

  public DataObjectMetaData getMetaData() {
    return metaData;
  }

  public boolean hasNext() {
    if (!open) {
      open();
    }
    return mapIterator.hasNext();
  }

  public Iterator<DataObject> iterator() {
    return this;
  }

  public DataObject next() {
    if (hasNext()) {
      final Map<String, Object> source = mapIterator.next();
      final DataObject target = new ArrayDataObject(metaData);
      for (final Attribute attribute : metaData.getAttributes()) {
        final String name = attribute.getName();
        final Object value = source.get(name);
        if (value != null) {
          final DataType dataType = metaData.getAttributeType(name);
          final Object convertedValue = StringConverterRegistry.toObject(
            dataType, value);
          target.setValue(name, convertedValue);
        }
      }
      return target;
    } else {
      throw new NoSuchElementException();
    }
  }

  public void open() {
    open = true;
    this.mapIterator = mapReader.iterator();
  }

  public void remove() {
    mapIterator.remove();
  }
}
