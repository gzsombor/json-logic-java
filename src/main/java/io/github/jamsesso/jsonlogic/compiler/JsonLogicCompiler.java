package io.github.jamsesso.jsonlogic.compiler;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.compiler.internal.ByteArrayClassLoader;
import io.github.jamsesso.jsonlogic.compiler.internal.InMemoryClassFileManager;
import io.github.jamsesso.jsonlogic.compiler.internal.InMemoryJavaFileObject;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;

/**
 * Compiles JSON Logic rule ASTs to Java bytecode via {@link JavaCompiler} and caches the
 * resulting {@link CompiledRule} instances.
 *
 * <h2>Thread safety</h2>
 * <p>The compilation cache is a {@link ConcurrentHashMap}; {@link #compile} is safe to call
 * from multiple threads. Each unique rule JSON string is compiled at most once.
 *
 * <h2>Fallback behaviour</h2>
 * <p>If Javac reports errors for a specific rule, or if the compiled class cannot be loaded,
 * a WARNING is logged (including the generated source) and a {@link CompiledRule} that
 * delegates to the tree-walking interpreter is returned - so a single rule failure is
 * recoverable at runtime.
 *
 * <p>If the JDK compiler is entirely absent (JRE deployment), the <em>constructor</em> throws
 * {@link IllegalStateException} immediately - this is a deployment error that cannot be
 * recovered from per-rule.
 */
public final class JsonLogicCompiler {

  private static final Logger LOG = Logger.getLogger(JsonLogicCompiler.class.getName());

  private final JsonLogicEvaluator fallbackEvaluator;
  private final JavaCompiler javac;
  private final Map<String, CompiledRule> cache = new ConcurrentHashMap<>();

  /**
   * Creates a compiler backed by the given fallback evaluator.
   *
   * @param fallbackEvaluator used as the fallback for operators that are not natively compiled
   * @throws IllegalStateException if no JDK compiler is available on this JVM -
   *                               this is a deployment error (JRE instead of JDK)
   */
  public JsonLogicCompiler(JsonLogicEvaluator fallbackEvaluator) {
    this.fallbackEvaluator = fallbackEvaluator;
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new IllegalStateException(
          "javax.tools.JavaCompiler is not available. "
          + "Compilation requires a JDK -"
          + "To run without compilation, use new JsonLogic(false) or check "
          + "JsonLogic.isCompilationEnabled().");
    }
    this.javac = compiler;
  }

  /**
   * Returns a {@link CompiledRule} for {@code ast}, compiling it on first call and returning
   * the cached instance on subsequent calls.
   *
   * @param ruleJson the original JSON string (used as cache key)
   * @param ast      the parsed AST
   * @return a compiled rule, or an interpreter-backed rule if compilation fails
   */
  public CompiledRule compile(String ruleJson, JsonLogicNode ast) {
    return cache.computeIfAbsent(ruleJson, key -> compileInternal(ruleJson, ast));
  }

  /** Evicts all cached compiled rules (e.g. after {@code addOperation} is called). */
  public void invalidate() {
    cache.clear();
  }

  // -------------------------------------------------------------------------
  // Internal compilation
  // -------------------------------------------------------------------------

  private CompiledRule compileInternal(String ruleJson, JsonLogicNode ast) {
    // 1. Generate Java source
    final String className = classNameFor(ruleJson);
    final String qualifiedName = RuleSourceGenerator.GEN_PACKAGE + "." + className;
    final var generator = new RuleSourceGenerator();
    final String source = generator.generate(ast, className);
    final List<JsonLogicNode> fallbackNodes = generator.getFallbackNodes();

    LOG.fine(() -> "Generated source for " + className + ":\n" + source);

    // 2. Compile in memory
    Map<String, byte[]> classBytes;
    try {
      classBytes = compileSource(qualifiedName, source, ruleJson);
    } catch (Exception e) {
      LOG.log(Level.WARNING,
          "Compilation failed for rule, falling back to interpreter.\nRule: " + ruleJson
              + "\nSource:\n" + source,
          e);
      return interpreterFallback(ast);
    }

    if (classBytes == null) {
      // compileSource returned null - Javac errors were already logged with the generated source
      return interpreterFallback(ast);
    }

    // 3. Load the compiled class
    try {
      final var loader = new ByteArrayClassLoader(
          Thread.currentThread().getContextClassLoader(), classBytes);
      final Class<?> clazz = loader.loadClass(qualifiedName);
      final Constructor<?> ctor = clazz.getConstructor(JsonLogicEvaluator.class, JsonLogicNode[].class, String.class);
      final JsonLogicNode[] nodesArray = fallbackNodes.toArray(new JsonLogicNode[0]);
      return (CompiledRule) ctor.newInstance(fallbackEvaluator, nodesArray, ruleJson);
    } catch (Exception e) {
      LOG.log(Level.WARNING,
          "Failed to instantiate compiled rule class, falling back to interpreter. Rule: " + ruleJson, e);
      return interpreterFallback(ast);
    }
  }

  /**
   * Compiles {@code source} in memory and returns the class bytes, or {@code null} if Javac
   * reported diagnostic errors. On failure a WARNING is logged that includes both the Javac
   * error messages and the full generated source, so failures are self-contained in the log.
   */
  private Map<String, byte[]> compileSource(String className, String source, String ruleJson) {
    var diagnostics = new DiagnosticCollector<JavaFileObject>();

    try (StandardJavaFileManager stdFileManager = javac.getStandardFileManager(
        diagnostics, null, StandardCharsets.UTF_8);
         InMemoryClassFileManager fileManager = new InMemoryClassFileManager(stdFileManager)) {

      final var sourceFile = new InMemoryJavaFileObject(className, source);

      // Use the same source/target version as the running JVM
      final String release = String.valueOf(Runtime.version().feature());
      final List<String> options = Arrays.asList("-source", release, "-target", release);

      final JavaCompiler.CompilationTask task = javac.getTask(
          new StringWriter(), // discard Javac's own output stream - we use diagnostics
          fileManager,
          diagnostics,
          options,
          null,
          Collections.singletonList(sourceFile));

      final boolean success = task.call();

      if (!success) {
        final var sb = new StringBuilder(
            "Javac reported errors compiling rule for '" + ruleJson + "':\n");
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
          if (d.getKind() == Diagnostic.Kind.ERROR) {
            sb.append("  line ").append(d.getLineNumber())
              .append(": ").append(d.getMessage(null)).append("\n");
          }
        }
        sb.append("Generated source:\n").append(source);
        LOG.warning(sb.toString());
        return null;
      }

      return fileManager.getClassBytes();
    } catch (Exception e) {
      throw new RuntimeException("Error during in-memory compilation", e);
    }
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  /** A {@link CompiledRule} that simply delegates to the interpreter. */
  private CompiledRule interpreterFallback(JsonLogicNode ast) {
    return data -> fallbackEvaluator.evaluate(ast, data);
  }

  /**
   * Derives a stable, valid Java class name from the rule JSON.
   * Uses a simple polynomial hash to keep the name short.
   */
  static String classNameFor(String ruleJson) {
    // FNV-1a 64-bit hash for a compact, collision-resistant identifier
    long hash = 0xcbf29ce484222325L;
    for (int i = 0; i < ruleJson.length(); i++) {
      hash ^= ruleJson.charAt(i);
      hash *= 0x100000001b3L;
    }
    // Format as unsigned hex to avoid leading minus sign
    return "Rule_" + Long.toUnsignedString(hash, 16);
  }
}
