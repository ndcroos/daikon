package daikon.inv.ternary.threeScalar;

import daikon.*;
import daikon.inv.Invariant;
import java.lang.reflect.*;


public class FunctionBinary
  extends ThreeScalar
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  public static boolean dkconfig_enabled = true;

  final static boolean debugFunctionBinary = false;

  FunctionBinaryCore core;

  protected FunctionBinary(PptSlice ppt, String methodname, Method function, int var_order) {
    super(ppt);
    core = new FunctionBinaryCore(this, methodname, function, var_order);
  }

  public static FunctionBinary instantiate(PptSlice ppt, String methodname, Method function, int var_order) {
    if (!dkconfig_enabled) return null;

    {
      int[] indices = FunctionBinaryCore.var_indices[var_order];
      VarInfo argresult = ppt.var_infos[indices[0]];
      VarInfo arg1 = ppt.var_infos[indices[1]];
      VarInfo arg2 = ppt.var_infos[indices[2]];
      if (debugFunctionBinary)
        System.out.println("FunctionBinary.instantiate(" + ppt.name + ", " + function.getName() + ", " + argresult.name + "=" + "f(" + arg1.name + "," + arg2.name + ")" + " )");
    }

    // SUPPRESS INVARIANT: if any var is constant, suppress FunctionBinary.
    // Skip if the arguments are constant (but not if the result is
    // constant, as we might get something like y=abs(x)).  (Actually, for
    // now I'm skipping if the result is constant, too: that's a
    // relationship over the two arguments, not a ternary relationship.)
    /* [INCR]
    int[] indices = FunctionBinaryCore.var_indices[var_order];
    VarInfo resultvar = ppt.var_infos[indices[0]];
    VarInfo arg1 = ppt.var_infos[indices[1]];
    VarInfo arg2 = ppt.var_infos[indices[2]];
    if (resultvar.isConstant() || (arg1.isConstant() && arg2.isConstant())) {
      if (debugFunctionBinary)
        System.out.println("FunctionBinary.instantiate: both args are constant");
      Global.subexact_noninstantiated_invariants++;
      return null;
    }
    */

    return new FunctionBinary(ppt, methodname, function, var_order);
  }

  protected Object clone() {
    FunctionBinary result = (FunctionBinary) super.clone();
    result.core = (FunctionBinaryCore) core.clone();
    result.core.wrapper = result;
    return result;
  }

  protected Invariant resurrect_done(int[] permutation) {
    core.permute(permutation);
    return this;
  }

  public String repr() {
    return "FunctionBinary" + varNames() + ": ";
  }

  public String format() {
    return core.format();
  }

  /* IOA */
  public String format_ioa() {
    return core.format_ioa();
  }

  public String format_esc() {
    String classname = this.getClass().toString().substring(6); // remove leading "class"
    return "warning: method " + classname + ".format_esc() needs to be implemented: " + format();
  }

  public String format_simplify() {
    String classname = this.getClass().toString().substring(6); // remove leading "class"
    return "warning: method " + classname + ".format_simplify() needs to be implemented: " + format();
  }

  public void add_modified(long x_int, long y_int, long z_int, int count) {
    core.add_modified(x_int, y_int, z_int, count);
  }


  protected double computeProbability() {
    return core.computeProbability();
  }

  public boolean isSameFormula(Invariant other)
  {
    return core.isSameFormula(((FunctionBinary) other).core);
  }

  // // For testing only; to be commented out
  // public void destroy() {
  //   if (debugFunctionBinary) {
  //     Method function = core.function;
  //     int var_order = core.var_order;
  //     int[] indices = FunctionBinaryCore.var_indices[var_order];
  //     VarInfo argresult = ppt.var_infos[indices[0]];
  //     VarInfo arg1 = ppt.var_infos[indices[1]];
  //     VarInfo arg2 = ppt.var_infos[indices[2]];
  //     System.out.println("FunctionBinary.destroy: "
  //                        + argresult.name + " = "
  //                        + function.getName() + "(" + arg1.name + ", " + arg2.name + ")");
  //   }
  //   super.destroy();
  // }

}
