package io.github.jamsesso.jsonlogic.compiler.internal;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link javax.tools.JavaFileManager} that captures compiled {@code .class} bytes
 * in memory instead of writing them to disk.
 *
 * <p>After compilation, retrieve the bytes via {@link #getClassBytes()}.
 */
public final class InMemoryClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
  private final Map<String, byte[]> classBytes = new HashMap<>();

  public InMemoryClassFileManager(StandardJavaFileManager delegate) {
    super(delegate);
  }

  @Override
  public JavaFileObject getJavaFileForOutput(
      Location location,
      String className,
      JavaFileObject.Kind kind,
      FileObject sibling) {

    return new SimpleJavaFileObject(
        URI.create("bytes:///" + className.replace('.', '/') + kind.extension),
        kind) {
      @Override
      public OutputStream openOutputStream() {
        return new ByteArrayOutputStream() {
          @Override
          public void close() {
            classBytes.put(className, toByteArray());
          }
        };
      }
    };
  }

  /** Returns a snapshot of all compiled class bytes keyed by binary class name. */
  public Map<String, byte[]> getClassBytes() {
    return classBytes;
  }
}
