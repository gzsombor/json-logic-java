package io.github.jamsesso.jsonlogic.compiler.internal;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * Wraps a Java source {@code String} as a {@link javax.tools.JavaFileObject} so that
 * {@link javax.tools.JavaCompiler} can compile it without touching the file system.
 */
public final class InMemoryJavaFileObject extends SimpleJavaFileObject {
  private final String source;

  public InMemoryJavaFileObject(String className, String source) {
    super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
    this.source = source;
  }

  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
    return source;
  }
}
