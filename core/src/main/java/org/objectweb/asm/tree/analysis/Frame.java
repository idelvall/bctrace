// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.objectweb.asm.tree.analysis;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * A symbolic execution stack frame. A stack frame contains a set of local variable slots, and an
 * operand stack. Warning: long and double values are represented with <i>two</i> slots in local
 * variables, and with <i>one</i> slot in the operand stack.
 *
 * @param <V> type of the Value used for the analysis.
 * @author Eric Bruneton
 */
public class Frame<V extends Value> {

  /**
   * The expected return type of the analyzed method, or <tt>null</tt> if the method returns void.
   */
  private V returnValue;

  /**
   * The local variables and the operand stack of this frame. The first {@link #nLocals} elements
   * correspond to the local variables. The following {@link #nStack} elements correspond to the
   * operand stack.
   */
  private V[] values;

  /** The number of local variables of this frame. */
  private int nLocals;

  /** The number of elements in the operand stack. */
  private int nStack;

  /**
   * Constructs a new frame with the given size.
   *
   * @param nLocals the maximum number of local variables of the frame.
   * @param nStack the maximum stack size of the frame.
   */
  @SuppressWarnings("unchecked")
  public Frame(final int nLocals, final int nStack) {
    this.values = (V[]) new Value[nLocals + nStack];
    this.nLocals = nLocals;
  }

  /**
   * Constructs a copy of the given.
   *
   * @param frame a frame.
   */
  public Frame(final Frame<? extends V> frame) {
    this(frame.nLocals, frame.values.length - frame.nLocals);
    init(frame);
  }

  /**
   * Copies the state of the given frame into this frame.
   *
   * @param frame a frame.
   * @return this frame.
   */
  public Frame<V> init(final Frame<? extends V> frame) {
    returnValue = frame.returnValue;
    System.arraycopy(frame.values, 0, values, 0, values.length);
    nStack = frame.nStack;
    return this;
  }

  /**
   * Sets the expected return type of the analyzed method.
   *
   * @param v the expected return type of the analyzed method, or <tt>null</tt> if the method
   *     returns void.
   */
  public void setReturn(final V v) {
    returnValue = v;
  }

  /**
   * Returns the maximum number of local variables of this frame.
   *
   * @return the maximum number of local variables of this frame.
   */
  public int getLocals() {
    return nLocals;
  }

  /**
   * Returns the maximum stack size of this frame.
   *
   * @return the maximum stack size of this frame.
   */
  public int getMaxStackSize() {
    return values.length - nLocals;
  }

  /**
   * Returns the value of the given local variable.
   *
   * @param index a local variable index.
   * @return the value of the given local variable.
   * @throws IndexOutOfBoundsException if the variable does not exist.
   */
  public V getLocal(final int index) {
    if (index >= nLocals) {
      throw new IndexOutOfBoundsException("Trying to access an inexistant local variable");
    }
    return values[index];
  }

  /**
   * Sets the value of the given local variable.
   *
   * @param index a local variable index.
   * @param value the new value of this local variable.
   * @throws IndexOutOfBoundsException if the variable does not exist.
   */
  public void setLocal(final int index, final V value) {
    if (index >= nLocals) {
      throw new IndexOutOfBoundsException("Trying to access an inexistant local variable " + index);
    }
    values[index] = value;
  }

  /**
   * Returns the number of values in the operand stack of this frame. Long and double values are
   * treated as single values.
   *
   * @return the number of values in the operand stack of this frame.
   */
  public int getStackSize() {
    return nStack;
  }

  /**
   * Returns the value of the given operand stack slot.
   *
   * @param index the index of an operand stack slot.
   * @return the value of the given operand stack slot.
   * @throws IndexOutOfBoundsException if the operand stack slot does not exist.
   */
  public V getStack(final int index) {
    return values[nLocals + index];
  }

  /** Clears the operand stack of this frame. */
  public void clearStack() {
    nStack = 0;
  }

  /**
   * Pops a value from the operand stack of this frame.
   *
   * @return the value that has been popped from the stack.
   * @throws IndexOutOfBoundsException if the operand stack is empty.
   */
  public V pop() {
    if (nStack == 0) {
      throw new IndexOutOfBoundsException("Cannot pop operand off an empty stack.");
    }
    return values[nLocals + (--nStack)];
  }

