package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
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
 * Parametrized golden-file test: for every *.json scenario under
 * src/test/resources/scenarios/ (and its error/ subdirectory) the generator
 * output must exactly match the sibling *.java file.
 */
public class RuleSourceGeneratorScenarioTest {

  static Stream<Object[]> scenarios() throws IOException {
    List<Object[]> params = new ArrayList<>();
    addScenariosFrom("scenarios", params);
    addScenariosFrom("scenarios/error", params);
    return params.stream();
  }

  private static void addScenariosFrom(String resource, List<Object[]> params) throws IOException {
    URL dir = RuleSourceGeneratorScenarioTest.class.getClassLoader().getResource(resource);
    if (dir == null) return;
    Path scenariosDir = Paths.get(dir.getPath());
    Files.list(scenariosDir)
        .filter(p -> p.toString().endsWith(".json"))
        .sorted()
        .forEach(jsonPath -> {
          String base = jsonPath.getFileName().toString().replace(".json", "");
          Path javaPath = jsonPath.resolveSibling(base + ".java");
          params.add(new Object[]{base, jsonPath, javaPath});
        });
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("scenarios")
  public void generatedSourceMatchesFixture(String scenarioName, Path jsonPath, Path javaPath) throws Exception {
    String json = new String(Files.readAllBytes(jsonPath), StandardCharsets.UTF_8).trim();
    JsonLogicNode node = JsonLogicParser.parse(json);
    RuleSourceGenerator generator = new RuleSourceGenerator();
    String actualSource = generator.generate(node, "TestRule");

    if (!Files.exists(javaPath)) {
      Path sourcePath = toSourcePath(javaPath);
      Files.writeString(sourcePath, actualSource, StandardCharsets.UTF_8);
      fail("Created missing fixture (re-run to verify): " + sourcePath);
    }

    String expectedSource = new String(Files.readAllBytes(javaPath), StandardCharsets.UTF_8);
    assertEquals(
        expectedSource.stripTrailing(),
        actualSource.stripTrailing(),
        "Generated source mismatch for scenario: " + scenarioName);
  }

  private static Path toSourcePath(Path buildPath) {
    return Paths.get(buildPath.toString()
        .replace("/build/resources/test/", "/src/test/resources/"));
  }
}
