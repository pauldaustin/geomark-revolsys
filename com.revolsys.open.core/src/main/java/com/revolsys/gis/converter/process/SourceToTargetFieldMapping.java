package com.revolsys.gis.converter.process;

import java.util.HashMap;
import java.util.Map;

import com.revolsys.data.record.Record;

public class SourceToTargetFieldMapping extends
  AbstractSourceToTargetProcess<Record, Record> {
  private Map<String, SourceToTargetProcess<Record, Record>> targetFieldMappings = new HashMap<String, SourceToTargetProcess<Record, Record>>();

  public SourceToTargetFieldMapping() {
  }

  public SourceToTargetFieldMapping(
    final Map<String, SourceToTargetProcess<Record, Record>> targetFieldMappings) {
    this.targetFieldMappings = targetFieldMappings;
  }

  @Override
  public void close() {
    for (final SourceToTargetProcess<Record, Record> process : targetFieldMappings.values()) {
      process.close();
    }
  }

  @Override
  public void init() {
    for (final SourceToTargetProcess<Record, Record> process : targetFieldMappings.values()) {
      process.init();
    }
  }

  @Override
  public void process(final Record source, final Record target) {
    for (final SourceToTargetProcess<Record, Record> mapping : targetFieldMappings.values()) {
      mapping.process(source, target);
    }
  }

  @Override
  public String toString() {
    return "mapping=" + targetFieldMappings;
  }
}