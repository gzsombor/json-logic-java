package io.github.jamsesso.jsonlogic.utils;

import com.google.gson.JsonArray;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class ArrayLike implements List<Object> {
  private final List<Object> delegate;

  public ArrayLike(Object data) {
    delegate = toList(data);
  }

  @SuppressWarnings("unchecked")
  public static List<Object> toList(Object data) {
    if (data instanceof List) {
      return ((List<Object>) data)
              .stream()
              .map(JsonLogicEvaluator::transform)
              .collect(Collectors.toList());
    }
    else if (data != null && data.getClass().isArray()) {
      List<Object> list = new ArrayList<>();

      for (int i = 0; i < Array.getLength(data); i++) {
        list.add(i, JsonLogicEvaluator.transform(Array.get(data, i)));
      }
      return list;
    }
    else if (data instanceof JsonArray) {
      return (List<Object>) JsonValueExtractor.extract((JsonArray) data);
    }
    else if (data instanceof Iterable) {
      List<Object> list = new ArrayList<>();

      for (Object item : (Iterable<Object>) data) {
        list.add(JsonLogicEvaluator.transform(item));
      }
      return list;
    }
    else {
      throw new IllegalArgumentException("ArrayLike only works with lists, iterables, arrays, or JsonArray");
    }
  }

  @SuppressWarnings("unchecked")
  public static Iterator<Object> iterator(Object data) {
    if (data instanceof List) {
      final Iterator<Object> iterator = ((List<Object>) data).iterator();
      return transformingIterator(iterator);
    }
    if (data != null && data.getClass().isArray()) {
      return new Iterator<Object>() {
        private int index = 0;

        @Override
        public boolean hasNext() {
          return index < Array.getLength(data);
        }

        @Override
        public Object next() {
          return JsonLogicEvaluator.transform(Array.get(data, index++));
        }
      };
    }
    if (data instanceof JsonArray) {
      return toList(data).iterator();
    }
    if (data instanceof Iterable) {
      return transformingIterator(((Iterable<Object>) data).iterator());
    }
    throw new IllegalArgumentException("ArrayLike only works with lists, iterables, arrays, or JsonArray");
  }

  public static Iterable<Object> iterable(Object data) {
    return () -> iterator(data);
  }

  public static boolean isEmpty(Object data) {
    if (data instanceof Collection) {
      return ((Collection<?>) data).isEmpty();
    }
    if (data != null && data.getClass().isArray()) {
      return Array.getLength(data) == 0;
    }
    if (data instanceof JsonArray) {
      return ((JsonArray) data).isEmpty();
    }
    if (data instanceof Iterable) {
      return !((Iterable<?>) data).iterator().hasNext();
    }
    throw new IllegalArgumentException("ArrayLike only works with lists, iterables, arrays, or JsonArray");
  }

  private static Iterator<Object> transformingIterator(Iterator<Object> iterator) {
    return new Iterator<Object>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Object next() {
        return JsonLogicEvaluator.transform(iterator.next());
      }
    };
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return delegate.contains(o);
  }

  @Override
  public Iterator<Object> iterator() {
    return delegate.iterator();
  }

  @Override
  public Object[] toArray() {
    return delegate.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return delegate.toArray(a);
  }

  @Override
  public boolean add(Object o) {
    throw new UnsupportedOperationException("ArrayLike is immutable");
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException("ArrayLike is immutable");
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return delegate.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<?> c) {
    throw new UnsupportedOperationException("ArrayLike is immutable");
  }

  @Override
  public boolean addAll(int index, Collection<?> c) {
    throw new UnsupportedOperationException("ArrayLike is immutable");
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException("ArrayLike is immutable");
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException("ArrayLike is immutable");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("ArrayLike is immutable");
  }

  @Override
  public Object get(int index) {
    return delegate.get(index);
  }

  @Override
  public Object set(int index, Object element) {
    throw new UnsupportedOperationException("ArrayLike is immutable");
  }

  @Override
  public void add(int index, Object element) {
    throw new UnsupportedOperationException("ArrayLike is immutable");
  }

  @Override
  public Object remove(int index) {
    throw new UnsupportedOperationException("ArrayLike is immutable");
  }

  @Override
  public int indexOf(Object o) {
    return delegate.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return delegate.lastIndexOf(o);
  }

  @Override
  public ListIterator<Object> listIterator() {
    return delegate.listIterator();
  }

  @Override
  public ListIterator<Object> listIterator(int index) {
    return delegate.listIterator(index);
  }

  @Override
  public List<Object> subList(int fromIndex, int toIndex) {
    return delegate.subList(fromIndex, toIndex);
  }

  @Override
  public String toString() {
    return Arrays.toString(delegate.toArray());
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof Iterable) {
      int i = 0;

      for (Object item : (Iterable) other) {
        if (i >= delegate.size()) {
          return false;
        }
        else if (!Objects.equals(item, delegate.get(i))) {
          return false;
        }

        i++;
      }

      return i == delegate.size();
    }

    return false;
  }

  public static boolean isEligible(Object data) {
    return data != null && (data instanceof Iterable || data.getClass().isArray());
  }
}
