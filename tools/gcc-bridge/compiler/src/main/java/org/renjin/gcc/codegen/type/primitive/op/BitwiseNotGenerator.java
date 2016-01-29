package org.renjin.gcc.codegen.type.primitive.op;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.expr.AbstractExprGenerator;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.type.primitive.AddressOfPrimitiveValue;
import org.renjin.gcc.gimple.type.GimpleType;

public class BitwiseNotGenerator extends AbstractExprGenerator implements ExprGenerator {

  private final ExprGenerator valueGenerator;

  public BitwiseNotGenerator(ExprGenerator valueGenerator) {
    this.valueGenerator = valueGenerator;
  }
  

  @Override
  public void emitPrimitiveValue(MethodGenerator mv) {
    
    if(!valueGenerator.getJvmPrimitiveType().equals(Type.INT_TYPE)) {
      throw new UnsupportedOperationException("Bitwise not only supported for int32 operands.");
    }
    
    // Unary bitwise complement operator is implemented
    // as an XOR operation with -1 (all bits set)
    valueGenerator.emitPrimitiveValue(mv);
    mv.visitInsn(Opcodes.ICONST_M1);
    mv.visitInsn(Opcodes.IXOR);
  }

  @Override
  public GimpleType getGimpleType() {
    return valueGenerator.getGimpleType();
  }


  @Override
  public ExprGenerator addressOf() {
    return new AddressOfPrimitiveValue(this);
  }
}
