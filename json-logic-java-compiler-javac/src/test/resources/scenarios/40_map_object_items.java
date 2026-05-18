package io.github.jamsesso.jsonlogic.compiler.gen;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import static io.github.jamsesso.jsonlogic.compiler.RuleHelpers.*;
import java.util.*;

public final class TestRule implements CompiledRule {

  private final JsonLogicEvaluator fallback;
  private final JsonLogicNode[] fallbackNodes;
  private final String ruleJson;

  public TestRule(JsonLogicEvaluator fallback, JsonLogicNode[] fallbackNodes, String ruleJson) {
    this.fallback = fallback;
    this.fallbackNodes = fallbackNodes;
    this.ruleJson = ruleJson;
  }

  @Override
  public String toString() {
    return "CompiledRule(" + ruleJson + ")";
  }

  @Override
  public Object apply(Object data) throws JsonLogicEvaluationException {
    final Object var_users_6 = resolveVarChecked(data, "users", null);
    Object mapArray_1 = var_users_6;
    final List<Object> mapResult_2 = new ArrayList<>();
    if (ArrayLike.isEligible(mapArray_1)) {
      for (Object mapItem_3 : ArrayLike.iterable(mapArray_1)) {
        mapResult_2.add(collectionBody$0(mapItem_3));
      }
    }
    Object result_0 = mapResult_2;
    return result_0;
  }

  private Object collectionBody$0(Object item) throws JsonLogicEvaluationException {
    final Object var_name_5 = resolveVarChecked(item, "name", null);
    Object collectionBodyResult_4 = (catStr(var_name_5) + " hello");
    return collectionBodyResult_4;
  }
}
