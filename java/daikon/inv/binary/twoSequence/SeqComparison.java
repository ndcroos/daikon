package daikon.inv.binary.twoSequence;

import daikon.*;
import daikon.inv.*;
import daikon.inv.binary.twoScalar.*;

import utilMDE.*;

import java.util.*;

// Lexically compares the two sequences.
public class SeqComparison
  extends TwoSequence
  implements Comparison
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff SeqComparison invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  static Comparator comparator = new ArraysMDE.LongArrayComparatorLexical();

  public final boolean only_check_eq;

  boolean can_be_eq = false;
  boolean can_be_lt = false;
  boolean can_be_gt = false;

  int num_sc_samples = 0;
  private ValueTracker values_cache = new ValueTracker(8);

  protected SeqComparison(PptSlice ppt, boolean only_eq) {
    super(ppt);
    only_check_eq = only_eq;
  }

  public static SeqComparison instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;

    VarInfo var1 = ppt.var_infos[0];
    VarInfo var2 = ppt.var_infos[1];

    // System.out.println("Ppt: " + ppt.name);
    // System.out.println("vars[0]: " + var1.type.format());
    // System.out.println("vars[1]: " + var2.type.format());

    if ((SubSequence.isObviousDerived(var1, var2))
        || (SubSequence.isObviousDerived(var2, var1))) {
      Global.implied_noninstantiated_invariants++;
      return null;
    }

    ProglangType type1 = var1.type;
    ProglangType type2 = var2.type;
    // This intentonally checks dimensions(), not pseudoDimensions.
    boolean only_eq = (! ((type1.dimensions() == 1)
                          && type1.baseIsIntegral()
                          && (type2.dimensions() == 1)
                          && type2.baseIsIntegral()));
    // System.out.println("only_eq: " + only_eq);
    return new SeqComparison(ppt, only_eq);
  }

  protected Object clone() {
    SeqComparison result = (SeqComparison) super.clone();
    result.values_cache = (ValueTracker) values_cache.clone();
    return result;
  }

  protected Invariant resurrect_done_swapped() {
    boolean tmp = can_be_lt;
    can_be_gt = can_be_lt;
    can_be_gt = tmp;
    return this;
  }

  public String repr() {
    return "SeqComparison" + varNames() + ": "
      + "can_be_eq=" + can_be_eq
      + ",can_be_lt=" + can_be_lt
      + ",can_be_gt=" + can_be_gt
      + ",only_check_eq=" + only_check_eq;
  }

  public String format_using(OutputFormat format) {
    String name1 = var1().name.name_using(format);
    String name2 = var2().name.name_using(format);
    String comparator = IntComparisonCore.format_comparator
      (format, can_be_lt, can_be_eq, can_be_gt);

    if ((format == OutputFormat.DAIKON)
	|| (format == OutputFormat.JAVA))
    {
      return name1 + " " + comparator + " " + name1 + " (lexically)";
    }

    if (format == OutputFormat.IOA) {
      if (var1().isIOASet() || var2().isIOASet()) {
	return "Not valid for Sets: " + format();
      }
      return name1 + " " + comparator + " " + name2 + " ***";
    }

    return format_unimplemented(format);
  }

  public void add_modified(long[] v1, long[] v2, int count) {
    /// This does not do the right thing; I really want to avoid comparisons
    /// if one is missing, but not if one is zero-length.
    // Don't make comparisons with empty arrays.
    if ((v1.length == 0) || (v2.length == 0)) {
      return;
    }
    num_sc_samples += count;

    int comparison = comparator.compare(v1, v2);
    // System.out.println("SeqComparison" + varNames() + ": "
    //                    + "compare(" + ArraysMDE.toString(v1)
    //                    + ", " + ArraysMDE.toString(v2) + ") = " + comparison);

    boolean new_can_be_eq = can_be_eq;
    boolean new_can_be_lt = can_be_lt;
    boolean new_can_be_gt = can_be_gt;
    boolean changed = false;
    if (comparison == 0) {
      new_can_be_eq = true;
      changed = true;
    } else if (comparison < 0) {
      new_can_be_lt = true;
      changed = true;
    } else {
      new_can_be_gt = true;
      changed = true;
    }

    if (! changed) {
      values_cache.add(v1, v2);
      return;
    }

    if ((new_can_be_lt && new_can_be_gt)
        || (only_check_eq && (new_can_be_lt || new_can_be_gt))) {
      flowThis();
      destroy();
      return;
    }

    // changed but didn't die
    flowClone();
    can_be_eq = new_can_be_eq;
    can_be_lt = new_can_be_lt;
    can_be_gt = new_can_be_gt;

    values_cache.add(v1, v2);
  }

  protected double computeProbability() {
    if (no_invariant) {
      return Invariant.PROBABILITY_NEVER;
    } else if (can_be_lt || can_be_gt) {
      // System.out.println("prob = " + Math.pow(.5, ppt.num_values()) + " for " + format());
      return Math.pow(.5, values_cache.num_values());
    } else if (num_sc_samples == 0) {
      return Invariant.PROBABILITY_UNJUSTIFIED;
    } else {
      return Invariant.PROBABILITY_JUSTIFIED;
    }
  }

  // For Comparison interface
  public double eq_probability() {
    if (can_be_eq && (!can_be_lt) && (!can_be_gt))
      return computeProbability();
    else
      return Invariant.PROBABILITY_NEVER;
  }

  public boolean isSameFormula(Invariant o)
  {
    SeqComparison other = (SeqComparison) o;
    return
      (can_be_eq == other.can_be_eq) &&
      (can_be_lt == other.can_be_lt) &&
      (can_be_gt == other.can_be_gt);
  }

  public boolean isExclusiveFormula(Invariant o)
  {
    if (o instanceof SeqComparison) {
      SeqComparison other = (SeqComparison) o;
      return (! ((can_be_eq && other.can_be_eq)
                 || (can_be_lt && other.can_be_lt)
                 || (can_be_gt && other.can_be_gt)));
    }
    return false;
  }

  // Copied from IntComparison.
  public boolean isObviousImplied() {
    PairwiseIntComparison pic = PairwiseIntComparison.find(ppt);
    if ((pic != null)
        && (pic.core.can_be_eq == can_be_eq)
        && (pic.core.can_be_lt == can_be_lt)
        && (pic.core.can_be_gt == can_be_gt)) {
      return true;
    }

    return false;
  }

}
