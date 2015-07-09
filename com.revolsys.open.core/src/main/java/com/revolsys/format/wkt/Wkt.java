package com.revolsys.format.wkt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.springframework.core.io.Resource;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.io.AbstractRecordIoFactory;
import com.revolsys.data.record.io.RecordIteratorReader;
import com.revolsys.data.record.io.RecordReader;
import com.revolsys.data.record.io.RecordWriterFactory;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.io.FileUtil;
import com.revolsys.io.Writer;

public class Wkt extends AbstractRecordIoFactory implements RecordWriterFactory, WktConstants {
  public Wkt() {
    super(WktConstants.DESCRIPTION);
    addMediaTypeAndFileExtension(MEDIA_TYPE, FILE_EXTENSION);
  }

  @Override
  public RecordReader createRecordReader(final Resource resource, final RecordFactory factory) {
    try {
      final WktRecordIterator iterator = new WktRecordIterator(factory, resource);

      return new RecordIteratorReader(iterator);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to create reader for " + resource, e);
    }
  }

  @Override
  public Writer<Record> createRecordWriter(final String baseName,
    final RecordDefinition recordDefinition, final OutputStream outputStream,
    final Charset charset) {
    final OutputStreamWriter writer = FileUtil.createUtf8Writer(outputStream);
    return new WktRecordWriter(recordDefinition, writer);
  }

  @Override
  public boolean isCustomFieldsSupported() {
    return false;
  }
}
