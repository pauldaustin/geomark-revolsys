package com.revolsys.record.query.functions;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.revolsys.equals.Equals;
import com.revolsys.record.Record;
import com.revolsys.record.query.Query;
import com.revolsys.record.query.QueryValue;
import com.revolsys.record.schema.RecordStore;
import com.revolsys.util.Strings;

public class Function implements QueryValue {

  private final String name;

  private List<QueryValue> parameters;

  public Function(final String name, final List<QueryValue> parameters) {
    this.name = name;
    this.parameters = new ArrayList<QueryValue>(parameters);
  }

  public Function(final String name, final QueryValue... parameters) {
    this(name, Arrays.asList(parameters));
  }

  public void add(final QueryValue value) {
    this.parameters.add(value);
  }

  @Override
  public void appendDefaultSql(final Query query, final RecordStore recordStore,
    final StringBuilder buffer) {
    buffer.append(this.name);
    buffer.append("(");
    boolean first = true;
    for (final QueryValue parameter : getParameters()) {
      if (first) {
        first = false;
      } else {
        buffer.append(", ");
      }
      parameter.appendSql(query, recordStore, buffer);
    }
    buffer.append(")");
  }

  @Override
  public int appendParameters(int index, final PreparedStatement statement) {
    for (final QueryValue value : getParameters()) {
      index = value.appendParameters(index, statement);
    }
    return index;
  }

  public void clear() {
    this.parameters.clear();
  }

  @Override
  public Function clone() {
    try {
      final Function clone = (Function)super.clone();
      clone.parameters = QueryValue.cloneQueryValues(this.parameters);
      return clone;
    } catch (final CloneNotSupportedException e) {
      return null;
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Function) {
      final Function value = (Function)obj;
      if (Equals.equal(getName(), value.getName())) {
        final List<QueryValue> parameters1 = getParameters();
        final List<QueryValue> parameters2 = value.getParameters();
        if (parameters1.size() == parameters2.size()) {
          for (int i = 0; i < parameters1.size(); i++) {
            final QueryValue value1 = parameters1.get(i);
            final QueryValue value2 = parameters2.get(i);
            if (!Equals.equal(value1, value2)) {
              return false;
            }
          }
          return true;
        }
      }
    }
    return false;
  }

  public String getName() {
    return this.name;
  }

  public QueryValue getParameter(final int index) {
    final List<QueryValue> parameters = getParameters();
    if (index >= 0 && index < parameters.size()) {
      return parameters.get(index);
    } else {
      return null;
    }
  }

  public List<QueryValue> getParameters() {
    return Collections.unmodifiableList(this.parameters);
  }

  public String getParameterStringValue(final int index, final Record record) {
    final QueryValue parameter = getParameter(index);
    if (parameter == null) {
      return null;
    } else {
      return parameter.getStringValue(record);
    }
  }

  public <V> V getParameterValue(final int index, final Record record) {
    final QueryValue parameter = getParameter(index);
    if (parameter == null) {
      return null;
    } else {
      final V value = parameter.getValue(record);
      return value;
    }
  }

  @Override
  public List<QueryValue> getQueryValues() {
    return Collections.<QueryValue> unmodifiableList(this.parameters);

  }

  @Override
  public <V> V getValue(final Record record) {
    throw new UnsupportedOperationException("Function is not supported" + getName());
  }

  @Override
  public String toString() {
    final List<QueryValue> parameters = getParameters();
    return getName() + "(" + Strings.toString(", ", parameters) + ")";
  }
}