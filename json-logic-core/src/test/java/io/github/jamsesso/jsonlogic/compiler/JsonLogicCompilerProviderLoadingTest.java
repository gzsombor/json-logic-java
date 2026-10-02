package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a provider which cannot be loaded on the running JVM (e.g. the JDK 24 based
 * provider on JDK 11) is skipped instead of failing the creation of the compiler.
 */
public class JsonLogicCompilerProviderLoadingTest {
  private static final String SERVICE_FILE =
      "META-INF/services/" + JsonLogicCompilerProvider.class.getName();
  private static final String BROKEN_PROVIDER = "test.broken.FutureJvmProvider";

  @TempDir
  Path tempDir;

  @Test
  public void providerCompiledForNewerJvmIsSkipped() throws Exception {
    writeClassWithMajorVersion(BROKEN_PROVIDER, Runtime.version().feature() + 44 + 1);
    // the broken provider is listed first, so it is encountered before the working one
    writeServiceFile(BROKEN_PROVIDER, WorkingProvider.class.getName());

    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader loader = new URLClassLoader(
        new URL[] {tempDir.toUri().toURL()}, getClass().getClassLoader())) {
      Thread.currentThread().setContextClassLoader(loader);
      JsonLogicCompiler compiler = new JsonLogicCompiler((JsonLogicEvaluator) null, true);
      assertTrue(compiler.isStrictMode(), "the working provider must have been used");
      assertEquals(1, WorkingProvider.created);
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  private void writeServiceFile(String... classNames) throws IOException {
    Path file = tempDir.resolve(SERVICE_FILE);
    Files.createDirectories(file.getParent());
    Files.write(file, String.join("\n", classNames).getBytes(StandardCharsets.UTF_8));
  }

  /** Writes only a class file header: the JVM rejects the version before reading anything else. */
  private void writeClassWithMajorVersion(String className, int major) throws IOException {
    Path file = tempDir.resolve(className.replace('.', '/') + ".class");
    Files.createDirectories(file.getParent());
    try (OutputStream out = Files.newOutputStream(file);
        DataOutputStream data = new DataOutputStream(out)) {
      data.writeInt(0xCAFEBABE);
      data.writeShort(0);
      data.writeShort(major);
    }
  }

  public static class WorkingProvider implements JsonLogicCompilerProvider {
    static int created;

    @Override
    public int priority() {
      return 0;
    }

    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    public JsonLogicCompilerImplementation create(
        JsonLogicEvaluator fallbackEvaluator, boolean strictMode, boolean fallbackEnabled) {
      created++;
      return new JsonLogicCompilerImplementation() {
        @Override
        public boolean isStrictMode() {
          return strictMode;
        }

        @Override
        public CompiledRule compile(String ruleJson, JsonLogicNode ast) {
          throw new UnsupportedOperationException();
        }
      };
    }
  }
}