  /**
   * Pushes a value into the operand stack of this frame.
   *
   * @param value the value that must be pushed into the stack.
   * @throws IndexOutOfBoundsException if the operand stack is full.
   */
  public void push(final V value) {
    if (nLocals + nStack >= values.length) {
      throw new IndexOutOfBoundsException("Insufficient maximum stack size.");
    }
    values[nLocals + (nStack++)] = value;
  }

  /**
   * Simulates the execution of the given instruction on this execution stack frame.
   *
   * @param insn the instruction to execute.
   * @param interpreter the interpreter to use to compute values from other values.
   * @throws AnalyzerException if the instruction cannot be executed on this execution frame (e.g. a
   *     POP on an empty operand stack).
   */
  public void execute(final AbstractInsnNode insn, final Interpreter<V> interpreter)
      throws AnalyzerException {
    V value1;
    V value2;
    V value3;
    V value4;
    int var;

    switch (insn.getOpcode()) {
      case Opcodes.NOP:
        break;
      case Opcodes.ACONST_NULL:
      case Opcodes.ICONST_M1:
      case Opcodes.ICONST_0:
      case Opcodes.ICONST_1:
      case Opcodes.ICONST_2:
      case Opcodes.ICONST_3:
      case Opcodes.ICONST_4:
      case Opcodes.ICONST_5:
      case Opcodes.LCONST_0:
      case Opcodes.LCONST_1:
      case Opcodes.FCONST_0:
      case Opcodes.FCONST_1:
      case Opcodes.FCONST_2:
      case Opcodes.DCONST_0:
      case Opcodes.DCONST_1:
      case Opcodes.BIPUSH:
      case Opcodes.SIPUSH:
      case Opcodes.LDC:
        push(interpreter.newOperation(insn));
        break;
      case Opcodes.ILOAD:
      case Opcodes.LLOAD:
      case Opcodes.FLOAD:
      case Opcodes.DLOAD:
      case Opcodes.ALOAD:
        push(interpreter.copyOperation(insn, getLocal(((VarInsnNode) insn).var)));
        break;
      case Opcodes.ISTORE:
      case Opcodes.LSTORE:
      case Opcodes.FSTORE:
      case Opcodes.DSTORE:
      case Opcodes.ASTORE:
        value1 = interpreter.copyOperation(insn, pop());
        var = ((VarInsnNode) insn).var;
        setLocal(var, value1);
        if (value1.getSize() == 2) {
          setLocal(var + 1, interpreter.newValue(null));
        }
        if (var > 0) {
          Value local = getLocal(var - 1);
          if (local != null && local.getSize() == 2) {
            setLocal(var - 1, interpreter.newValue(null));
          }
        }
        break;
      case Opcodes.IASTORE:
      case Opcodes.LASTORE:
      case Opcodes.FASTORE:
      case Opcodes.DASTORE:
      case Opcodes.AASTORE:
      case Opcodes.BASTORE:
      case Opcodes.CASTORE:
      case Opcodes.SASTORE:
        value3 = pop();
        value2 = pop();
        value1 = pop();
        interpreter.ternaryOperation(insn, value1, value2, value3);
        break;
      case Opcodes.POP:
        if (pop().getSize() == 2) {
          throw new AnalyzerException(insn, "Illegal use of POP");
        }
        break;
      case Opcodes.POP2:
        if (pop().getSize() == 1 && pop().getSize() != 1) {
          throw new AnalyzerException(insn, "Illegal use of POP2");
        }
        break;
      case Opcodes.DUP:
        value1 = pop();
        if (value1.getSize() != 1) {
          throw new AnalyzerException(insn, "Illegal use of DUP");
        }
        push(value1);
        push(interpreter.copyOperation(insn, value1));
        break;
      case Opcodes.DUP_X1:
        value1 = pop();
        value2 = pop();
        if (value1.getSize() != 1 || value2.getSize() != 1) {
          throw new AnalyzerException(insn, "Illegal use of DUP_X1");
        }
        push(interpreter.copyOperation(insn, value1));
        push(value2);
        push(value1);
        break;
      case Opcodes.DUP_X2:
        value1 = pop();
        if (value1.getSize() == 1) {
          value2 = pop();
          if (value2.getSize() == 1) {
            value3 = pop();
            if (value3.getSize() == 1) {
              push(interpreter.copyOperation(insn, value1));
              push(value3);
              push(value2);
              push(value1);
              break;
            }
          } else {
            push(interpreter.copyOperation(insn, value1));
            push(value2);
            push(value1);
            break;
          }
        }
        throw new AnalyzerException(insn, "Illegal use of DUP_X2");
      case Opcodes.DUP2:
        value1 = pop();
        if (value1.getSize() == 1) {
          value2 = pop();
          if (value2.getSize() == 1) {
            push(value2);
            push(value1);
            push(interpreter.copyOperation(insn, value2));
            push(interpreter.copyOperation(insn, value1));
            break;
          }
        } else {
          push(value1);
          push(interpreter.copyOperation(insn, value1));
          break;
        }
        throw new AnalyzerException(insn, "Illegal use of DUP2");
      case Opcodes.DUP2_X1:
        value1 = pop();
        if (value1.getSize() == 1) {
          value2 = pop();
          if (value2.getSize() == 1) {
            value3 = pop();
            if (value3.getSize() == 1) {
              push(interpreter.copyOperation(insn, value2));
              push(interpreter.copyOperation(insn, value1));
              push(value3);
              push(value2);
              push(value1);
              break;
            }
          }
        } else {
          value2 = pop();
          if (value2.getSize() == 1) {
            push(interpreter.copyOperation(insn, value1));
            push(value2);
            push(value1);
            break;
          }
        }
        throw new AnalyzerException(insn, "Illegal use of DUP2_X1");
      case Opcodes.DUP2_X2:
        value1 = pop();
        if (value1.getSize() == 1) {
          value2 = pop();
          if (value2.getSize() == 1) {
            value3 = pop();
            if (value3.getSize() == 1) {
              value4 = pop();
              if (value4.getSize() == 1) {
                push(interpreter.copyOperation(insn, value2));
                push(interpreter.copyOperation(insn, value1));
                push(value4);
                push(value3);
                push(value2);
                push(value1);
                break;
              }
            } else {
              push(interpreter.copyOperation(insn, value2));
              push(interpreter.copyOperation(insn, value1));
              push(value3);
              push(value2);
              push(value1);
              break;
            }
          }
        } else {
          value2 = pop();
          if (value2.getSize() == 1) {
            value3 = pop();
            if (value3.getSize() == 1) {
              push(interpreter.copyOperation(insn, value1));
              push(value3);
              push(value2);
              push(value1);
              break;
            }
          } else {
            push(interpreter.copyOperation(insn, value1));
            push(value2);
            push(value1);
            break;
          }
        }
        throw new AnalyzerException(insn, "Illegal use of DUP2_X2");
      case Opcodes.SWAP:
        value2 = pop();
        value1 = pop();
        if (value1.getSize() != 1 || value2.getSize() != 1) {
          throw new AnalyzerException(insn, "Illegal use of SWAP");
        }
        push(interpreter.copyOperation(insn, value2));
        push(interpreter.copyOperation(insn, value1));
        break;
      case Opcodes.IALOAD:
      case Opcodes.LALOAD:
      case Opcodes.FALOAD:
      case Opcodes.DALOAD:
      case Opcodes.AALOAD:
      case Opcodes.BALOAD:
      case Opcodes.CALOAD:
      case Opcodes.SALOAD:
      case Opcodes.IADD:
      case Opcodes.LADD:
      case Opcodes.FADD:
      case Opcodes.DADD:
      case Opcodes.ISUB:
      case Opcodes.LSUB:
      case Opcodes.FSUB:
      case Opcodes.DSUB:
      case Opcodes.IMUL:
      case Opcodes.LMUL:
      case Opcodes.FMUL:
      case Opcodes.DMUL:
      case Opcodes.IDIV:
      case Opcodes.LDIV:
      case Opcodes.FDIV:
      case Opcodes.DDIV:
      case Opcodes.IREM:
      case Opcodes.LREM:
      case Opcodes.FREM:
      case Opcodes.DREM:
      case Opcodes.ISHL:
      case Opcodes.LSHL:
      case Opcodes.ISHR:
      case Opcodes.LSHR:
      case Opcodes.IUSHR:
      case Opcodes.LUSHR:
      case Opcodes.IAND:
      case Opcodes.LAND:
      case Opcodes.IOR:
      case Opcodes.LOR:
      case Opcodes.IXOR:
      case Opcodes.LXOR:
      case Opcodes.LCMP:
      case Opcodes.FCMPL:
      case Opcodes.FCMPG:
      case Opcodes.DCMPL:
      case Opcodes.DCMPG:
        value2 = pop();
        value1 = pop();
        push(interpreter.binaryOperation(insn, value1, value2));
        break;
      case Opcodes.INEG:
      case Opcodes.LNEG:
      case Opcodes.FNEG:
      case Opcodes.DNEG:
        push(interpreter.unaryOperation(insn, pop()));
        break;
      case Opcodes.IINC:
        var = ((IincInsnNode) insn).var;
        setLocal(var, interpreter.unaryOperation(insn, getLocal(var)));
        break;
      case Opcodes.I2L:
      case Opcodes.I2F:
      case Opcodes.I2D:
      case Opcodes.L2I:
      case Opcodes.L2F:
      case Opcodes.L2D:
      case Opcodes.F2I:
      case Opcodes.F2L:
      case Opcodes.F2D:
      case Opcodes.D2I:
      case Opcodes.D2L:
      case Opcodes.D2F:
      case Opcodes.I2B:
      case Opcodes.I2C:
      case Opcodes.I2S:
        push(interpreter.unaryOperation(insn, pop()));
        break;
      case Opcodes.IFEQ:
      case Opcodes.IFNE:
      case Opcodes.IFLT:
      case Opcodes.IFGE:
      case Opcodes.IFGT:
      case Opcodes.IFLE:
        interpreter.unaryOperation(insn, pop());
        break;
      case Opcodes.IF_ICMPEQ:
      case Opcodes.IF_ICMPNE:
      case Opcodes.IF_ICMPLT:
      case Opcodes.IF_ICMPGE:
      case Opcodes.IF_ICMPGT:
      case Opcodes.IF_ICMPLE:
      case Opcodes.IF_ACMPEQ:
      case Opcodes.IF_ACMPNE:
      case Opcodes.PUTFIELD:
        value2 = pop();
        value1 = pop();
        interpreter.binaryOperation(insn, value1, value2);
        break;
      case Opcodes.GOTO:
        break;
      case Opcodes.JSR:
        push(interpreter.newOperation(insn));
        break;
      case Opcodes.RET:
        break;
      case Opcodes.TABLESWITCH:
      case Opcodes.LOOKUPSWITCH:
        interpreter.unaryOperation(insn, pop());
        break;
      case Opcodes.IRETURN:
      case Opcodes.LRETURN:
      case Opcodes.FRETURN:
      case Opcodes.DRETURN:
      case Opcodes.ARETURN:
        value1 = pop();
        interpreter.unaryOperation(insn, value1);
        interpreter.returnOperation(insn, value1, returnValue);
        break;
      case Opcodes.RETURN:
        if (returnValue != null) {
          throw new AnalyzerException(insn, "Incompatible return type");
        }
        break;
      case Opcodes.GETSTATIC:
        push(interpreter.newOperation(insn));
        break;
      case Opcodes.PUTSTATIC:
        interpreter.unaryOperation(insn, pop());
        break;
      case Opcodes.GETFIELD:
        push(interpreter.unaryOperation(insn, pop()));
        break;
      case Opcodes.INVOKEVIRTUAL:
      case Opcodes.INVOKESPECIAL:
      case Opcodes.INVOKESTATIC:
      case Opcodes.INVOKEINTERFACE:
        {
          List<V> valueList = new ArrayList<V>();
          String methodDescriptor = ((MethodInsnNode) insn).desc;
          for (int i = Type.getArgumentTypes(methodDescriptor).length; i > 0; --i) {
            valueList.add(0, pop());
          }
          if (insn.getOpcode() != Opcodes.INVOKESTATIC) {
            valueList.add(0, pop());
          }
          if (Type.getReturnType(methodDescriptor) == Type.VOID_TYPE) {
            interpreter.naryOperation(insn, valueList);
          } else {
            push(interpreter.naryOperation(insn, valueList));
          }
          break;
        }
      case Opcodes.INVOKEDYNAMIC:
        {
          List<V> valueList = new ArrayList<V>();
          String methodDesccriptor = ((InvokeDynamicInsnNode) insn).desc;
          for (int i = Type.getArgumentTypes(methodDesccriptor).length; i > 0; --i) {
            valueList.add(0, pop());
          }
          if (Type.getReturnType(methodDesccriptor) == Type.VOID_TYPE) {
            interpreter.naryOperation(insn, valueList);
          } else {
            push(interpreter.naryOperation(insn, valueList));
          }
          break;
        }
      case Opcodes.NEW:
        push(interpreter.newOperation(insn));
        break;
      case Opcodes.NEWARRAY:
      case Opcodes.ANEWARRAY:
      case Opcodes.ARRAYLENGTH:
        push(interpreter.unaryOperation(insn, pop()));
        break;
      case Opcodes.ATHROW:
        interpreter.unaryOperation(insn, pop());
        break;
      case Opcodes.CHECKCAST:
      case Opcodes.INSTANCEOF:
        push(interpreter.unaryOperation(insn, pop()));
        break;
      case Opcodes.MONITORENTER:
      case Opcodes.MONITOREXIT:
        interpreter.unaryOperation(insn, pop());
        break;
      case Opcodes.MULTIANEWARRAY:
        List<V> valueList = new ArrayList<V>();
        for (int i = ((MultiANewArrayInsnNode) insn).dims; i > 0; --i) {
          valueList.add(0, pop());
        }
        push(interpreter.naryOperation(insn, valueList));
        break;
      case Opcodes.IFNULL:
      case Opcodes.IFNONNULL:
        interpreter.unaryOperation(insn, pop());
        break;
      default:
        throw new AnalyzerException(insn, "Illegal opcode " + insn.getOpcode());
    }
  }

