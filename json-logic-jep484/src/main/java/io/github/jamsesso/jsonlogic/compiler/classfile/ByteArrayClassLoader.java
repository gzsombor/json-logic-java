package io.github.jamsesso.jsonlogic.compiler.classfile;

import java.util.Map;

final class ByteArrayClassLoader extends ClassLoader {
  private final Map<String, byte[]> classes;

  ByteArrayClassLoader(ClassLoader parent, Map<String, byte[]> classes) {
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
