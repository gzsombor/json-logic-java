package io.github.jamsesso.jsonlogic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.jamsesso.jsonlogic.utils.JsonValueExtractor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FixtureTests {
  private static final List<Fixture> FIXTURES = readFixtures("fixtures.json", Fixture::fromArray);
  public static <F> List<F> readFixtures(String fileName, Function<JsonArray, F> makeFixture) {
    InputStream inputStream = FixtureTests.class.getClassLoader().getResourceAsStream(fileName);
    JsonArray json = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonArray();

    List<F> fixtures = new ArrayList<>();
    for (JsonElement element : json) {
      if (!element.isJsonArray()) {
        continue;
      }

      JsonArray array = element.getAsJsonArray();
      fixtures.add(makeFixture.apply(array));
    }
    return fixtures;
  }

  static Stream<Arguments> fixtures() {
    return JsonLogicTestEngines.engines()
        .flatMap(engine -> FIXTURES.stream()
            .map(fixture -> Arguments.of(engine[0], engine[1], fixture)));
  }

  @ParameterizedTest(name = "{0} {2}")
  @MethodSource("fixtures")
  public void testAllFixtures(String label, JsonLogic jsonLogic, Fixture fixture) throws JsonLogicException {
    Object result = jsonLogic.apply(fixture.getJson(), fixture.getData());
    assertEquals(fixture.getExpectedValue(), result,
        String.format("FAIL [%s]: %s\n\t%s", label, fixture.getJson(), fixture.getData()));
  }

  private static class Fixture {
    public static Fixture fromArray(JsonArray array) {
      return new Fixture(array.get(0).toString(), array.get(1), array.get(2));
    }

    private final String json;
    private final Object data;
    private final Object expectedValue;

    private Fixture(String json, JsonElement data, JsonElement expectedValue) {
      this.json = json;
      this.data = JsonValueExtractor.extract(data);
      this.expectedValue = JsonValueExtractor.extract(expectedValue);
    }

    String getJson() {
      return json;
    }

    Object getData() {
      return data;
    }

    Object getExpectedValue() {
      return expectedValue;
    }

    @Override
    public String toString() {
      return json;
    }
  }
}
