package com.revolsys.json;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Map;

import com.revolsys.gis.data.io.AbstractDataObjectAndGeometryWriterFactory;
import com.revolsys.gis.data.model.ArrayDataObject;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.io.IoConstants;
import com.revolsys.io.Writer;

public class JsonDataObjectIoFactory extends
  AbstractDataObjectAndGeometryWriterFactory {

  /** The factory instance. */
  public static final JsonDataObjectIoFactory INSTANCE = new JsonDataObjectIoFactory();

  public static final DataObject toDataObject(
    final DataObjectMetaData metaData, final String string) {
    final StringReader in = new StringReader(string);
    final JsonDataObjectIterator iterator = new JsonDataObjectIterator(
      metaData, in, true);
    try {
      if (iterator.hasNext()) {
        return iterator.next();
      } else {
        return null;
      }
    } finally {
      iterator.close();
    }
  }

  public static final String toString(final DataObject object) {
    final DataObjectMetaData metaData = object.getMetaData();
    final StringWriter writer = new StringWriter();
    final JsonDataObjectWriter dataObjectWriter = new JsonDataObjectWriter(
      metaData, writer);
    dataObjectWriter.setProperty(IoConstants.SINGLE_OBJECT_PROPERTY,
      Boolean.TRUE);
    dataObjectWriter.write(object);
    dataObjectWriter.close();
    return writer.toString();
  }

  public static String toString(final DataObjectMetaData metaData,
    final Map<String, ? extends Object> parameters) {
    DataObject object = new ArrayDataObject(metaData);
    for (String attributeName : metaData.getAttributeNames()) {
      Object value = parameters.get(attributeName);
      object.setValue(attributeName, value);
    }
    return toString(object);
  }

  public JsonDataObjectIoFactory() {
    super("JavaScript Object Notation");
    addMediaTypeAndFileExtension("application/json", "json");
  }

  public Writer<DataObject> createDataObjectWriter(final String baseName,
    final DataObjectMetaData metaData, final OutputStream outputStream,
    final Charset charset) {
    return new JsonDataObjectWriter(metaData, new OutputStreamWriter(
      outputStream, charset));
  }

}
