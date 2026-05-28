package io.github.jamsesso.jsonlogic.compiler.classfile;

import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilerImplementation;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilerProvider;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;

public final class ClassFileJsonLogicCompilerProvider implements JsonLogicCompilerProvider {
  @Override
  public int priority() {
    return 200;
  }

  @Override
  public boolean isAvailable() {
    try {
      Class.forName("java.lang.classfile.ClassFile");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public JsonLogicCompilerImplementation create(JsonLogicEvaluator fallbackEvaluator, boolean strictMode) {
    return new ClassFileJsonLogicCompiler(fallbackEvaluator, strictMode);
  }
}
