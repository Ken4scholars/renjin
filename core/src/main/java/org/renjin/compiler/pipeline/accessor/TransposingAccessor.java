/**
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2016 BeDataDriven Groep B.V. and contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.gnu.org/licenses/gpl-2.0.txt
 */
package org.renjin.compiler.pipeline.accessor;

import org.renjin.compiler.pipeline.ComputeMethod;
import org.renjin.compiler.pipeline.DeferredNode;
import org.renjin.repackaged.asm.MethodVisitor;

import static org.renjin.repackaged.asm.Opcodes.*;

public class TransposingAccessor extends Accessor {

  private Accessor operandAccessor;
  private Accessor sourceRowCountAccessor;
  private int sourceRowCountLocal;
  private int sourceColCountLocal;

  public TransposingAccessor(DeferredNode node, InputGraph inputGraph) {
    this.operandAccessor = Accessors.create(node.getOperand(0), inputGraph);
    this.sourceRowCountAccessor = Accessors.create(node.getOperand(1), inputGraph);
  }

  @Override
  public void init(ComputeMethod method) {
    operandAccessor.init(method);
    sourceRowCountAccessor.init(method);

    this.sourceRowCountLocal = method.reserveLocal(1);
    this.sourceColCountLocal = method.reserveLocal(1);

    // Store the source ncol and nrow into
    // local variables
    MethodVisitor mv = method.getVisitor();
    operandAccessor.pushLength(method);
    mv.visitInsn(ICONST_0);
    // stack => { length, rowCountVector, 0 }
    sourceRowCountAccessor.pushInt(method);
    // stack => { length, nrows }
    mv.visitInsn(DUP);
    // stack => { length, nrows, nrows }
    mv.visitVarInsn(ISTORE, sourceRowCountLocal);
    // stack => { length, nrows }
    mv.visitInsn(IDIV);
    // stack => { ncols }
    mv.visitVarInsn(ISTORE, sourceColCountLocal);
  }

  @Override
  public void pushDouble(ComputeMethod method) {
    MethodVisitor mv = method.getVisitor();
    mv.visitInsn(DUP);

    // here we have to compute the row/col given
    // the original dimensions of the matrix,
    // and then use these coordinates to find
    // the storage position in the transposed matrix
    // source row = index % ncol
    // source col = index / ncol
    // new index = col + row * nrow
    //           = (index / ncol) + (index % ncol) * nrow

    // stack => { index, index }
    mv.visitVarInsn(ILOAD, sourceColCountLocal);
    // stack => { index, index, ncol }
    mv.visitInsn(IDIV);
    // stack => { index, col }
    mv.visitInsn(SWAP);
    // stack => { col, index}
    mv.visitVarInsn(ILOAD, sourceColCountLocal);
    // stack => { col, index, ncols }
    mv.visitInsn(IREM);
    // stack => { col, row }
    mv.visitVarInsn(ILOAD, sourceRowCountLocal);
    // stack => { col, row, nrow }
    mv.visitInsn(IMUL);
    // stack => { col, row*nrow }
    mv.visitInsn(IADD);
    // stack => { transposed index }
    operandAccessor.pushDouble(method);
  }

  @Override
  public void pushLength(ComputeMethod method) {
    operandAccessor.pushLength(method);
  }
}
