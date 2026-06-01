package io.github.jamsesso.jsonlogic;

import java.util.List;
import java.util.stream.Stream;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.github.jamsesso.jsonlogic.utils.JsonValueExtractor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import static io.github.jamsesso.jsonlogic.FixtureTests.readFixtures;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ErrorFixtureTests {
  private static final List<ErrorFixture> FIXTURES = readFixtures("error-fixtures.json", ErrorFixture::fromArray);

  static Stream<Arguments> fixtures() {
    return JsonLogicTestEngines.engines()
        .flatMap(engine -> FIXTURES.stream()
            .map(fixture -> Arguments.of(engine[0], engine[1], fixture)));
  }

  @ParameterizedTest(name = "{0} {2}")
  @MethodSource("fixtures")
  public void testAllFixtures(String label, JsonLogic jsonLogic, ErrorFixture fixture) {
    try {
      jsonLogic.apply(fixture.getJson(), fixture.getData());
      fail("Expected an exception at " + fixture.getExpectedJsonPath());
    } catch (JsonLogicException e) {
      assertEquals(fixture.getExpectedError(), e.getMessage());
      assertEquals(fixture.getExpectedJsonPath(), e.getJsonPath());
    }
  }

  private static class ErrorFixture {
    private final String json;
    private final Object data;
    private final String expectedPath;
    private final String expectedError;

    private ErrorFixture(String json, JsonElement data, String expectedPath, String expectedError) {
      this.json = json;
      this.data = JsonValueExtractor.extract(data);
      this.expectedPath = expectedPath;
      this.expectedError = expectedError;
    }

    public static ErrorFixture fromArray(JsonArray array) {
      return new ErrorFixture(array.get(0).toString(), array.get(1), array.get(2).getAsString(), array.get(3).getAsString());
    }

    String getJson() {
      return json;
    }

    Object getData() {
      return data;
    }

    String getExpectedJsonPath() {
      return expectedPath;
    }

    String getExpectedError() {
      return expectedError;
    }

    @Override
    public String toString() {
      return json;
    }
  }
}
