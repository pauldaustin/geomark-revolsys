package com.revolsys.collection.list;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import com.revolsys.util.Property;

public interface Lists {
  public static <V> void addAll(final List<V> list, final Iterable<? extends V> values) {
    if (values != null) {
      for (final V value : values) {
        list.add(value);
      }
    }
  }

  public static <V> void addAll(final List<V> list,
    @SuppressWarnings("unchecked") final V... values) {
    if (values != null) {
      for (final V value : values) {
        list.add(value);
      }
    }
  }

  /**
  * Add the value to the list if it is not empty and not already in the list.
  * @param list
  * @param value
  * @return
  */
  public static <V> boolean addNotContains(final List<V> list, final int index, final V value) {
    if (Property.hasValue(value)) {
      if (!list.contains(value)) {
        list.add(index, value);
        return true;
      }
    }
    return false;
  }

  /**
   * Add the value to the list if it is not empty and not already in the list.
   * @param list
   * @param value
   * @return
   */
  public static <V> boolean addNotContains(final List<V> list, final V value) {
    if (Property.hasValue(value)) {
      if (!list.contains(value)) {
        return list.add(value);
      }
    }
    return false;
  }

  /**
   * Add the value to the list if it is not empty and not already in the list.
   * @param list
   * @param value
   * @return
   */
  public static <V> boolean addNotContainsLast(final List<V> list, final V value) {
    if (Property.hasValue(value)) {
      if (list.isEmpty() || !list.get(list.size() - 1).equals(value)) {
        list.add(value);
        return true;
      }
    }
    return false;
  }

  /**
   * Add the value to the list if it is not empty.
   * @param list
   * @param value
   * @return
   */
  public static <V> boolean addNotEmpty(final List<V> list, final int index, final V value) {
    if (Property.hasValue(value)) {
      list.add(index, value);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Add the value to the list if it is not empty.
   * @param list
   * @param value
   * @return
   */
  public static <V> boolean addNotEmpty(final List<V> list, final V value) {
    if (Property.hasValue(value)) {
      return list.add(value);
    } else {
      return false;
    }
  }

  static List<Double> array(final double... values) {
    if (values == null) {
      return Collections.emptyList();
    } else {
      final List<Double> list = new ArrayList<Double>();
      for (final double value : values) {
        list.add(value);
      }
      return list;
    }
  }

  static List<Integer> array(final int... values) {
    if (values == null) {
      return Collections.emptyList();
    } else {
      final List<Integer> list = new ArrayList<Integer>();
      for (final int value : values) {
        list.add(value);
      }
      return list;
    }
  }

  public static <V> List<V> array(final Iterable<? extends V> values) {
    final List<V> list = new ArrayList<>();
    addAll(list, values);
    return list;
  }

  static <T> List<T> array(final Iterable<T> iterable, final int size) {
    final List<T> list = new ArrayList<T>(size);
    int i = 0;
    for (final T value : iterable) {
      if (i < size) {
        list.add(value);
        i++;
      } else {
        return list;
      }
    }
    return list;
  }

  public static <V> ArrayList<V> array(@SuppressWarnings("unchecked") final V... values) {
    final ArrayList<V> list = new ArrayList<>();
    addAll(list, values);
    return list;
  }

  public static <V> Supplier<List<V>> arrayFactory() {
    return () -> {
      return new ArrayList<V>();
    };
  }

  static List<? extends Object> arrayToList(final Object value) {
    final List<Object> list = new ArrayList<Object>();
    if (value instanceof boolean[]) {
      for (final Object item : (boolean[])value) {
        list.add(item);
      }
    } else if (value instanceof Object[]) {
      for (final Object item : (Object[])value) {
        list.add(item);
      }
    } else if (value instanceof byte[]) {
      for (final Object item : (byte[])value) {
        list.add(item);
      }
    } else if (value instanceof short[]) {
      for (final Object item : (short[])value) {
        list.add(item);
      }
    } else if (value instanceof int[]) {
      for (final Object item : (int[])value) {
        list.add(item);
      }
    } else if (value instanceof long[]) {
      for (final Object item : (long[])value) {
        list.add(item);
      }
    } else if (value instanceof float[]) {
      for (final Object item : (float[])value) {
        list.add(item);
      }
    } else if (value instanceof double[]) {
      for (final Object item : (double[])value) {
        list.add(item);
      }
    } else {
      list.add(value);
    }
    return list;
  }

  static <T> boolean containsReference(final List<WeakReference<T>> list, final T object) {
    for (int i = 0; i < list.size(); i++) {
      final WeakReference<T> reference = list.get(i);
      final T value = reference.get();
      if (value == null) {
        list.remove(i);
      } else if (value == object) {
        return true;
      }
    }
    return false;
  }

  static <T> List<T> getReferences(final List<WeakReference<T>> list) {
    final List<T> values = new ArrayList<T>();
    for (int i = 0; i < list.size(); i++) {
      final WeakReference<T> reference = list.get(i);
      final T value = reference.get();
      if (value == null) {
        list.remove(i);
      } else {
        values.add(value);
      }
    }
    return values;
  }

  public static <V> LinkedList<V> linked(@SuppressWarnings("unchecked") final V... values) {
    final LinkedList<V> list = new LinkedList<>();
    addAll(list, values);
    return list;
  }

  public static <V> Supplier<List<V>> linkedFactory() {
    return () -> {
      return new LinkedList<V>();
    };
  }

  static <T> void removeReference(final List<WeakReference<T>> list, final T object) {
    for (int i = 0; i < list.size(); i++) {
      final WeakReference<T> reference = list.get(i);
      final T value = reference.get();
      if (value == null) {
        list.remove(i);
      } else if (value == object) {
        list.remove(i);
      }
    }
  }

  static List<String> split(final String text, final String regex) {
    if (Property.hasValue(text)) {
      return Arrays.asList(text.split(regex));
    } else {
      return Collections.emptyList();
    }
  }

  public static <V> List<V> unmodifiable(final Iterable<? extends V> values) {
    return new UnmodifiableArrayList<V>(values);
  }

  public static <V> List<V> unmodifiable(@SuppressWarnings("unchecked") final V... values) {
    return new UnmodifiableArrayList<V>(values);
  }
}
