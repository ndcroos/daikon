package daikon.inv.unary.sequence;

import daikon.*;
import daikon.derive.unary.*;
import daikon.inv.binary.twoScalar.*;
import daikon.inv.binary.twoSequence.*;
import daikon.inv.*;
import java.util.*;
import utilMDE.*;

public final class SeqIndexNonEqual
  extends SingleSequence
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  public static boolean dkconfig_enabled = true;

  public NonEqualCore core;

  static boolean debugSeqIndexNonEqual = false;

  protected SeqIndexNonEqual(PptSlice ppt) {
    super(ppt);

    VarInfo var = var();
    Assert.assert(var.rep_type == ProglangType.INT_ARRAY);
    Assert.assert(var.type.elementIsIntegral());
    core = new NonEqualCore(this, 0);

    if (debugSeqIndexNonEqual) {
      System.out.println("Instantiated: " + format());
    }
  }

  public static SeqIndexNonEqual instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;

    VarInfo seqvar = ppt.var_infos[0];

    if (debugSeqIndexNonEqual) {
      System.out.println("SeqIndexNonEqual instantiated: " + seqvar.name);
    }

    // Don't compare indices to object addresses.
    ProglangType elt_type = seqvar.type.elementType();
    if (! elt_type.baseIsIntegral())
      return null;

    return new SeqIndexNonEqual(ppt);
  }

  protected Object clone() {
    SeqIndexNonEqual result = (SeqIndexNonEqual) super.clone();
    result.core = (NonEqualCore) core.clone();
    result.core.wrapper = result;
    return result;
  }

  public String repr() {
    return "SeqIndexNonEqual" + varNames() + ": "
      + core.repr()
      + ",no_invariant=" + no_invariant;
  }

  public String format() {
    // this is wrong because "a[k..] != i" doesn't need the subscript
    // return var().name.applySubscript(VarInfoName.parse("i")).name() + " != i";
    VarInfoName name = var().name;
    if ((new VarInfoName.ElementsFinder(name)).elems() != null) {
      return name.applySubscript(VarInfoName.parse("i")).name() + " != i";
    } else {
      return name.name() + " != (index)";
    }
  }

  public String format_esc() {
    String[] form =
      VarInfoName.QuantHelper.format_esc(new VarInfoName[]
	{ var().name });
    return form[0] + "(" + form[1] + " != i)" + form[2];
  }

  /* IOA */
  public String format_ioa() {
    if (var().isIOASet())
      return "Not valid for sets: " + format();
    String[] form =
      VarInfoName.QuantHelper.format_ioa(new VarInfo[] { var() });
    return form[0] + form[1] + " ~= " + form[3] + form[2];
  }
  public String format_simplify() {
    String[] form =
      VarInfoName.QuantHelper.format_simplify(new VarInfoName[]
	{ var().name });
    return form[0] + "(NEQ " + form[1] + " |i|)" + form[2];
  }

  public void add_modified(long [] a, int count) {
    for (int i=0; i<a.length; i++) {
      core.add_modified(a[i], i, count);
      if (no_invariant)
        return;
    }
  }

  protected double computeProbability() {
    return core.computeProbability();
  }

  public boolean isExact() {
    return core.isExact();
  }

  public boolean isSameFormula(Invariant other)
  {
    return core.isSameFormula(((SeqIndexNonEqual) other).core);
  }

  public boolean isExclusiveFormula(Invariant other)
  {
    if (other instanceof SeqIndexComparison) {
      if (((SeqIndexComparison)other).isExact()) {
        return true;
      }
    }
    return false;
  }

  // Look up a previously instantiated invariant.
  public static SeqIndexNonEqual find(PptSlice ppt) {
    Assert.assert(ppt.arity == 1);
    for (Iterator itor = ppt.invs.iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if (inv instanceof SeqIndexNonEqual)
        return (SeqIndexNonEqual) inv;
    }
    return null;
  }



  // Copied from IntComparison.
  // public boolean isExclusiveFormula(Invariant other)
  // {
  //   if (other instanceof IntComparison) {
  //     return core.isExclusiveFormula(((IntComparison) other).core);
  //   }
  //   if (other instanceof NonEqual) {
  //     return isExact();
  //   }
  //   return false;
  // }


  // public boolean isObviousImplied() {
  //   return isEqualToObviousSeqIndexNonEqual(sclvar(), seqvar());
  // }


  // Copied from IntComparison.
  public boolean isObviousImplied() {
    VarInfo seqvar = var();

    // For each other sequence variable, if it is a supersequence of this
    // one and it has the same invariant, then this one is obvious.
    PptTopLevel pptt = (PptTopLevel) ppt.parent;
    for (int i=0; i<pptt.var_infos.length; i++) {
      VarInfo vi = pptt.var_infos[i];
      if (SubSequence.isObviousDerived(seqvar, vi)) {
        PptSlice1 other_slice = pptt.findSlice(vi);
        // I'm not sure exactly how this can be null, but it can.
        if (other_slice != null) {
          SeqIndexNonEqual other_sine = SeqIndexNonEqual.find(other_slice);
          if ((other_sine != null) && other_sine.enoughSamples()) {
            return true;
          }
        }
      }
    }

    return false;
  }

}
