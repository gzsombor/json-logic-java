package io.github.jamsesso.jsonlogic.evaluator;

import io.github.jamsesso.jsonlogic.JsonLogicException;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonLogicEvaluatorTest {

  @Test
  public void shouldEvaluateBuiltInExpressionsByDefault() throws JsonLogicException {
    JsonLogicEvaluator evaluator = new JsonLogicEvaluator();

    Object result = evaluator.evaluate(JsonLogicParser.parse("{\"+\":[1,2]}"), null);

    assertEquals(3.0, result);
  }
}
