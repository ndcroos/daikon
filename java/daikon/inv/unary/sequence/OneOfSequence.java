package daikon.inv.unary.sequence;

import daikon.*;
import daikon.inv.*;
import daikon.derive.unary.*;
import daikon.inv.unary.scalar.*;
import daikon.inv.unary.sequence.*;
import daikon.inv.binary.sequenceScalar.*;

import utilMDE.*;

import java.util.*;
import java.io.*;

// *****
// Do not edit this file directly:
// it is automatically generated from OneOf.java.jpp
// *****

// States that the value is one of the specified values.

// This subsumes an "exact" invariant that says the value is always exactly
// a specific value.  Do I want to make that a separate invariant
// nonetheless?  Probably not, as this will simplify implication and such.

public final class OneOfSequence 
  extends SingleSequence 
  implements OneOf
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  public static boolean dkconfig_enabled = true;
  public static int dkconfig_size = 3;

  // Probably needs to keep its own list of the values, and number of each seen.
  // (That depends on the slice; maybe not until the slice is cleared out.
  // But so few values is cheap, so this is quite fine for now and long-term.)

  private long[] [] elts;
  private int num_elts;

  private boolean is_boolean;
  private boolean is_hashcode;

  OneOfSequence (PptSlice ppt) {
    super(ppt);

    elts = new long[dkconfig_size][];    // elements are interned, so can test with ==
                                // (in the general online case, not worth interning)

    num_elts = 0;

    Assert.assert(var().type.isPseudoArray(),
		  "ProglangType must be pseudo-array for EltOneOf or OneOfSequence");
    is_boolean = (var().file_rep_type.elementType() == ProglangType.BOOLEAN);
    is_hashcode = (var().file_rep_type.elementType() == ProglangType.HASHCODE);
    // System.out.println("is_hashcode=" + is_hashcode + " for " + format()
    //                    + "; file_rep_type=" + var().file_rep_type.format());

  }

  public static OneOfSequence  instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;
    return new OneOfSequence (ppt);
  }

  protected Object clone() {
    OneOfSequence  result = (OneOfSequence ) super.clone();
    result.elts = (long[] []) elts.clone();

    for (int i=0; i < num_elts; i++) {
      result.elts[i] = (long[] ) elts[i].clone();
    }

    return result;
  }

  public int num_elts() {
    return num_elts;
  }

  public Object elt() {
    if (num_elts != 1)
      throw new Error("Represents " + num_elts + " elements");

    return elts[0];

  }

  static Comparator comparator = new ArraysMDE.LongArrayComparatorLexical();

  private void sort_rep() {
    Arrays.sort(elts, 0, num_elts , comparator );
  }

  public Object min_elt() {
    if (num_elts == 0)
      throw new Error("Represents no elements");
    sort_rep();

    return elts[0];

  }

  public Object max_elt() {
    if (num_elts == 0)
      throw new Error("Represents no elements");
    sort_rep();

    return elts[num_elts-1];

  }

  // Assumes the other array is already sorted
  public boolean compare_rep(int num_other_elts, long[] [] other_elts) {
    if (num_elts != num_other_elts)
      return false;
    sort_rep();
    for (int i=0; i < num_elts; i++)
      if (elts[i] != other_elts[i]) // elements are interned
        return false;
    return true;
  }

  private String subarray_rep() {
    // Not so efficient an implementation, but simple;
    // and how often will we need to print this anyway?
    sort_rep();
    StringBuffer sb = new StringBuffer();
    sb.append("{ ");
    for (int i=0; i<num_elts; i++) {
      if (i != 0)
        sb.append(", ");
      sb.append(ArraysMDE.toString( elts[i] ) );
    }
    sb.append(" }");
    return sb.toString();
  }

  public String repr() {
    return "OneOfSequence"  + varNames() + ": "
      + "no_invariant=" + no_invariant
      + ", num_elts=" + num_elts
      + ", elts=" + subarray_rep();
  }

  private boolean all_nulls(int value_no) {
    long[]  seq = elts[value_no];
    for (int i=0; i<seq.length; i++) {
      if (seq[i] != 0) return false;
    }
    return true;
  }
  private boolean no_nulls(int value_no) {
    long[]  seq = elts[value_no];
    for (int i=0; i<seq.length; i++) {
      if (seq[i] == 0) return false;
    }
    return true;
  }

  public String format() {
    String varname = var().name.name() ;
    if (num_elts == 1) {

      if (is_hashcode) {
	// we only have one value, because add_modified dies if more
	long[]  value = elts[0];
        if (value.length == 0) {
          return varname + " == []";
        } else if ((value.length == 1) && (value[0] == 0)) {
          return varname + " == [null]";
        } else if (no_nulls(0)) {
          return varname + " contains no nulls and has only one value, of length " + value.length;
        } else if (all_nulls(0)) {
          return varname + " contains only nulls and has only one value, of length " + value.length;
	} else {
          return varname + " has only one value, of length " + value.length;
        }
      } else {
        return varname + " == " + ArraysMDE.toString( elts[0] ) ;
      }

    } else {
      return varname + " one of " + subarray_rep();
    }
  }

    public String format_java() {
       StringBuffer sb = new StringBuffer();
       for (int i = 0; i < num_elts; i++) {
	 sb.append (" || (" + var().name.java_name()  + " == " +  ArraysMDE.toString( elts[i] )   );
	 sb.append (")");
       }
       // trim off the && at the beginning for the first case
       return sb.toString().substring (4);
    }

  /* IOA */
  public String format_ioa() {

    String result;

    String length = "";
    String forall = "";
    if (is_hashcode) {
      // we only have one value, because add_modified dies if more
      long[]  value = elts[0];
      if (var().name.isApplySizeSafe()) {
	length = "size("+var().name.ioa_name() + ") = " + value.length;
      }
      if (no_nulls(0)) {
	String[] form = VarInfoName.QuantHelper.format_ioa(new VarInfo[] { var() });
	forall = form[0] + form[1] + " ~= null ***" + form[2];
      } else if (all_nulls(0)) {
	String[] form = VarInfoName.QuantHelper.format_ioa(new VarInfo[] { var() });
	forall = form[0] + form[1] + " = null ***" + form[2];
      }
    }
    if (length == "" && forall == "") { // interned
      String thisclassname = this.getClass().getName();
      result = "warning: " + thisclassname + ".format_ioa()  needs to be implemented: " + format();
    } else if (length == "") { // interned
      result = forall;
    } else if ((forall == "")||(elts[0].length==0)) { // interned
      result = length;
    } else {
      result = "(" + length + ") /\\ (" + forall + ")";
    }

    return result;
  }

  public String format_esc() {

    String result;

    String length = "";
    String forall = "";
    if (is_hashcode) {
      // we only have one value, because add_modified dies if more
      long[]  value = elts[0];
      if (var().name.isApplySizeSafe()) {
	length = var().name.applySize().esc_name() + " == " + value.length;
      }
      if (no_nulls(0)) {
	String[] form = VarInfoName.QuantHelper.format_esc(new VarInfoName[] { var().name } );
	forall = form[0] + "(" + form[1] + " != null)" + form[2];
      } else if (all_nulls(0)) {
	String[] form = VarInfoName.QuantHelper.format_esc(new VarInfoName[] { var().name } );
	forall = form[0] + "(" + form[1] + " == null)" + form[2];
      }
    }
    if (length == "" && forall == "") { // interned
      String classname = this.getClass().toString().substring(6); // remove leading "class"
      result = "warning: method " + classname + ".format_esc() needs to be implemented: " + format();
    } else if (length == "") { // interned
      result = forall;
    } else if (forall == "") { // interned
      result = length;
    } else {
      result = "(" + length + ") && (" + forall + ")";
    }

    return result;
  }

  public String format_simplify() {

    String result;

    String length = "";
    String forall = "";
    if (is_hashcode) {
      // we only have one value, because add_modified dies if more
      long[]  value = elts[0];
      if (var().name.isApplySizeSafe()) {
	length = "(EQ " + var().name.applySize().simplify_name() + " " + value.length + ")";
      }
      if (no_nulls(0)) {
	String[] form = VarInfoName.QuantHelper.format_simplify(new VarInfoName[] { var().name } );
	forall = form[0] + "(NEQ " + form[1] + "  null)" + form[2];
      } else if (all_nulls(0)) {
	String[] form = VarInfoName.QuantHelper.format_simplify(new VarInfoName[] { var().name } );
	forall = form[0] + "(EQ " + form[1] + "  null)" + form[2];
      }
    }
    if (length == "" && forall == "") { // interned
      String classname = this.getClass().toString().substring(6); // remove leading "class"
      result = "warning: method " + classname + ".format_simplify() needs to be implemented: " + format();
    } else if (length == "") { // interned
      result = forall;
    } else if (forall == "") { // interned
      result = length;
    } else {
      result = "(AND " + length + " " + forall + ")";
    }

    return result;
  }

  public void add_modified(long[]  v, int count) {

    Assert.assert(Intern.isInterned(v));

    for (int i=0; i<num_elts; i++)
      if (elts[i] == v) {

        return;

      }
    if (num_elts == dkconfig_size) {
      flowThis();
      destroy();
      return;
    }

    if (is_hashcode && (num_elts == 1)) {
      flowThis();
      destroy();
      return;
    }

    if (num_elts > 0) {
      // We are significantly changing our state (not just zeroing in on
      // a constant), so we have to flow a copy before we do so.
      flowClone();
    }

    elts[num_elts] = v;
    num_elts++;

  }

  protected double computeProbability() {
    // This is not ideal.
    if (num_elts == 0) {
      return Invariant.PROBABILITY_UNJUSTIFIED;

    } else {
      return Invariant.PROBABILITY_JUSTIFIED;
    }
  }

  public boolean isSameFormula(Invariant o)
  {
    OneOfSequence  other = (OneOfSequence ) o;
    if (num_elts != other.num_elts)
      return false;

    sort_rep();
    other.sort_rep();
    for (int i=0; i < num_elts; i++)
      if (elts[i] != other.elts[i]) // elements are interned
	return false;

    return true;
  }

  public boolean isExclusiveFormula(Invariant o)
  {
    if (o instanceof OneOfSequence ) {
      OneOfSequence  other = (OneOfSequence ) o;

      for (int i=0; i < num_elts; i++) {
        for (int j=0; j < other.num_elts; j++) {
          if (elts[i] == other.elts[j]) // elements are interned
            return false;
        }
      }
      return true;
    }

    return false;
  }

  // OneOf invariants that indicate a small set of possible values are
  // uninteresting.  OneOf invariants that indicate exactly one value
  // are interesting.
  public boolean isInteresting() {
    if (num_elts() > 1) {
      return false;
    } else {
      return true;
    }
  }

  // Look up a previously instantiated invariant.
  public static OneOfSequence  find(PptSlice ppt) {
    Assert.assert(ppt.arity == 1);
    for (Iterator itor = ppt.invs.iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if (inv instanceof OneOfSequence )
        return (OneOfSequence ) inv;
    }
    return null;
  }

  // Interning is lost when an object is serialized and deserialized.
  // Manually re-intern any interned fields upon deserialization.
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    for (int i=0; i < num_elts; i++)
      elts[i] = Intern.intern(elts[i]);
  }

}

