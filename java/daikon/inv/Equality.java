package daikon.inv;

import daikon.*;
import utilMDE.*;
import java.util.*;

/**
 * The Equality invariant is used for displaying several equality
 * Comparison invariants ("x == y", "x == z") as one Equality
 * invariant ("x == y == z").  This class is created after the actual
 * invariant detection, and right before printing (eg, during the GUI
 * stage).  Hence this is not a real invariant class; it does not
 * implement many of the methods that most invariant classes do.
 * Furthermore, calling arbitrary methods on this class may not work.
 **/
public final class Equality extends Invariant {

  private VarInfo[] vars;		

  /**
   * @param vars Variables which are equivalent, with the canonical
   * one first.  Elements must be of type VarInfo.
   **/
  public Equality(Collection variables, PptSlice ppt) {
    super(ppt);    
    vars = (VarInfo[]) variables.toArray(new VarInfo[variables.size()]);
    Assert.assert(vars.length >= 2);
    for (int i=0; i < vars.length; i++) {
      Assert.assert(vars[0].ppt == vars[i].ppt);
      Assert.assert(vars[0].rep_type.isArray() == vars[i].rep_type.isArray());
    }
  }

  public boolean hasNonCanonicalVariable() {
    // In fact, we do have non-canonical variables, but it's our
    // little secret.
    return false;
  }

  //  Here is my rationale for always returning 0.  This Equality invariant aggregates
  //  several Comparison invariants who passed the IsEqualityComparison.it.accept() test.
  //  IsEqualityComparison.it.accept() checks if getProbability() returns less than
  //  Invariant.probability_limit, which is currently .01.  That means the probability for
  //  any of the involved Comparison invariants is at most .01.  In practice, the
  //  probability is always almost 0, and .01 is close to 0, so just report 0.

  public double computeProbability() { return 0; }

  public String repr() {
    return format();
  }

  public String format() {
    StringBuffer result = new StringBuffer(vars[0].name.name());
    for (int i=1; i < vars.length; i++) {
      result.append(" == ");
      result.append(vars[i].name.name());
    }
    return result.toString();
  }

  public String format_esc() {
    StringBuffer result = new StringBuffer();
    if (vars[0].rep_type.isArray()) {
      for (int i=1; i < vars.length; i++) {
	if (i > 1) {
	  result.append(Global.lineSep + "&& ");
	}
	String[] form =
	  VarInfoName.QuantHelper.format_esc(new VarInfoName[]
	    { vars[0].name, vars[i].name }, true); // elementwise
	result.append(form[0] + "( " + form[1] + " == " + form[2] + " )" + form[3]);
      }
    } else {
      for (int i=1; i < vars.length; i++) {
	if (i > 1) {
	  result.append(" && ");
	}
	result.append("(");
	result.append(vars[0].name.esc_name());
	result.append(" == ");
	result.append(vars[i].name.esc_name());
	result.append(")");
      }
    }
    return result.toString();
  }

  public String format_simplify() {
    StringBuffer result = new StringBuffer("(AND");
    if (vars[0].rep_type.isArray()) {
      for (int i=1; i < vars.length; i++) {
	String[] form =
	  VarInfoName.QuantHelper.format_simplify(new VarInfoName[]
	    { vars[0].name, vars[i].name }, true); // elementwise
	result.append(" " + form[0] + "(EQ " + form[1] + " " + form[2] + ")" + form[3]);
      }
    } else {
      for (int i=1; i < vars.length; i++) {
	result.append(" (EQ ");
	result.append(vars[0].name.simplify_name());
	result.append(" ");
	result.append(vars[i].name.simplify_name());
	result.append(")");
      }
    }
    result.append(")");
    return result.toString();
  }

  //  This method isn't going to be called, but it's declared abstract in Invariant.
  public boolean isSameFormula( Invariant other ) {
    throw new UnsupportedOperationException( "Equality.isSameFormula(): this method should not be called" );
  }
}












