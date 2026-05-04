package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parametrized test that verifies interpreter and compiled paths throw the same
 * {@link JsonLogicException} (same message and JSON path) for rules listed under
 * {@code src/test/resources/scenarios/error/}.
 */
public class ExceptionConsistencyTest {

  static Stream<Object[]> scenarios() throws IOException {
    URL dir = ExceptionConsistencyTest.class
        .getClassLoader()
        .getResource("scenarios/error");
    assertNotNull(dir, "scenarios/error/ resource directory not found");

    Path scenariosDir = Paths.get(dir.getPath());
    List<Object[]> params = new ArrayList<>();

    Files.list(scenariosDir)
        .filter(p -> p.toString().endsWith(".txt"))
        .sorted()
        .forEach(txtPath -> {
          String base = txtPath.getFileName().toString().replace(".txt", "");
          Path jsonPath = txtPath.resolveSibling(base + ".json");
          params.add(new Object[]{base, jsonPath, txtPath});
        });

    return params.stream();
  }

  private String readFile(Path p) throws IOException {
    return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
  }

  private String[] parseDescriptor(String txt, String scenarioName) {
    String message = null;
    String path = null;
    for (String line : txt.split("\\r?\\n")) {
      if (line.startsWith("message: ")) {
        message = line.substring("message: ".length());
      } else if (line.startsWith("path: ")) {
        path = line.substring("path: ".length());
      }
    }
    assertNotNull(message, "Missing 'message:' in " + scenarioName + ".txt");
    assertNotNull(path, "Missing 'path:' in " + scenarioName + ".txt");
    return new String[]{message, path};
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("scenarios")
  public void interpreterAndCompiledThrowSameException(String scenarioName, Path jsonPath, Path txtPath) throws Exception {
    assertTrue(Files.exists(jsonPath), "Missing .json for scenario: " + scenarioName);
    assertTrue(Files.exists(txtPath), "Missing .txt for scenario: " + scenarioName);

    String json = readFile(jsonPath).trim();
    String[] expected = parseDescriptor(readFile(txtPath).trim(), scenarioName);
    String expectedMessage = expected[0];
    String expectedPath    = expected[1];

    JsonLogic interpreter = new JsonLogic(false);
    JsonLogic compiled    = new JsonLogic(true);

    JsonLogicException interpEx = applyExpectingException(interpreter, json, "interpreter", scenarioName);
    JsonLogicException compiledEx = applyExpectingException(compiled, json, "compiled", scenarioName);

    assertEquals(expectedMessage, interpEx.getMessage(),
        "[interpreter] wrong exception message for " + scenarioName);
    assertEquals(expectedPath, interpEx.getJsonPath(),
        "[interpreter] wrong JSON path for " + scenarioName);

    assertEquals(expectedMessage, compiledEx.getMessage(),
        "[compiled] wrong exception message for " + scenarioName);
    assertEquals(expectedPath, compiledEx.getJsonPath(),
        "[compiled] wrong JSON path for " + scenarioName);
  }

  private JsonLogicException applyExpectingException(JsonLogic engine, String json, String label, String scenarioName) {
    try {
      engine.apply(json, null);
      fail("[" + label + "] Expected JsonLogicException for scenario: " + scenarioName
          + " but no exception was thrown");
      return null;
    } catch (JsonLogicException e) {
      return e;
    } catch (Exception e) {
      fail("[" + label + "] Expected JsonLogicException but got "
          + e.getClass().getName() + ": " + e.getMessage());
      return null;
    }
  }
}
