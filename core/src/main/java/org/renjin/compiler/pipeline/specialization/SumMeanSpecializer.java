package org.renjin.compiler.pipeline.specialization;

import org.renjin.compiler.pipeline.ComputeMethod;
import org.renjin.compiler.pipeline.fusion.Accessor;
import org.renjin.compiler.pipeline.fusion.Accessors;
import org.renjin.compiler.pipeline.fusion.InputGraph;
import org.renjin.compiler.pipeline.node.ComputationNode;
import org.renjin.compiler.pipeline.node.DeferredNode;
import org.renjin.repackaged.asm.Label;
import org.renjin.repackaged.asm.MethodVisitor;

import static org.renjin.repackaged.asm.Opcodes.*;

public class SumMeanSpecializer implements FunctionSpecializer {

  @Override
  public void compute(ComputeMethod method, DeferredNode node) {

    InputGraph inputGraph = new InputGraph(node);

    Accessor accessor = Accessors.create(node.getOperands().get(0), inputGraph);
    accessor.init(method);

    MethodVisitor mv = method.getVisitor();

    // get the length of the vector
    int lengthLocal = method.reserveLocal(1);
    accessor.pushLength(method);
    mv.visitVarInsn(ISTORE, lengthLocal);

    // initial the sum variable
    int sumLocal = method.reserveLocal(2);
    mv.visitInsn(DCONST_0);
    mv.visitVarInsn(DSTORE, sumLocal);

    int counterLocal = method.reserveLocal(1);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, counterLocal);

    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ILOAD, counterLocal);
    mv.visitVarInsn(ILOAD, lengthLocal);

    Label l4 = new Label();
    mv.visitJumpInsn(IF_ICMPEQ, l4);

    Label l5 = new Label();
    mv.visitLabel(l5);

    // load the sum on to the stack, and the next value
    mv.visitVarInsn(DLOAD, sumLocal);
    mv.visitVarInsn(ILOAD, counterLocal);
    accessor.pushElementAsDouble(method);

    // add the two values and store back into sum
    mv.visitInsn(DADD);
    mv.visitVarInsn(DSTORE, sumLocal);

    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitIincInsn(counterLocal, 1);
    mv.visitJumpInsn(GOTO, l3);
    mv.visitLabel(l4);

    // return result
    mv.visitInsn(ICONST_1);
    mv.visitIntInsn(NEWARRAY, T_DOUBLE);
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(DLOAD, sumLocal);
    
    if(((ComputationNode) node).getComputationName().equals("mean")) {
      mv.visitVarInsn(ILOAD, lengthLocal);
      mv.visitInsn(I2D);
      mv.visitInsn(DDIV);
    }
    
    mv.visitInsn(DASTORE);
    mv.visitInsn(ARETURN);
  }
}
