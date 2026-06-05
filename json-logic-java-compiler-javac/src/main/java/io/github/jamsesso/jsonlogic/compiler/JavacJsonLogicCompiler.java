package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.compiler.internal.ByteArrayClassLoader;
import io.github.jamsesso.jsonlogic.compiler.internal.InMemoryClassFileManager;
import io.github.jamsesso.jsonlogic.compiler.internal.InMemoryJavaFileObject;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Compiles JSON Logic rule ASTs to Java bytecode via {@link JavaCompiler}. */
public final class JavacJsonLogicCompiler implements JsonLogicCompilerImplementation {

  private static final Logger LOG = Logger.getLogger(JavacJsonLogicCompiler.class.getName());

  private final JsonLogicEvaluator fallbackEvaluator;
  private final JavaCompiler javac;
  private final boolean strictMode;

  public JavacJsonLogicCompiler(JsonLogicEvaluator fallbackEvaluator) {
    this(fallbackEvaluator, false);
  }

  public JavacJsonLogicCompiler(JsonLogicEvaluator fallbackEvaluator, boolean strictMode) {
    this.fallbackEvaluator = fallbackEvaluator;
    this.strictMode = strictMode;
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new IllegalStateException(
          "javax.tools.JavaCompiler is not available. "
              + "Compilation requires the 'jdk.compiler' module to provide a compiler.");
    }
    this.javac = compiler;
  }

  @Override
  public boolean isStrictMode() {
    return strictMode;
  }

  @Override
  public CompiledRule compile(String ruleJson, JsonLogicNode ast) throws JsonLogicCompilationException {
    final String className = classNameFor(ruleJson);
    final String qualifiedName = RuleSourceGenerator.GEN_PACKAGE + "." + className;
    final var generator = new RuleSourceGenerator();
    final String source = generator.generate(ast, className);
    final List<JsonLogicNode> fallbackNodes = generator.getFallbackNodes();

    LOG.fine(() -> "Generated source for " + className + ":\n" + source);

    Map<String, byte[]> classBytes;
    try {
      classBytes = compileSource(qualifiedName, source, ruleJson);
    } catch (Exception e) {
      if (strictMode) {
        throw new JsonLogicCompilationException(
            "Compilation failed for rule: " + ruleJson + "\nSource:\n" + source, e);
      }
      LOG.log(Level.WARNING,
          "Compilation failed for rule, falling back to interpreter.\nRule: " + ruleJson
              + "\nSource:\n" + source,
          e);
      return interpreterFallback(ast);
    }

    if (classBytes == null) {
      if (strictMode) {
        throw new JsonLogicCompilationException(
            "Javac errors during compilation for rule: " + ruleJson + "\nSource:\n" + source);
      }
      return interpreterFallback(ast);
    }

    try {
      final var loader = new ByteArrayClassLoader(
          Thread.currentThread().getContextClassLoader(), classBytes);
      final Class<?> clazz = loader.loadClass(qualifiedName);
      final Constructor<?> ctor = clazz.getConstructor(
          JsonLogicEvaluator.class, JsonLogicNode[].class, String.class);
      final JsonLogicNode[] nodesArray = fallbackNodes.toArray(new JsonLogicNode[0]);
      return (CompiledRule) ctor.newInstance(fallbackEvaluator, nodesArray, ruleJson);
    } catch (Exception e) {
      if (strictMode) {
        throw new JsonLogicCompilationException(
            "Failed to instantiate compiled rule class for rule: " + ruleJson + "\nSource:\n" + source, e);
      }
      LOG.log(Level.WARNING,
          "Failed to instantiate compiled rule class, falling back to interpreter. Rule: " + ruleJson, e);
      return interpreterFallback(ast);
    }
  }

  /** Returns generated javac class bytes keyed by binary class name for inspection tooling. */
  public Map<String, byte[]> compileClassBytes(String ruleJson, JsonLogicNode ast) {
    final String className = classNameFor(ruleJson);
    final String qualifiedName = RuleSourceGenerator.GEN_PACKAGE + "." + className;
    final String source = new RuleSourceGenerator().generate(ast, className);
    final Map<String, byte[]> classBytes = compileSource(qualifiedName, source, ruleJson);
    if (classBytes == null) {
      throw new JsonLogicCompilationException("Javac errors during compilation for rule: " + ruleJson);
    }
    return classBytes;
  }

  private Map<String, byte[]> compileSource(String className, String source, String ruleJson) {
    var diagnostics = new DiagnosticCollector<JavaFileObject>();

    try (StandardJavaFileManager stdFileManager = javac.getStandardFileManager(
        diagnostics, null, StandardCharsets.UTF_8);
         InMemoryClassFileManager fileManager = new InMemoryClassFileManager(stdFileManager)) {

      final var sourceFile = new InMemoryJavaFileObject(className, source);
      final String release = String.valueOf(Runtime.version().feature());
      final List<String> options = Arrays.asList("-source", release, "-target", release);

      final JavaCompiler.CompilationTask task = javac.getTask(
          new StringWriter(),
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

  private CompiledRule interpreterFallback(JsonLogicNode ast) {
    return data -> fallbackEvaluator.evaluate(ast, data);
  }

  public static String classNameFor(String ruleJson) {
    long hash = 0xcbf29ce484222325L;
    for (int i = 0; i < ruleJson.length(); i++) {
      hash ^= ruleJson.charAt(i);
      hash *= 0x100000001b3L;
    }
    return "Rule_" + Long.toUnsignedString(hash, 16);
  }
}
