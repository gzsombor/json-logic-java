package io.github.jamsesso.jsonlogic.compiler;

import com.google.gson.*;
import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import io.github.jamsesso.jsonlogic.utils.JsonValueExtractor;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Verifies that every fixture in {@code fixtures.json} produces the same result when
 * evaluated via the JIT compiler as when evaluated via the tree-walking interpreter.
 *
 * <p>This gives ~400 data-driven cases for free without writing individual tests.
 */
public class CompiledRuleEquivalenceTest {

  private static List<Fixture> FIXTURES;

  @BeforeClass
  public static void loadFixtures() {
    FIXTURES = readFixtures("fixtures.json");
  }

  @Test
  public void compiledResultsMatchInterpreter() {
    JsonLogic interpreter = new JsonLogic(false);
    JsonLogic compiled    = new JsonLogic();

    List<String> failures = new ArrayList<>();

    for (Fixture f : FIXTURES) {
      Object expected;
      try {
        expected = interpreter.apply(f.json, f.data);
      } catch (JsonLogicException e) {
        // Interpreter threw — compiler must throw too (we just skip value comparison)
        try {
          compiled.apply(f.json, f.data);
          failures.add("Interpreter threw but compiled succeeded.\n  Rule: " + f.json
              + "\n  Data: " + f.data + "\n  Interpreter error: " + e.getMessage());
        } catch (JsonLogicException ignored) {
          // Both threw — acceptable
        }
        continue;
      }

      Object actual;
      try {
        actual = compiled.apply(f.json, f.data);
      } catch (JsonLogicException e) {
        failures.add("Compiled threw but interpreter succeeded.\n  Rule: " + f.json
            + "\n  Data: " + f.data + "\n  Expected: " + expected
            + "\n  Compiled error: " + e.getMessage());
        continue;
      }

      if (!Objects.equals(expected, actual)) {
        failures.add("Result mismatch.\n  Rule: " + f.json
            + "\n  Data: " + f.data
            + "\n  Expected: " + expected
            + "\n  Got:      " + actual);
      }
    }

    if (!failures.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append(failures.size()).append(" fixture(s) failed:\n\n");
      for (String failure : failures) {
        sb.append(failure).append("\n\n");
      }
      Assert.fail(sb.toString());
    }
  }

  // -------------------------------------------------------------------------

  private static List<Fixture> readFixtures(String fileName) {
    InputStream is = CompiledRuleEquivalenceTest.class.getClassLoader().getResourceAsStream(fileName);
    JsonArray json = new JsonParser().parse(new InputStreamReader(is)).getAsJsonArray();
    List<Fixture> list = new ArrayList<>();
    for (JsonElement element : json) {
      if (!element.isJsonArray()) continue;
      JsonArray arr = element.getAsJsonArray();
      list.add(new Fixture(arr.get(0).toString(),
          JsonValueExtractor.extract(arr.get(1))));
    }
    return list;
  }

  private static final class Fixture {
    final String json;
    final Object data;

    Fixture(String json, Object data) {
      this.json = json;
      this.data = data;
    }
  }
}
