package daikon.inv.unary.sequence;

import daikon.*;
import daikon.inv.*;
import daikon.inv.binary.twoSequence.*;

import utilMDE.*;

import java.util.*;


public class CommonSequence
  extends SingleSequence
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff CommonSequence invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  private int elts;
  private long[] intersect = null;

  protected CommonSequence(PptSlice ppt) {
    super(ppt);
  }

  public static CommonSequence instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;
    return new CommonSequence(ppt);
  }

  // this.intersect is read-only, so don't clone it
  // public Object clone();

  public String repr() {
    return "CommonSequence " + varNames() + ": "
      + "elts=\"" + elts;
  }

  private String printIntersect() {
    if (intersect==null)
      return "{}";

    String result = "{";
    for (int i=0; i<intersect.length; i++) {
      result += intersect[i];
      if (i!=intersect.length-1)
	result += ", ";
    }
    result += "}";
    return result;
  }

  public String format_using(OutputFormat format) {
    if (format == OutputFormat.DAIKON) return format_daikon();
    if (format == OutputFormat.IOA) return format_ioa();

    return format_unimplemented(format);
  }

  public String format_daikon() {
    return (printIntersect() + " subset of " + var().name);
  }

  /* IOA */
  public String format_ioa() {
    String vname = var().name.ioa_name();
    if (var().isIOASet())
      return printIntersect() + " \\in " + vname;
    else
      return "(" + printIntersect() + " \\in " + vname + ") ***";
  }

  public void add_modified(long[] a, int count) {
    if (intersect==null)
      intersect = a;
    else {
      long[] tmp = new long[intersect.length];
      int    size = 0;
      for (int i=1; i<a.length; i++)
	if ((ArraysMDE.indexOf(intersect, a[i])!=-1) &&
	    ((size==0) ||
	     (ArraysMDE.indexOf(ArraysMDE.subarray(tmp,0,size), a[i])==-1)))
	  tmp[size++] = a[i];

      if (size==0) {
	flowThis();
	destroy();
	return;
      }

      intersect = ArraysMDE.subarray(tmp, 0, size);
    }

    intersect = (long []) Intern.intern(intersect);
    elts++;
  }

  protected double computeProbability() {
    if (no_invariant) {
      return Invariant.PROBABILITY_NEVER;
    } else {
      return Math.pow(.9, elts);
    }
  }

  public boolean isObviousImplied() {
    return false;
  }

  public boolean isSameFormula(Invariant other) {
    Assert.assert(other instanceof CommonSequence);
    return true;
  }
}
