package io.github.jamsesso.jsonlogic.runner;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilationException;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompiler;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilerProvider;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.utils.JsonValueExtractor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class JsonLogicRunner {

  private JsonLogicRunner() {}

  public static void main(String[] args) throws Exception {
    if (args.length == 1 && "--help".equals(args[0])) {
      printHelp();
      return;
    }
    if (args.length < 1 || args.length > 2) {
      System.err.println("Usage: java -jar json-logic-runner.jar <rule-json|@rule-file> [data-json|@data-file]");
      System.err.println("       java -jar json-logic-runner.jar --help");
      System.exit(2);
    }

    String ruleJson = resolveArg(args[0]);
    String dataJson = args.length >= 2 ? resolveArg(args[1]) : "null";

    System.out.println("JVM     : " + System.getProperty("java.version")
        + " (" + System.getProperty("java.vendor") + ")");
    System.out.println("Rule    : " + ruleJson);
    System.out.println("Data    : " + dataJson);
    System.out.println();

    List<ProviderEntry> entries = probeProviders();
    System.out.println("Compiler providers (" + entries.size() + " found on classpath):");
    for (ProviderEntry e : entries) {
      if (e.provider != null) {
        System.out.printf("  [priority=%3d, available=%-5s] %s%n",
            e.provider.priority(), e.provider.isAvailable(), e.provider.getClass().getSimpleName());
      } else {
        System.out.printf("  [load failed] %s : %s%n", e.className, e.error);
      }
    }

    ProviderEntry selected = entries.stream()
        .filter(e -> e.provider != null && e.provider.isAvailable())
        .max(Comparator.comparingInt(e -> e.provider.priority()))
        .orElse(null);

    System.out.println();
    if (selected == null) {
      System.out.println("Selected: (none) — using interpreter fallback");
    } else {
      System.out.println("Selected: " + selected.provider.getClass().getSimpleName()
          + " (priority=" + selected.provider.priority() + ")");
    }

    JsonLogicEvaluator evaluator = new JsonLogicEvaluator();
    JsonLogicNode ruleAst = JsonLogicParser.parse(ruleJson);
    Object data = JsonValueExtractor.extract(JsonParser.parseString(dataJson));

    Object result;
    if (selected != null) {
      JsonLogicCompiler compiler = new JsonLogicCompiler(
          selected.provider.create(evaluator, false, true));
      try {
        CompiledRule compiledRule = compiler.compile(ruleJson, ruleAst);
        System.out.println("Compiled : " + compiledRule.getClass().getName());
        result = compiledRule.apply(data);
      } catch (JsonLogicCompilationException e) {
        System.out.println("Compile failed: " + e.getMessage() + " — falling back to interpreter");
        result = evaluator.evaluate(ruleAst, data);
      }
    } else {
      System.out.println("Compiled : n/a");
      result = evaluator.evaluate(ruleAst, data);
    }

    System.out.println("Result  : " + new Gson().toJson(result));
  }

  private static void printHelp() {
    System.out.println("JVM     : " + System.getProperty("java.version")
        + " (" + System.getProperty("java.vendor") + ")");
    System.out.println();

    List<ProviderEntry> entries = probeProviders();
    ProviderEntry selected = entries.stream()
        .filter(e -> e.provider != null && e.provider.isAvailable())
        .max(Comparator.comparingInt(e -> e.provider.priority()))
        .orElse(null);

    if (selected == null) {
      System.out.println("Compiler: not available — rules will be interpreted");
    } else {
      System.out.println("Compiler: " + selected.provider.getClass().getSimpleName()
          + " (priority=" + selected.provider.priority() + ")");
    }

    System.out.println();
    System.out.println("All providers on classpath:");
    for (ProviderEntry e : entries) {
      if (e.provider != null) {
        System.out.printf("  [priority=%3d, available=%-5s] %s%n",
            e.provider.priority(), e.provider.isAvailable(), e.provider.getClass().getSimpleName());
      } else {
        System.out.printf("  [unavailable] %s%n", e.className);
        System.out.printf("                %s%n", e.error);
      }
    }

    System.out.println();
    System.out.println("Usage: java -jar json-logic-runner.jar <rule-json|@rule-file> [data-json|@data-file]");
  }

  private static String resolveArg(String arg) throws Exception {
    if (arg.startsWith("@")) {
      return Files.readString(Path.of(arg.substring(1)), StandardCharsets.UTF_8).trim();
    }
    return arg;
  }

  private static List<ProviderEntry> probeProviders() {
    String[] classNames = {
        "io.github.jamsesso.jsonlogic.compiler.classfile.ClassFileJsonLogicCompilerProvider",
        "io.github.jamsesso.jsonlogic.compiler.JavacJsonLogicCompilerProvider"
    };
    List<ProviderEntry> result = new ArrayList<>();
    for (String className : classNames) {
      try {
        Class<?> cls = Class.forName(className);
        JsonLogicCompilerProvider provider =
            (JsonLogicCompilerProvider) cls.getDeclaredConstructor().newInstance();
        result.add(new ProviderEntry(className, provider, null));
      } catch (Throwable t) {
        result.add(new ProviderEntry(className, null, t.toString()));
      }
    }
    return result;
  }

  private static final class ProviderEntry {
    final String className;
    final JsonLogicCompilerProvider provider;
    final String error;

    ProviderEntry(String className, JsonLogicCompilerProvider provider, String error) {
      this.className = className;
      this.provider = provider;
      this.error = error;
    }
  }
}
