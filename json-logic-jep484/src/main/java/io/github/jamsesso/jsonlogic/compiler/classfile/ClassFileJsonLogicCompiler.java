package io.github.jamsesso.jsonlogic.compiler.classfile;

import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.INIT_NAME;
import static java.lang.constant.ConstantDescs.MTD_void;
import static java.lang.constant.ConstantDescs.ofCallsiteBootstrap;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilationException;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilerImplementation;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import java.lang.classfile.ClassFile;
import java.lang.classfile.attribute.ExceptionsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClassFileJsonLogicCompiler implements JsonLogicCompilerImplementation {
  private static final Logger LOG = Logger.getLogger(ClassFileJsonLogicCompiler.class.getName());
  private static final String GENERATED_PACKAGE = "io.github.jamsesso.jsonlogic.compiler.classfile.gen";
  private static final ClassDesc CD_CALL_SITE = ClassDesc.of("java.lang.invoke.CallSite");
  private static final ClassDesc CD_COMPILED_RULE = ClassDesc.of(CompiledRule.class.getName());
  private static final ClassDesc CD_EVALUATION_EXCEPTION = ClassDesc.of(JsonLogicEvaluationException.class.getName());
  private static final ClassDesc CD_JSON_LOGIC_EVALUATOR = ClassDesc.of(JsonLogicEvaluator.class.getName());
  private static final ClassDesc CD_JSON_LOGIC_NODE = ClassDesc.of(JsonLogicNode.class.getName());
  private static final ClassDesc CD_LOOKUP = ClassDesc.of("java.lang.invoke.MethodHandles$Lookup");
  private static final ClassDesc CD_METHOD_TYPE = ClassDesc.of("java.lang.invoke.MethodType");
  private static final ClassDesc CD_STRING_CONCAT_FACTORY = ClassDesc.of("java.lang.invoke.StringConcatFactory");
  private static final MethodTypeDesc MTD_STRING_CONCAT_BOOTSTRAP = MethodTypeDesc.of(
      CD_CALL_SITE, CD_LOOKUP, CD_String, CD_METHOD_TYPE, CD_String);

  private final JsonLogicEvaluator fallbackEvaluator;
  private final boolean strictMode;

  public ClassFileJsonLogicCompiler(JsonLogicEvaluator fallbackEvaluator, boolean strictMode) {
    this.fallbackEvaluator = fallbackEvaluator;
    this.strictMode = strictMode;
  }

  @Override
  public boolean isStrictMode() {
    return strictMode;
  }

  @Override
  public CompiledRule compile(String ruleJson, JsonLogicNode ast) throws JsonLogicCompilationException {
    final String className = classNameFor(ruleJson);
    final String qualifiedName = GENERATED_PACKAGE + "." + className;
    final ClassDesc generatedClass = ClassDesc.of(GENERATED_PACKAGE, className);

    try {
      final byte[] classBytes = generateClass(generatedClass);
      final var loader = new ByteArrayClassLoader(
          Thread.currentThread().getContextClassLoader(), Map.of(qualifiedName, classBytes));
      final Class<?> clazz = loader.loadClass(qualifiedName);
      final Constructor<?> constructor = clazz.getConstructor(
          JsonLogicEvaluator.class, JsonLogicNode.class, String.class);
      return (CompiledRule) constructor.newInstance(fallbackEvaluator, ast, ruleJson);
    } catch (InvocationTargetException e) {
      return handleFailure(ruleJson, ast, e.getCause());
    } catch (Exception e) {
      return handleFailure(ruleJson, ast, e);
    }
  }

  private byte[] generateClass(ClassDesc generatedClass) {
    return ClassFile.of().build(generatedClass, classBuilder -> classBuilder
        .withFlags(Modifier.PUBLIC | Modifier.FINAL)
        .withInterfaceSymbols(CD_COMPILED_RULE)
        .withField("fallback", CD_JSON_LOGIC_EVALUATOR, Modifier.PRIVATE | Modifier.FINAL)
        .withField("ast", CD_JSON_LOGIC_NODE, Modifier.PRIVATE | Modifier.FINAL)
        .withField("ruleJson", CD_String, Modifier.PRIVATE | Modifier.FINAL)
        .withMethodBody(
            INIT_NAME,
            MethodTypeDesc.ofDescriptor("(Lio/github/jamsesso/jsonlogic/evaluator/JsonLogicEvaluator;"
                + "Lio/github/jamsesso/jsonlogic/ast/JsonLogicNode;Ljava/lang/String;)V"),
            Modifier.PUBLIC,
            codeBuilder -> codeBuilder
                .aload(0)
                .invokespecial(CD_Object, INIT_NAME, MTD_void)
                .aload(0)
                .aload(1)
                .putfield(generatedClass, "fallback", CD_JSON_LOGIC_EVALUATOR)
                .aload(0)
                .aload(2)
                .putfield(generatedClass, "ast", CD_JSON_LOGIC_NODE)
                .aload(0)
                .aload(3)
                .putfield(generatedClass, "ruleJson", CD_String)
                .return_())
        .withMethod(
            "apply",
            MethodTypeDesc.of(CD_Object, CD_Object),
            Modifier.PUBLIC,
            methodBuilder -> methodBuilder
                .with(ExceptionsAttribute.ofSymbols(CD_EVALUATION_EXCEPTION))
                .withCode(codeBuilder -> codeBuilder
                    .aload(0)
                    .getfield(generatedClass, "fallback", CD_JSON_LOGIC_EVALUATOR)
                    .aload(0)
                    .getfield(generatedClass, "ast", CD_JSON_LOGIC_NODE)
                    .aload(1)
                    .invokevirtual(
                        CD_JSON_LOGIC_EVALUATOR,
                        "evaluate",
                        MethodTypeDesc.of(CD_Object, CD_JSON_LOGIC_NODE, CD_Object))
                    .areturn()))
        .withMethodBody(
            "toString",
            MethodTypeDesc.of(CD_String),
            Modifier.PUBLIC,
            codeBuilder -> codeBuilder
                .aload(0)
                .getfield(generatedClass, "ruleJson", CD_String)
                .invokedynamic(DynamicCallSiteDesc.of(
                    ofCallsiteBootstrap(
                        CD_STRING_CONCAT_FACTORY,
                        "makeConcatWithConstants",
                        CD_CALL_SITE,
                        CD_String),
                    "makeConcatWithConstants",
                    MethodTypeDesc.of(CD_String, CD_String),
                    "CompiledRule(\u0001)"))
                .areturn()));
  }

  private CompiledRule handleFailure(String ruleJson, JsonLogicNode ast, Throwable failure) {
    if (strictMode) {
      throw new JsonLogicCompilationException("Class-File API compilation failed for rule: " + ruleJson, failure);
    }
    LOG.log(Level.WARNING, "Class-File API compilation failed, falling back to interpreter. Rule: " + ruleJson, failure);
    return data -> fallbackEvaluator.evaluate(ast, data);
  }

  private static String classNameFor(String ruleJson) {
    long hash = 0xcbf29ce484222325L;
    for (int i = 0; i < ruleJson.length(); i++) {
      hash ^= ruleJson.charAt(i);
      hash *= 0x100000001b3L;
    }
    return "Rule_" + Long.toUnsignedString(hash, 16);
  }
}
