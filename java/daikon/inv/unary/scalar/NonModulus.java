package daikon.inv.unary.scalar;

import daikon.*;
import daikon.inv.*;
import utilMDE.*;
import java.util.*;

public class NonModulus
  extends SingleScalar
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff NonModulus invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  // Set elements = new HashSet();
  SortedSet elements = new TreeSet();

  private long modulus = 0;
  private long remainder = 0;
  // The next two variables indicate whether the "modulus" and "result"
  // fields are up to date.
  // Indicates that no nonmodulus has been found; maybe with more
  // samples, one will appear.
  private boolean no_result_yet = false;
  // We don't continuously keep the modulus and remainder field up to date.
  // This indicates whether it is.
  private boolean results_accurate = false;

  private NonModulus(PptSlice ppt) {
    super(ppt);
  }

  public static NonModulus instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;
    return new NonModulus(ppt);
  }

  protected Object clone() {
    NonModulus result = (NonModulus) super.clone();
    result.elements = new TreeSet(this.elements);
    return result;
  }

  public String repr() {
    return "NonModulus" + varNames() + ": "
      + "m=" + modulus + ",r=" + remainder;
  }

  public String format_using(OutputFormat format) {
    updateResults();
    String name = var().name.name_using(format);

    if (no_result_yet) {
      return name + " != ? (mod ?) ***";
    }

    if (format == OutputFormat.DAIKON) {
      return name + " != " + remainder + "  (mod " + modulus + ")";
    }

    if (format == OutputFormat.IOA) {
      return "mod("+name+", "+modulus+") ~= "+remainder;
    }

    return format_unimplemented(format);
  }

  // Set either modulus and remainder, or no_result_yet.
  void updateResults() {
    if (results_accurate)
      return;
    if (elements.size() == 0) {
      no_result_yet = true;
    } else {
      // Do I want to communicate back some information about the smallest
      // possible modulus?
      long[] result = MathMDE.nonmodulus_strict_long(elements.iterator());
      if (result == null) {
	no_result_yet = true;
      } else {
	remainder = result[0];
	modulus = result[1];
        no_result_yet = false;
      }
    }
    results_accurate = true;
  }

  // XXX have to deal with flowing this; maybe it should live at all ppts?
  public void add_modified(long value, int count) {
    if (elements.add(Intern.internedLong(value))
	&& results_accurate
	&& (! no_result_yet)
	&& (MathMDE.mod_positive(value, modulus) == remainder))
      results_accurate = false;
  }

  protected double computeProbability() {
    updateResults();
    if (no_result_yet)
      return Invariant.PROBABILITY_UNJUSTIFIED;
    double probability_one_elt_nonmodulus = 1 - 1.0/modulus;
    return Math.pow(probability_one_elt_nonmodulus, ppt.num_mod_non_missing_samples());
  }

  public boolean isSameFormula(Invariant o)
  {
    NonModulus other = (NonModulus) o;

    updateResults();
    other.updateResults();

    if (no_result_yet && other.no_result_yet) {
      return true;
    } else if (no_result_yet || other.no_result_yet) {
      return false;
    } else {
      return
        (modulus == other.modulus) &&
        (remainder == other.remainder);
    }
  }

  public boolean hasModulusRemainder(long modulus, long remainder) {
    updateResults();
    if (no_result_yet)
      return false;

    return ((modulus == this.modulus)
            && (remainder == this.remainder));
  }


  public boolean isExclusiveFormula(Invariant o) {
    updateResults();
    if (no_result_yet)
      return false;
    if (o instanceof NonModulus) {
      NonModulus other = (NonModulus) o;
      other.updateResults();
      if (other.no_result_yet)
        return false;
      return ((modulus == other.modulus)
              && (remainder != other.remainder));
    } else if (o instanceof Modulus) {
      Modulus other = (Modulus) o;
      return ((modulus == other.modulus)
              && (remainder == other.remainder));
    }

    return false;
  }

}
