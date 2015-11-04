package org.renjin.gcc.codegen.ret;

import com.google.common.base.Preconditions;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.expr.ValueGenerator;
import org.renjin.gcc.gimple.type.GimplePrimitiveType;
import org.renjin.gcc.gimple.type.GimpleType;

import static org.objectweb.asm.Opcodes.IRETURN;


public class PrimitiveReturnGenerator implements ReturnGenerator {
  
  private GimplePrimitiveType gimpleType;
  private Type type;
  
  public PrimitiveReturnGenerator(GimpleType gimpleType) {
    Preconditions.checkNotNull(gimpleType);
    this.gimpleType = (GimplePrimitiveType) gimpleType;
    this.type = ((GimplePrimitiveType) gimpleType).jvmType();
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public GimpleType getGimpleType() {
    return gimpleType;
  }

  @Override
  public void emitReturn(MethodVisitor mv, ExprGenerator valueGenerator) {
    ValueGenerator primitiveGenerator = (ValueGenerator) valueGenerator;
    primitiveGenerator.emitPrimitiveValue(mv);
    mv.visitInsn(type.getOpcode(IRETURN));
  }

  @Override
  public void emitVoidReturn(MethodVisitor mv) {
    throw new UnsupportedOperationException();
  }
}
