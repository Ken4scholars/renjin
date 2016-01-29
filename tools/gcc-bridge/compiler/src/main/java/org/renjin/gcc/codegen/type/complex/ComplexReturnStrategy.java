package org.renjin.gcc.codegen.type.complex;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.call.CallGenerator;
import org.renjin.gcc.codegen.expr.AbstractExprGenerator;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.type.ReturnStrategy;
import org.renjin.gcc.gimple.type.GimpleComplexType;
import org.renjin.gcc.gimple.type.GimpleType;

import java.util.List;

/**
 * Strategy for returning a complex value as a {@code double[2]} or {@code float[2]}
 */
public class ComplexReturnStrategy implements ReturnStrategy {
  
  private GimpleComplexType type;

  public ComplexReturnStrategy(GimpleComplexType type) {
    this.type = type;
  }

  @Override
  public Type getType() {
    return type.getJvmPartArrayType();
  }

  @Override
  public void emitReturnValue(MethodGenerator mv, ExprGenerator valueGenerator) {
    valueGenerator.emitPushComplexAsArray(mv);
    mv.visitInsn(Opcodes.ARETURN);
  }

  @Override
  public void emitReturnDefault(MethodGenerator mv) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ExprGenerator callExpression(CallGenerator callGenerator, List<ExprGenerator> arguments) {
    return new CallExpr(callGenerator, arguments);
  }

  private class CallExpr extends AbstractExprGenerator {
    private CallGenerator callGenerator;
    private List<ExprGenerator> argumentGenerators;

    public CallExpr(CallGenerator callGenerator, List<ExprGenerator> argumentGenerators) {
      this.callGenerator = callGenerator;
      this.argumentGenerators = argumentGenerators;
    }

    @Override
    public GimpleType getGimpleType() {
      return type;
    }

    @Override
    public void emitPushComplexAsArray(MethodGenerator mv) {
      callGenerator.emitCall(mv, argumentGenerators);
    }
  }
}
