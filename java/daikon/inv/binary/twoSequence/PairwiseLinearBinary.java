package daikon.inv.binary.twoSequence;

import daikon.*;
import daikon.inv.Invariant;
import daikon.inv.binary.twoScalar.*;

public class PairwiseLinearBinary
  extends TwoSequence
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff PairwiseLinearBinary invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  LinearBinaryCore core;

  protected PairwiseLinearBinary(PptSlice ppt) {
    super(ppt);
    core = new LinearBinaryCore(this);
  }

  public static PairwiseLinearBinary instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;
    return new PairwiseLinearBinary(ppt);
  }

  protected Object clone() {
    PairwiseLinearBinary result = (PairwiseLinearBinary) super.clone();
    result.core = (LinearBinaryCore) core.clone();
    result.core.wrapper = result;
    return result;
  }

  protected Invariant resurrect_done_swapped() {
    core.swap();
    return this;
  }

  public String repr() {
    return "PairwiseLinearBinary" + varNames() + ": "
      + "no_invariant=" + no_invariant
      + "; " + core.repr();
  }

  public String format_using(OutputFormat format) {
    if (format == OutputFormat.DAIKON) return format_daikon();
    if (format == OutputFormat.IOA) return format_ioa();

    return format_unimplemented(format);
  }

  public String format_daikon() {
    return core.format_using(OutputFormat.DAIKON, var1().name, var2().name);
  }

  /* IOA */
  public String format_ioa() {
    if (var1().isIOASet() || var2().isIOASet())
      return "Not valid for sets: " + format();
    VarInfoName.QuantHelper.IOAQuantification quant1 = new VarInfoName.QuantHelper.IOAQuantification(var1());
    VarInfoName.QuantHelper.IOAQuantification quant2 = new VarInfoName.QuantHelper.IOAQuantification(var2());

    return quant1.getQuantifierExp()
      + core.format_using(OutputFormat.IOA,
			  quant1.getVarIndexed(0),
			  quant2.getVarIndexed(0))
      + quant1.getClosingExp();
  }

  public void add_modified(long[] x_arr, long[] y_arr, int count) {
    if (x_arr.length != y_arr.length) {
      flowThis();
      destroy();
      return;
    }
    int len = x_arr.length;
    // int len = Math.min(x_arr.length, y_arr.length);

    for (int i=0; i<len; i++) {
      long x = x_arr[i];
      long y = y_arr[i];

      core.add_modified(x, y, count);
      if (no_invariant) {
        // destroy() must have already been called
        return;
      }
    }
  }

  protected double computeProbability() {
    return core.computeProbability();
  }

  public boolean isSameFormula(Invariant other)
  {
    return core.isSameFormula(((PairwiseLinearBinary) other).core);
  }

  public boolean isExclusiveFormula(Invariant other)
  {
    if (other instanceof PairwiseLinearBinary) {
      return core.isExclusiveFormula(((PairwiseLinearBinary) other).core);
    }
    return false;
  }

}
