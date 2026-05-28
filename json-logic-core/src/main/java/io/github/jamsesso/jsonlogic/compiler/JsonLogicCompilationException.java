package io.github.jamsesso.jsonlogic.compiler;

/**
 * Thrown when {@link JsonLogicCompiler} fails to compile a rule and strict mode is enabled.
 * This is a runtime exception since it represents a fatal configuration error in strict mode.
 */
public class JsonLogicCompilationException extends RuntimeException {
  public JsonLogicCompilationException(String message) {
    super(message);
  }

  public JsonLogicCompilationException(String message, Throwable cause) {
    super(message, cause);
  }
}