  /**
   * Merges the given frame into this frame.
   *
   * @param frame a frame. This frame is left unchanged by this method.
   * @param interpreter the interpreter used to merge values.
   * @return <tt>true</tt> if this frame has been changed as a result of the merge operation, or
   *     <tt>false</tt> otherwise.
   * @throws AnalyzerException if the frames have incompatible sizes.
   */
  public boolean merge(final Frame<? extends V> frame, final Interpreter<V> interpreter)
      throws AnalyzerException {
    if (nStack != frame.nStack) {
      throw new AnalyzerException(null, "Incompatible stack heights");
    }
    boolean changed = false;
    for (int i = 0; i < nLocals + nStack; ++i) {
      V v = interpreter.merge(values[i], frame.values[i]);
      if (!v.equals(values[i])) {
        values[i] = v;
        changed = true;
      }
    }
    return changed;
  }

  /**
   * Merges the given frame into this frame (case of a subroutine). The operand stacks are not
   * merged, and only the local variables that have not been used by the subroutine are merged.
   *
   * @param frame a frame. This frame is left unchanged by this method.
   * @param localsUsed the local variables that are read or written by the subroutine. The i-th
   *     element is true if and only if the local variable at index i is read or written by the
   *     subroutine.
   * @return <tt>true</tt> if this frame has been changed as a result of the merge operation, or
   *     <tt>false</tt> otherwise.
   */
  public boolean merge(final Frame<? extends V> frame, final boolean[] localsUsed) {
    boolean changed = false;
    for (int i = 0; i < nLocals; ++i) {
      if (!localsUsed[i] && !values[i].equals(frame.values[i])) {
        values[i] = frame.values[i];
        changed = true;
      }
    }
    return changed;
  }

  /**
   * Returns a string representation of this frame.
   *
   * @return a string representation of this frame.
   */
  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < getLocals(); ++i) {
      stringBuilder.append(getLocal(i));
    }
    stringBuilder.append(' ');
    for (int i = 0; i < getStackSize(); ++i) {
      stringBuilder.append(getStack(i).toString());
    }
    return stringBuilder.toString();
  }
}
