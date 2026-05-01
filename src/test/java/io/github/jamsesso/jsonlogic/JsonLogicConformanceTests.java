package io.github.jamsesso.jsonlogic;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.github.jamsesso.jsonlogic.utils.JsonValueExtractor;

@RunWith(Parameterized.class)
public class JsonLogicConformanceTests {
  private static final List<ConformanceFixture> FIXTURES = readConformanceFixtures("jsonlogic-tests.json");

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> engines() {
    return Arrays.asList(new Object[][]{
        {"interpreter", new JsonLogic(false)},
        {"compiled",    new JsonLogic(true)},
    });
  }

  private final String label;
  private final JsonLogic jsonLogic;

  public JsonLogicConformanceTests(String label, JsonLogic jsonLogic) {
    this.label = label;
    this.jsonLogic = jsonLogic;
  }

  public static List<ConformanceFixture> readConformanceFixtures(String fileName) {
    InputStream inputStream = JsonLogicConformanceTests.class.getClassLoader().getResourceAsStream(fileName);
    JsonParser parser = new JsonParser();
    JsonArray json = parser.parse(new InputStreamReader(inputStream)).getAsJsonArray();

    List<ConformanceFixture> fixtures = new ArrayList<>();
    for (JsonElement element : json) {
      // Skip comments (strings starting with "#")
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
        continue;
      }

      if (!element.isJsonArray()) {
        continue;
      }

      JsonArray array = element.getAsJsonArray();
      if (array.size() >= 3) {
        fixtures.add(ConformanceFixture.fromArray(array));
      }
    }
    return fixtures;
  }

  @Test
  public void testAllConformanceFixtures() {
    List<ConformanceTestResult> failures = new ArrayList<>();

    for (ConformanceFixture fixture : FIXTURES) {
      try {
        Object result = jsonLogic.apply(fixture.getJson(), fixture.getData());

        if (!Objects.equals(result, fixture.getExpectedValue())) {
          failures.add(new ConformanceTestResult(fixture, result));
        }
      }
      catch (JsonLogicException e) {
        failures.add(new ConformanceTestResult(fixture, e));
      }
    }

    for (ConformanceTestResult testResult : failures) {
      Object actual = testResult.getResult();
      ConformanceFixture fixture = testResult.getFixture();

      System.out.println(String.format("FAIL [%s]: %s\n\t%s\n\tExpected: %s Got: %s\n",
        label, fixture.getJson(), fixture.getData(),
        fixture.getExpectedValue(), actual instanceof Exception ? ((Exception) actual).getMessage() : actual));
    }

    Assert.assertEquals(String.format("[%s] %d/%d conformance test failures!", label, failures.size(), FIXTURES.size()), 0, failures.size());
  }

  private static class ConformanceFixture {
    public static ConformanceFixture fromArray(JsonArray array) {
      return new ConformanceFixture(array.get(0).toString(), array.get(1), array.get(2));
    }

    private final String json;
    private final Object data;
    private final Object expectedValue;

    private ConformanceFixture(String json, JsonElement data, JsonElement expectedValue) {
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
  }

  private static class ConformanceTestResult {
    private final ConformanceFixture fixture;
    private final Object result;

    private ConformanceTestResult(ConformanceFixture fixture, Object result) {
      this.fixture = fixture;
      this.result = result;
    }

    ConformanceFixture getFixture() {
      return fixture;
    }

    Object getResult() {
      return result;
    }
  }
}
