package org.renjin.compiler.ir.tac.expressions;

import com.google.common.base.Joiner;
import com.sun.codemodel.JMethod;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.renjin.compiler.emit.EmitContext;
import org.renjin.compiler.ir.ssa.VariableMap;
import org.renjin.eval.Context;
import org.renjin.invoke.model.JvmMethod;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Expression which targets a set of JVM methods
 */
public class JvmMethodCall implements CallExpression {

  private final String name;
  private final List<Expression> arguments;
  private List<JvmMethod> overloads;
  private String[] argumentNames;
  private Class type;
  private JvmMethod method;

  public JvmMethodCall(String name, List<JvmMethod> overloads, String[] argumentNames, List<Expression> arguments) {
    super();
    this.name = name;
    this.arguments = arguments;
    this.argumentNames = argumentNames;
    this.overloads = overloads;

    method = overloads.get(0);
  }

  public List<Expression> getArguments() {
    return arguments;
  }

  @Override
  public String toString() {
    return "(" + name + " " + Joiner.on(" ").join(arguments) + ")";
  }

  @Override
  public boolean isDefinitelyPure() {
    for(JvmMethod overload : overloads) {
      if(!overload.isDeferrable()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int emitPush(EmitContext emitContext, MethodVisitor mv) {

    int stackIncreaseRequiredForArguments = 0;

    int paramIndex = 0;
    Class<?>[] parameterTypes = method.getMethod().getParameterTypes();

    Iterator<Expression> argIt = arguments.iterator();

    // push all the arguments onto the stack
    for(JvmMethod.Argument arg : method.getAllArguments()) {
      if(arg.isContextual()) {
        throw new UnsupportedOperationException("Contextual args not yet supported");
      } else {
        Expression argumentExpr = argIt.next();
        if(!argumentExpr.getType().equals(parameterTypes[paramIndex])) {
          throw new IllegalStateException("Argument mismatch at " + paramIndex + ": expected " + parameterTypes[paramIndex] +
            ", but got " + argumentExpr.getType());
        }
        stackIncreaseRequiredForArguments +=
            argumentExpr.emitPush(emitContext, mv);
        paramIndex++;
      }
    }
    // now invoke the method
    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
        Type.getInternalName(method.getDeclaringClass()),
        method.getName(),
        Type.getMethodDescriptor(method.getMethod()), false);

    return Math.max(
        stackIncreaseRequiredForArguments,
        stackIncreaseRequiredForReturnValue());
  }

  private int stackIncreaseRequiredForReturnValue() {
    if(method.getReturnType().equals(double.class) ||
        method.getReturnType().equals(long.class)) {
      return 2;
    } else {
      return 1;
    }
  }

  @Override
  public Class getType() {
    return type;
  }

  @Override
  public Class resolveType(VariableMap variableMap) {

    if(type != null) {
      return type;
    }

    // get the types of our arguments
    Class[] argTypes = new Class[arguments.size()];
    for(int i=0;i!=argTypes.length;++i) {
      argTypes[i] = arguments.get(i).resolveType(variableMap);
    }

    // choose the overload based on matching types
    this.method = null;
    for(JvmMethod overload : overloads) {
      if(matches(overload, argTypes)) {
        if(method == null) {
          method = overload;
        } else {
          throw new UnsupportedOperationException("Multiple matching overloads.\n" +
              "Argument types = " + Arrays.toString(argTypes) + "\n" +
              "Overloads: " + Joiner.on("\n").join(overloads));
        }
      }
    }

    type = method.getReturnType();
    return type;
  }

  private boolean matches(JvmMethod overload, Class[] argTypes) {
    List<JvmMethod.Argument> formals = overload.getFormals();
    if(formals.size() != argTypes.length) {
      return false;
    }
    for(int i=0;i!=formals.size();++i) {
      if(!formals.get(i).getClazz().equals(argTypes[i])) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void setChild(int childIndex, Expression child) {
    arguments.set(childIndex, child);
  }

  @Override
  public int getChildCount() {
    return arguments.size();
  }

  @Override
  public Expression childAt(int index) {
    return arguments.get(index);
  }

  public List<String> getArgumentNames() {
    return Arrays.asList(argumentNames);
  }
}
