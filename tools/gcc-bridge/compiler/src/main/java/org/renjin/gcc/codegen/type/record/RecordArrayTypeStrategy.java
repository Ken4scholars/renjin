package org.renjin.gcc.codegen.type.record;

import org.objectweb.asm.Type;
import org.renjin.gcc.codegen.array.ArrayTypeStrategy;
import org.renjin.gcc.codegen.expr.*;
import org.renjin.gcc.codegen.fatptr.FatPtrExpr;
import org.renjin.gcc.codegen.fatptr.FatPtrStrategy;
import org.renjin.gcc.codegen.fatptr.Wrappers;
import org.renjin.gcc.codegen.type.*;
import org.renjin.gcc.codegen.var.VarAllocator;
import org.renjin.gcc.gimple.GimpleVarDecl;
import org.renjin.gcc.gimple.expr.GimpleConstructor;
import org.renjin.gcc.gimple.expr.GimpleFieldRef;
import org.renjin.gcc.gimple.type.GimpleArrayType;
import org.renjin.gcc.gimple.type.GimpleField;
import org.renjin.gcc.gimple.type.GimplePrimitiveType;
import org.renjin.gcc.gimple.type.GimpleRecordTypeDef;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.renjin.gcc.codegen.expr.Expressions.*;

/**
 * Represents a record with a primitive array.
 * 
 * <p>This strategy only works for records that have only primitives fields of the same type. For example,
 * the C struct:</p>
 * <pre>
 *   struct point {
 *     double x;
 *     double y;
 *   }
 * </pre>
 * <p>Can be compiled using a simple {@code double[]} instead of a full-blown JVM class. This makes it 
 * easy to allow pointers to records of such types to be cast back and forth between {@code double*} pointers
 * (or {@code int*} etc.)</p>
 */
public class RecordArrayTypeStrategy extends RecordTypeStrategy<RecordArrayExpr> {
  
  private GimplePrimitiveType fieldType;
  private Type arrayType;
  private int arrayLength;
  private final RecordArrayValueFunction valueFunction;
  
  public RecordArrayTypeStrategy(GimpleRecordTypeDef recordTypeDef) {
    super(recordTypeDef);
    fieldType = getPrimitiveType(recordTypeDef.getFields().get(0));
    arrayType = Wrappers.valueArrayType(fieldType.jvmType());
    arrayLength = recordTypeDef.getFields().size();
    valueFunction = new RecordArrayValueFunction(fieldType, arrayLength);
  }

  /**
   * Returns true if the given {@code recordTypeDef} can be compiled using this strategy.
   */
  public static boolean accept(GimpleRecordTypeDef recordTypeDef) {
    if(recordTypeDef.getFields().isEmpty()) {
      return false;
    }
    Iterator<GimpleField> it = recordTypeDef.getFields().iterator();
    if(!it.hasNext()) {
      return false;
    }
    GimplePrimitiveType type = getPrimitiveType(it.next());
    if(type == null) {
      return false;
    }
    while(it.hasNext()) {
      if(!type.equals(getPrimitiveType(it.next()))) {
        return false;
      }
    }
    return true;
  }
  
  private static GimplePrimitiveType getPrimitiveType(GimpleField field) {
    if(field.getType() instanceof GimplePrimitiveType) {
      return (GimplePrimitiveType) field.getType();
    } 
    return null;
  }

  @Override
  public void linkFields(TypeOracle typeOracle) {
    // NOOP
  }

  @Override
  public void writeClassFiles(File outputDirectory) throws IOException {
    // NOOP
    // We don't use classes
  }

  @Override
  public Expr memberOf(RecordArrayExpr instance, GimpleFieldRef fieldRef) {
    
    // All the fields in this record are necessarily primitives, so we need
    // simple to retrieve the element from within the array that corresponds to
    // the given field name
    SimpleExpr array = instance.getArray();
    SimpleExpr fieldOffset = constantInt(fieldRef.getOffsetBytes() / fieldType.sizeOf());
    SimpleExpr offset = sum(instance.getOffset(), fieldOffset);

    // Because this value is backed by an array, we can also make it addressable. 
    FatPtrExpr address = new FatPtrExpr(array, offset);
    SimpleExpr value = elementAt(array, offset);
    
    return new SimpleAddressableExpr(value, address);
  }

  @Override
  public ParamStrategy getParamStrategy() {
    return new RecordArrayParamStrategy(arrayType, arrayLength);
  }

  @Override
  public ReturnStrategy getReturnStrategy() {
    return new RecordArrayReturnStrategy(arrayType, arrayLength);
  }

  @Override
  public RecordArrayExpr variable(GimpleVarDecl decl, VarAllocator allocator) {
    
    SimpleLValue array = allocator.reserve(decl.getName(), arrayType, newArray(fieldType.jvmType(), arrayLength));
    
    return new RecordArrayVar(array, arrayLength);
  }

  @Override
  public RecordArrayExpr constructorExpr(ExprFactory exprFactory, GimpleConstructor value) {
    return new RecordArrayVar(newArray(fieldType.jvmType(), arrayLength), arrayLength);
  }

  @Override
  public FieldStrategy fieldGenerator(Type className, final String fieldName) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public FieldStrategy addressableFieldGenerator(Type className, String fieldName) {
    return fieldGenerator(className, fieldName);
  }

  @Override
  public PointerTypeStrategy pointerTo() {
    return new FatPtrStrategy(valueFunction);
  }

  @Override
  public ArrayTypeStrategy arrayOf(GimpleArrayType arrayType) {
    return new ArrayTypeStrategy(arrayType, valueFunction);
  }

}
