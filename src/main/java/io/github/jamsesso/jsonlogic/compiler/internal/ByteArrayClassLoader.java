package io.github.jamsesso.jsonlogic.compiler.internal;

import java.util.Map;

/**
 * A {@link ClassLoader} that defines classes from raw byte arrays produced by
 * {@link InMemoryClassFileManager}.
 *
 * <p>Each {@link io.github.jamsesso.jsonlogic.compiler.JsonLogicCompiler} instance
 * creates its own {@code ByteArrayClassLoader} so that compiled rule classes are
 * isolated and can be GC'd together with the compiler.
 */
public final class ByteArrayClassLoader extends ClassLoader {
  private final Map<String, byte[]> classes;

  public ByteArrayClassLoader(ClassLoader parent, Map<String, byte[]> classes) {
    super(parent);
    this.classes = classes;
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    final byte[] bytes = classes.get(name);
    if (bytes == null) {
      throw new ClassNotFoundException(name);
    }
    return defineClass(name, bytes, 0, bytes.length);
  }
}
