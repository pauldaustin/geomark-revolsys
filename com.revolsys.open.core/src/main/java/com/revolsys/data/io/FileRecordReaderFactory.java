package com.revolsys.data.io;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;

import com.revolsys.data.record.ArrayRecordFactory;
import com.revolsys.data.record.RecordFactory;

public class FileRecordReaderFactory extends
  AbstractFactoryBean<RecordReader> {

  private RecordFactory factory = new ArrayRecordFactory();

  private Resource resource;

  @Override
  public RecordReader createInstance() throws Exception {
    return RecordIoFactories.recordReader(resource, factory);
  }

  @Override
  protected void destroyInstance(final RecordReader reader)
    throws Exception {
    reader.close();
    factory = null;
    resource = null;
  }

  public RecordFactory getFactory() {
    return factory;
  }

  @Override
  public Class<?> getObjectType() {
    return RecordReader.class;
  }

  public Resource getResource() {
    return resource;
  }

  public void setFactory(final RecordFactory factory) {
    this.factory = factory;
  }

  @Required
  public void setResource(final Resource resource) {
    this.resource = resource;
  }

}
