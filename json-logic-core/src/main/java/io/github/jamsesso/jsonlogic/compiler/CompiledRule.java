package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;

/**
 * A rule that has been compiled to Java bytecode via {@link JsonLogicCompiler}.
 * Calling {@link #apply(Object)} is equivalent to running the same rule through
 * the tree-walking interpreter, but without HashMap dispatch or recursive boxing
 * overhead for the natively-compiled operators.
 */
@FunctionalInterface
public interface CompiledRule {
  Object apply(Object data) throws JsonLogicEvaluationException;
}
