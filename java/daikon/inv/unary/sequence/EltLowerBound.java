package daikon.inv.unary.sequence;

import daikon.*;
import daikon.inv.*;
import daikon.inv.unary.sequence.*;
import daikon.inv.unary.scalar.*;
import daikon.inv.unary.*;
import daikon.inv.binary.sequenceScalar.*;
import daikon.inv.binary.twoSequence.*;
import daikon.derive.unary.*;
import utilMDE.*;

import java.util.*;

// *****
// Do not edit this file directly:
// it is automatically generated from Bound.java.jpp
// *****

// One reason not to combine LowerBound and Upperbound is that they have
// separate justifications:  one may be justified when the other is not.

// What should we do if there are few values in the range?
// This can make justifying that invariant easier, because with few values
// naturally there are more instances of each value.
// This might also make justifying that invariant harder, because to get more
// than (say) twice the expected number of samples (under the assumption of
// uniform distribution) requires many samples.
// Which of these dominates?  Is the behavior what I want?

public class EltLowerBound 
  extends SingleSequence 
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff EltLowerBound  invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;
  /**
   * Long integer.  Together with maximal_interesting, specifies the
   * range of the computed constant that is "intersting" --- the range
   * that should be reported.  For instance, setting this to -1 and
   * maximal_interesting to 2 would only permit output of EltLowerBound 
   * invariants whose cutoff was one of (-1,0,1,2).
   **/
  public static long dkconfig_minimal_interesting = Long.MIN_VALUE;
  /**
   * Long integer.  Together with minimal_interesting, specifies the
   * range of the computed constant that is "intersting" --- the range
   * that should be reported.  For instance, setting
   * minimal_interesting to -1 and this to 2 would only permit output
   * of EltLowerBound  invariants whose cutoff was one of (-1,0,1,2).
   **/
  public static long dkconfig_maximal_interesting = Long.MAX_VALUE;

  public LowerBoundCore  core;

  private EltLowerBound (PptSlice ppt) {
    super(ppt);
    core = new LowerBoundCore (this);
  }

  protected Object clone() {
    EltLowerBound  result = (EltLowerBound ) super.clone();
    result.core = (LowerBoundCore ) core.clone();
    result.core.wrapper = result;
    return result;
  }

  public static EltLowerBound  instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;
    return new EltLowerBound (ppt);
  }

  public String repr() {
    return "EltLowerBound"  + varNames() + ": "
      + core.repr();
  }

  public String format_using(OutputFormat format) {
    if (format == OutputFormat.DAIKON) {
      return format_daikon();
    } else if (format == OutputFormat.IOA) {
      return format_ioa();
    } else if (format == OutputFormat.SIMPLIFY) {
      return format_simplify();
    } else if (format == OutputFormat.ESCJAVA) {
      return format_esc();  
    }

    return format_unimplemented(format);
  }

  // ELTLOWEr || ELTUPPEr
  public String format_daikon() {
    return var().name.name() + " elements >= " + core.min1 ;
  }

  public String format_esc() {
    String[] form =
      VarInfoName.QuantHelper.format_esc(new VarInfoName[]
	{ var().name });
    return form[0] + "(" + form[1] + " >= " + core.min1  + ")" + form[2];
  }

  public String format_ioa() {
    VarInfoName.QuantHelper.IOAQuantification quant = new VarInfoName.QuantHelper.IOAQuantification (var());
    String result = quant.getQuantifierExp() + quant.getMembershipRestriction(0) +
      " => " + quant.getVarIndexed(0) + " >" +"= " + core.min1  + quant.getClosingExp();
    return result;
  }

  public String format_simplify() {
    String[] form =
      VarInfoName.QuantHelper.format_simplify(new VarInfoName[]
	{ var().name });
    return form[0] + "(>= " + form[1] + " " + core.min1  + ")" + form[2];
  }

  // XXX need to flow invariant if bound changed
  public void add_modified(long[]  value, int count) {
    // System.out.println("EltLowerBound"  + varNames() + ": "
    //                    + "add(" + value + ", " + modified + ", " + count + ")");

    for (int i=0; i<value.length; i++) {
      core.add_modified(value[i], count);
      if (no_invariant)
        return;
    }

  }

  public boolean enoughSamples() {
    return core.enoughSamples();
  }

  protected double computeProbability() {
    return core.computeProbability();
  }

  public boolean isExact() {
    return core.isExact();
  }

  public boolean isSameFormula(Invariant other)
  {
    return core.isSameFormula(((EltLowerBound ) other).core);
  }

  public boolean isObviousImplied() {
    // if the value is not in some range (like -1,0,1,2) then say that it is obvious
    if ((core.min1  < dkconfig_minimal_interesting) ||
	(core.min1  > dkconfig_maximal_interesting)) {
      return true;
    }
    EltOneOf  oo = EltOneOf .find(ppt);
    if ((oo != null) && oo.enoughSamples()) {
      // We could also use core.min1  == oo. min_elt (), since the LowerBound
      // will never have a core.min1  that does not appear in the OneOf.
      if (core.min1  <=  ((Long)oo. min_elt ()).longValue()) {
        return true;
      }
    }

    return super.isObviousImplied();
  }

  public boolean isObviousDerived() {
    VarInfo v = var();
    if (v.isDerived() && (v.derived instanceof SequenceLength)) {
      int vshift = ((SequenceLength) v.derived).shift;
      if (vshift != 0) {
        return true;

      }
    }

    // For each sequence variable, if this is an obvious member/subsequence, and
    // it has the same invariant, then this one is obvious.
    PptTopLevel pptt = (PptTopLevel) ppt.parent;
    for (int i=0; i<pptt.var_infos.length; i++) {
      VarInfo vi = pptt.var_infos[i];

      if (SubSequence.isObviousDerived(v, vi))

      {
        PptSlice1 other_slice = pptt.findSlice(vi);
        if (other_slice != null) {
          EltLowerBound  eb = EltLowerBound .find(other_slice);
          if ((eb != null)
              && eb.enoughSamples()
              && eb. core.min1  == core.min1 ) {
            return true;
          }
        }
      }
    }

    return false;
  }

  public boolean isExclusiveFormula(Invariant other) {
    if (other instanceof EltUpperBound ) {
      if (core.min1  >  ((EltUpperBound ) other). core.max1 )
        return true;
    }
    if (other instanceof OneOfScalar) {
      return other.isExclusiveFormula(this);
    }
    return false;
  }

  // Look up a previously instantiated invariant.
  public static EltLowerBound  find(PptSlice ppt) {
    Assert.assert(ppt.arity == 1);
    for (Iterator itor = ppt.invs.iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if (inv instanceof EltLowerBound )
        return (EltLowerBound ) inv;
    }
    return null;
  }

}

