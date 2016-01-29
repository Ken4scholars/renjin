package org.renjin.gcc.codegen.type.fun;

import org.objectweb.asm.Handle;
import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.expr.AbstractExprGenerator;
import org.renjin.gcc.gimple.type.GimpleType;

/**
 * Emits the bytecode necessary to push a method handle onto the stack
 */
public class FunctionRefGenerator extends AbstractExprGenerator {

  private Handle handle;

  public FunctionRefGenerator(Handle handle) {
    this.handle = handle;
  }

  @Override
  public GimpleType getGimpleType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void emitPushMethodHandle(MethodGenerator mv) {
    mv.visitLdcInsn(handle);
  }
}
