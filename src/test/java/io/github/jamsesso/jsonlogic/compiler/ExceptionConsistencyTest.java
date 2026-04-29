package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Parametrized test that verifies interpreter and compiled paths throw the same
 * {@link JsonLogicException} (same message and JSON path) for rules listed under
 * {@code src/test/resources/scenarios/error/}.
 *
 * <p>Each scenario consists of three sibling files:
 * <ul>
 *   <li>{@code <name>.json} – the JSON logic rule</li>
 *   <li>{@code <name>.txt}  – expected exception descriptor with two lines:
 *       {@code message: <text>} and {@code path: <jsonPath>}</li>
 *   <li>{@code <name>.java} – golden-file for the generated source (used by
 *       {@link RuleSourceGeneratorScenarioTest})</li>
 * </ul>
 *
 * <p>Only scenarios that have a {@code .txt} file are included in this test;
 * scenarios without one are treated as non-throwing (tested by the golden-file
 * suite instead).
 */
@RunWith(Parameterized.class)
public class ExceptionConsistencyTest {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> scenarios() throws IOException {
    URL dir = ExceptionConsistencyTest.class
        .getClassLoader()
        .getResource("scenarios/error");
    Assert.assertNotNull("scenarios/error/ resource directory not found", dir);

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

    return params;
  }

  private final String scenarioName;
  private final Path jsonPath;
  private final Path txtPath;

  public ExceptionConsistencyTest(String scenarioName, Path jsonPath, Path txtPath) {
    this.scenarioName = scenarioName;
    this.jsonPath = jsonPath;
    this.txtPath = txtPath;
  }

  // ---- helpers ---------------------------------------------------------------

  private String readFile(Path p) throws IOException {
    return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
  }

  /** Parses the two-line descriptor into a String[]{message, path}. */
  private String[] parseDescriptor(String txt) {
    String message = null;
    String path = null;
    for (String line : txt.split("\\r?\\n")) {
      if (line.startsWith("message: ")) {
        message = line.substring("message: ".length());
      } else if (line.startsWith("path: ")) {
        path = line.substring("path: ".length());
      }
    }
    Assert.assertNotNull("Missing 'message:' in " + scenarioName + ".txt", message);
    Assert.assertNotNull("Missing 'path:' in " + scenarioName + ".txt", path);
    return new String[]{message, path};
  }

  // ---- test body -------------------------------------------------------------

  @Test
  public void interpreterAndCompiledThrowSameException() throws Exception {
    Assert.assertTrue("Missing .json for scenario: " + scenarioName, Files.exists(jsonPath));
    Assert.assertTrue("Missing .txt for scenario: " + scenarioName, Files.exists(txtPath));

    String json = readFile(jsonPath).trim();
    String[] expected = parseDescriptor(readFile(txtPath).trim());
    String expectedMessage = expected[0];
    String expectedPath    = expected[1];

    JsonLogic interpreter = new JsonLogic(false);
    JsonLogic compiled    = new JsonLogic(true);

    JsonLogicException interpEx = applyExpectingException(interpreter, json, "interpreter");
    JsonLogicException compiledEx = applyExpectingException(compiled, json, "compiled");

    Assert.assertEquals(
        "[interpreter] wrong exception message for " + scenarioName,
        expectedMessage, interpEx.getMessage());
    Assert.assertEquals(
        "[interpreter] wrong JSON path for " + scenarioName,
        expectedPath, interpEx.getJsonPath());

    Assert.assertEquals(
        "[compiled] wrong exception message for " + scenarioName,
        expectedMessage, compiledEx.getMessage());
    Assert.assertEquals(
        "[compiled] wrong JSON path for " + scenarioName,
        expectedPath, compiledEx.getJsonPath());
  }

  private JsonLogicException applyExpectingException(JsonLogic engine, String json, String label) {
    try {
      engine.apply(json, null);
      Assert.fail("[" + label + "] Expected JsonLogicException for scenario: " + scenarioName
          + " but no exception was thrown");
      return null; // unreachable
    } catch (JsonLogicException e) {
      return e;
    } catch (Exception e) {
      Assert.fail("[" + label + "] Expected JsonLogicException but got "
          + e.getClass().getName() + ": " + e.getMessage());
      return null; // unreachable
    }
  }
}
