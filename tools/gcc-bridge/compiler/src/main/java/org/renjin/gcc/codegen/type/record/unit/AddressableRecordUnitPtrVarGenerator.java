package org.renjin.gcc.codegen.type.record.unit;

import com.google.common.base.Optional;
import org.objectweb.asm.Opcodes;
import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.expr.AbstractExprGenerator;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.expr.NullPtrGenerator;
import org.renjin.gcc.codegen.type.VarGenerator;
import org.renjin.gcc.codegen.type.record.RecordClassTypeStrategy;
import org.renjin.gcc.codegen.var.Var;
import org.renjin.gcc.gimple.type.GimpleType;


public class AddressableRecordUnitPtrVarGenerator extends AbstractExprGenerator implements VarGenerator {

  private RecordClassTypeStrategy strategy;
  private Var varIndex;

  public AddressableRecordUnitPtrVarGenerator(RecordClassTypeStrategy strategy, Var varIndex) {
    this.strategy = strategy;
    this.varIndex = varIndex;
  }

  @Override
  public void emitDefaultInit(MethodGenerator mv, Optional<ExprGenerator> initialValue) {
    
    // allocate a unit array so that we can provide an "address" for this pointer
    mv.visitInsn(Opcodes.ICONST_1);
    mv.visitTypeInsn(Opcodes.ANEWARRAY, strategy.getJvmType().getInternalName());
    varIndex.store(mv);
    
    if(initialValue.isPresent()) {
      if(initialValue.get() instanceof NullPtrGenerator) {
        // array values already initialized to zero by VM
        
      } else {
        throw new UnsupportedOperationException("initialValue: " + initialValue);
      }
    }
  }

  @Override
  public void emitPushRecordRef(MethodGenerator mv) {
    varIndex.load(mv);
    mv.visitInsn(Opcodes.ICONST_0);
    mv.visitInsn(Opcodes.AALOAD);
  }

  @Override
  public void emitStore(MethodGenerator mv, ExprGenerator valueGenerator) {
    varIndex.load(mv);
    mv.visitInsn(Opcodes.ICONST_0);
    valueGenerator.emitPushRecordRef(mv);
    mv.visitInsn(Opcodes.AASTORE);
  }

  @Override
  public GimpleType getGimpleType() {
    return strategy.getRecordType().pointerTo();
  }

  @Override
  public ExprGenerator addressOf() {
    return new AddressExpr();
  }

  @Override
  public void emitPushPtrArrayAndOffset(MethodGenerator mv) {
    varIndex.load(mv);
    mv.visitInsn(Opcodes.ICONST_0);
    mv.visitInsn(Opcodes.AALOAD);
  }

  private class AddressExpr extends AbstractExprGenerator {


    @Override
    public GimpleType getGimpleType() {
      return strategy.getRecordType().pointerTo().pointerTo();
    }

    @Override
    public void emitPushPtrArrayAndOffset(MethodGenerator mv) {
      varIndex.load(mv);
      mv.visitInsn(Opcodes.ICONST_0);
    }
  }
  
}
