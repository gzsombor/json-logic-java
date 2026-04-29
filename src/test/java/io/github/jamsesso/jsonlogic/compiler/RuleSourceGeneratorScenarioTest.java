package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
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
 * Parametrized golden-file test: for every *.json scenario under
 * src/test/resources/scenarios/ the generator output must exactly match the
 * sibling *.java file.
 */
@RunWith(Parameterized.class)
public class RuleSourceGeneratorScenarioTest {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> scenarios() throws IOException {
    URL dir = RuleSourceGeneratorScenarioTest.class
        .getClassLoader()
        .getResource("scenarios");
    Assert.assertNotNull("scenarios/ resource directory not found", dir);

    Path scenariosDir = Paths.get(dir.getPath());
    List<Object[]> params = new ArrayList<>();

    Files.list(scenariosDir)
        .filter(p -> p.toString().endsWith(".json"))
        .sorted()
        .forEach(jsonPath -> {
          String base = jsonPath.getFileName().toString().replace(".json", "");
          Path javaPath = jsonPath.resolveSibling(base + ".java");
          params.add(new Object[]{base, jsonPath, javaPath});
        });

    return params;
  }

  private final String scenarioName;
  private final Path jsonPath;
  private final Path javaPath;

  public RuleSourceGeneratorScenarioTest(String scenarioName, Path jsonPath, Path javaPath) {
    this.scenarioName = scenarioName;
    this.jsonPath = jsonPath;
    this.javaPath = javaPath;
  }

  @Test
  public void generatedSourceMatchesFixture() throws Exception {
    Assert.assertTrue(
        "Missing .java fixture for scenario: " + scenarioName,
        Files.exists(javaPath));

    String json = new String(Files.readAllBytes(jsonPath), StandardCharsets.UTF_8).trim();
    String expectedSource = new String(Files.readAllBytes(javaPath), StandardCharsets.UTF_8);

    JsonLogicNode node = JsonLogicParser.parse(json);
    RuleSourceGenerator generator = new RuleSourceGenerator();
    String actualSource = generator.generate(node, "TestRule");

    Assert.assertEquals(
        "Generated source mismatch for scenario: " + scenarioName,
        expectedSource.stripTrailing(),
        actualSource.stripTrailing());
  }
}
