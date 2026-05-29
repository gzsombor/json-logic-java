package io.github.jamsesso.jsonlogic;

import io.github.jamsesso.jsonlogic.compiler.JavacJsonLogicCompiler;
import io.github.jamsesso.jsonlogic.compiler.classfile.ClassFileJsonLogicCompiler;
import java.util.stream.Stream;

public final class JsonLogicTestEngines {
  private JsonLogicTestEngines() {}

  public static Stream<Object[]> engines() {
    return Stream.of(
        new Object[]{"interpreter", interpreter()},
        new Object[]{"javac", javac()},
        new Object[]{"jep484", jep484()});
  }

  public static JsonLogic interpreter() {
    return new JsonLogic(false);
  }

  public static JsonLogic compiled() {
    return javac();
  }

  public static JsonLogic javac() {
    return new JsonLogic(evaluator -> new JavacJsonLogicCompiler(evaluator, true));
  }

  public static JsonLogic jep484() {
    return new JsonLogic(evaluator -> new ClassFileJsonLogicCompiler(evaluator, true, false));
  }
}
