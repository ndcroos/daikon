package daikon;

import java.io.*;
import java.util.*;
import utilMDE.*;

/**
 * A collection of code that configures and assists the dataflow
 * system in Daikon.
 **/
public class Dataflow
{

  // Tempoary routine, for debugging
  // Will eventually move into daikon.test.FileIOTest
  public static void main(String[] args)
    throws Exception
  {
    String outf = "fileio.txt";
    if (args.length == 0) {
      args = new String[] { "daikon/test/fileIOTest.testStackAr" };
    }
    File[] files = new File[args.length];
    for (int i=0; i < args.length; i++) {
      files[i] = new File(args[i]);
    }
    PptMap map = FileIO.read_declaration_files(Arrays.asList(files));
    init(map);
    dump_ppts(new java.io.FileOutputStream(new File(outf)), map);
  }


  /**
   * @param files files to be read (java.io.File)
   * @return a new PptMap containing declarations read from the files
   * listed in the argument; connection information (controlling
   * variables and entry ppts) is set correctly upon return.
   **/
  public static void init(PptMap all_ppts)
  {
    // Set up EXIT points to control EXITnn points
    create_and_relate_combined_exits(all_ppts);
    // Set up OBJECT and CLASS relationships
    relate_object_procedure_ppts(all_ppts);
    // Set up orig() declarations and relationships
    create_and_relate_orig_vars(all_ppts);
    // Set up OBJECT on arguments relationships
    relate_arguments_to_object_ppts(all_ppts);
    // Set up derived variables
    create_derived_variables(all_ppts);
    // Set up the data flow vectors
    create_ppt_dataflow(all_ppts);
  }

  /**
   * For every variable that has the same name in higher and lower,
   * add a link in the po relating them.  See the definitions of lower
   * and higher in VarInfo for their semantics.
   * @see VarInfo.po_higher
   * @see VarInfo.po_lower
   **/
  private static void setup_po_same_name(VarInfo[] lower,
					 VarInfo[] higher)
  {
    setup_po_same_name(lower, VarInfoName.IDENTITY_TRANSFORMER,
		       higher, VarInfoName.IDENTITY_TRANSFORMER);
  }

  /**
   * For every variable that has the same name in higher and lower (after transformation),
   * add a link in the po relating them.  See the definitions of lower
   * and higher in VarInfo for their semantics.
   * @see VarInfo.po_higher
   * @see VarInfo.po_lower
   **/
  private static void setup_po_same_name(VarInfo[] lower,
					 VarInfoName.Transformer lower_xform,
					 VarInfo[] higher,
					 VarInfoName.Transformer higher_xform)
  {
    for (int i=0; i<higher.length; i++) {
      VarInfo higher_vi = higher[i];
      VarInfoName higher_vi_name = higher_xform.transform(higher_vi.name);
      for (int j=0; j<lower.length; j++) {
	VarInfo lower_vi = lower[j];
	VarInfoName lower_vi_name = lower_xform.transform(lower_vi.name);
	if (higher_vi_name == lower_vi_name) { // VarInfoNames are interned
	  lower_vi.addHigherPO(higher_vi, static_po_group_nonce);
	}
      }
    }
    static_po_group_nonce++;
  }

  // Used by setup_po_same name and related to group variables that flow together
  private static int static_po_group_nonce = 0;

  /**
   * For each fooENTER point, add a fooEXIT point that contains all of
   * the common variables from the fooEXITnn points.  It is an error
   * if one of the fooEXIT points already exists.
   **/
  public static void create_and_relate_combined_exits(PptMap ppts)
  {
    List new_ppts = new ArrayList(); // worklist to avoid concurrent mod exn
    for (Iterator itor = ppts.iterator(); itor.hasNext(); ) {
      PptTopLevel enter_ppt = (PptTopLevel) itor.next();
      if (!enter_ppt.ppt_name.isEnterPoint())
	continue;
      // Construct the combined EXIT name
      PptName exit_name = enter_ppt.ppt_name.makeExit();
      Assert.assert(ppts.get(exit_name) == null);
      // Find all of the EXITnn points
      List exits = new ArrayList();
      for (Iterator it = ppts.iterator(); it.hasNext(); ) {
	PptTopLevel exitnn = (PptTopLevel) it.next();
	if (exitnn.ppt_name.isExitPoint()) {
	  Assert.assert(!exitnn.ppt_name.isCombinedExitPoint());
	  if (exit_name.equals(exitnn.ppt_name.makeExit())) {
	    exits.add(exitnn);
	  }
	}
      }
      // Find common vars in EXITnn points, and make a new ppt from them
      VarInfo[] exit_vars = VarInfo.arrayclone_simple(Ppt.common_vars(exits));
      PptTopLevel exit_ppt = new PptTopLevel(exit_name.getName(), exit_vars);
      // Set up the PO between EXIT and EXITnn
      for (Iterator it = exits.iterator(); it.hasNext(); ) {
	PptTopLevel exitnn = (PptTopLevel) it.next();
	setup_po_same_name(exitnn.var_infos, exit_ppt.var_infos);
      }
      // Put new ppt on worklist to be added once iteration is complete
      new_ppts.add(exit_ppt);
    }
    // Avoid ConcurrentModificationException by adding after the above loop
    for (int i=0; i<new_ppts.size(); i++) {
      ppts.add((PptTopLevel) new_ppts.get(i));
    }
  }

  /**
   * Set up the "controlling program point" partial ordering on
   * VarInfos using the OBJECT, CLASS, ENTER-EXIT program point
   * naming.
   **/
  private static void relate_object_procedure_ppts(PptMap ppts)
  {
    for (Iterator itor = ppts.iterator() ; itor.hasNext() ; ) {
      PptTopLevel ppt = (PptTopLevel) itor.next();
      PptName ppt_name = ppt.ppt_name;
      // Find the ppt which controlls this one.
      // CLASS controls OBJECT controls ENTER/EXIT.
      PptTopLevel controlling_ppt = null;
      if (ppt_name.isEnterPoint() || ppt_name.isCombinedExitPoint()) {
	// TODO: also require that this is a public method (?)
	controlling_ppt = ppts.get(ppt_name.makeObject());
	if (controlling_ppt == null) {
	  // If we didn't find :::OBJECT, fall back to :::CLASS
	  controlling_ppt = ppts.get(ppt_name.makeClassStatic());
	}
      } else if (ppt_name.isObjectInstanceSynthetic()) {
	controlling_ppt = ppts.get(ppt_name.makeClassStatic());
      }
      // Create VarInfo relations with the controller when names match.
      if (controlling_ppt != null) {
	setup_po_same_name(ppt.var_infos, controlling_ppt.var_infos);
      }
    }
  }

  /**
   * Add orig() variables to all EXIT or EXITnn program points.
   * Should be performed after combined exits and controlling ppts
   * have been added.
   **/
  private static void create_and_relate_orig_vars(PptMap ppts)
  {
    for (Iterator i = ppts.iterator() ; i.hasNext() ; ) {
      PptTopLevel entry_ppt = (PptTopLevel) i.next();
      if (! entry_ppt.ppt_name.isEnterPoint()) {
	continue;
      }
      for (Iterator j = ppts.iterator() ; j.hasNext() ; ) {
	PptTopLevel exit_ppt = (PptTopLevel) j.next();
	if (! exit_ppt.ppt_name.isExitPoint()) {
	  continue;
	}
	if (! entry_ppt.ppt_name.equals(exit_ppt.ppt_name.makeEnter())) {
	  continue;
	}
	// comb_exit_ppt may be same as exit_ppt if exit_ppt is EXIT
	PptTopLevel comb_exit_ppt = ppts.get(exit_ppt.ppt_name.makeExit());
	// Add "orig(...)" (prestate) variables to the program point.
	// Don't bother to include the constants.  Walk through
	// entry_ppt's vars.  For each non-constant, put it on the
	// new_vis worklist after fixing its comparability information.
	exit_ppt.num_orig_vars = entry_ppt.var_infos.length - entry_ppt.num_static_constant_vars;
	VarInfo[] new_vis = new VarInfo[exit_ppt.num_orig_vars];
	{
	  VarInfo[] begin_vis = entry_ppt.var_infos;
	  Assert.assert(exit_ppt.num_orig_vars == entry_ppt.num_tracevars);
	  int new_vis_index = 0;
	  for (int k = 0; k < begin_vis.length; k++) {
	    VarInfo vi = begin_vis[k];
	    Assert.assert(!vi.isDerived(),"Derived when making orig(): "+vi.name);
	    if (vi.isStaticConstant())
	      continue;
	    VarInfo origvar = VarInfo.origVarInfo(vi);
	    // Fix comparability
	    VarInfo postvar = exit_ppt.findVar(vi.name);
	    Assert.assert(postvar != null,"Exit not superset of entry: "+vi.name);
	    origvar.comparability = postvar.comparability.makeAlias(origvar.name);
	    // Setup PO
	    if (exit_ppt.ppt_name.isCombinedExitPoint()) {
	      origvar.addHigherPO(vi, static_po_group_nonce);
	    } else {
	      // (We depend on the fact that we see EXIT before EXITnn
	      // in the iterator (eep!).)
	      VarInfo combvar = comb_exit_ppt.findVar(origvar.name);
	      origvar.addHigherPO(combvar, static_po_group_nonce);
	    }
	    // Add to new_vis
	    new_vis[new_vis_index] = origvar;
	    new_vis_index++;
	  }
	  Assert.assert(new_vis_index == exit_ppt.num_orig_vars);
	  static_po_group_nonce++; // advance once per (EXIT#) program point
	}
	exit_ppt.addVarInfos(new_vis);
      }
    }
  }

  /**
   * For all ENTER point arguments that have a type that we have an
   * OBJECT ppt for, add the appropriate partial order relation.  This
   * must be done after OBJECT-ENTER controlling relations are already
   * set up.
   * (XXX: Should we also set this up for post-state arguments in EXIT points?)
   **/
  private static void relate_arguments_to_object_ppts(PptMap ppts)
  {
    for (Iterator itor = ppts.iterator() ; itor.hasNext() ; ) {
      PptTopLevel entry_ppt = (PptTopLevel) itor.next();
      if (! entry_ppt.ppt_name.isEnterPoint()) {
	continue;
      }
      // All derived expressions from arguments
      List args = new ArrayList(); // [VarInfo]
      // Subset of above which we have an OBJECT ppt for
      Map known = new HashMap(); // [VarInfo -> PptTopLevel]
      // Search and fill these lists
      VarInfo[] vis = entry_ppt.var_infos;
      for (int i=0; i<vis.length; i++) {
	VarInfo vi = vis[i];
	// Arguments are the things with no controller yet
	if (vi.po_higher().size() == 0) {
	  args.add(vi);
	  if (! vi.type.isPseudoArray()) {
	    PptName objname = new PptName(vi.type.base(), // class
					  null, // method
					  FileIO.object_suffix // point
					  );
	    Assert.assert(objname.isObjectInstanceSynthetic());
	    PptTopLevel object_ppt = ppts.get(objname);
	    if (object_ppt != null) {
	      known.put(vi, object_ppt);
	    }
	  }
	}
      }
      // For each known-type variable, substitute its name in for
      // 'this' in the OBJECT ppt and see if we get any expression
      // matches with other "argument" variables.
      VarInfo[] args_array = (VarInfo[]) args.toArray(new VarInfo[args.size()]);
      for (Iterator it = known.keySet().iterator(); it.hasNext(); ) {
	final VarInfo known_vi = (VarInfo) it.next();
	PptTopLevel object_ppt = (PptTopLevel) known.get(known_vi);
	setup_po_same_name(args_array, // lower
			   VarInfoName.IDENTITY_TRANSFORMER,
			   object_ppt.var_infos, // higher
			   // but with known_vi.name in for "this"
			   new VarInfoName.Transformer() {
			       public VarInfoName transform(VarInfoName v) {
				 return v.replaceAll(VarInfoName.parse("this"),
						     known_vi.name);
			       }
			     }
			   );
      }
    }
  }

  /**
   * Create all of the derived variables for all program points.
   **/
  private static void create_derived_variables(PptMap all_ppts) {
    for (Iterator i = all_ppts.iterator(); i.hasNext(); ) {
      PptTopLevel ppt = (PptTopLevel) i.next();
      ppt.create_derived_variables();
    }
  }

  /**
   * Create the dataflow injection vectors in all of the PptTopLevels.
   * Must be done after VarInfo partial ordering relations have been
   * fully set up.
   **/
  private static void create_ppt_dataflow(PptMap all_ppts)
  {
    // For all points that receive samples (currently just EXITnn)
    for (Iterator h = all_ppts.iterator(); h.hasNext(); ) {
      PptTopLevel receiving_ppt = (PptTopLevel) h.next();
      boolean receives_samples = receiving_ppt.ppt_name.isExitPoint()
	&& !receiving_ppt.ppt_name.isCombinedExitPoint();
      if (!receives_samples) {
	continue;
      }

      // These two lists collect result that will be written to receiving_ppt
      List dataflow_ppts = new ArrayList(); // of type PptTopLevel
      List dataflow_transforms = new ArrayList(); // of type int[nvis]
      int nvis = receiving_ppt.var_infos.length;

      // Use our own record type for programming ease
      class VarAndSource {
	public VarInfo var; // variable at the head of a search path
	public int source;  // its source index in receiving_ppt.var_infos
	public VarAndSource(VarInfo _var, int _source) {
	  var = _var;
	  source = _source;
	}
      }

      // Our worklist is the heads of paths flowing up from "ppt".
      // We store the vars which specify the head and the original
      // indices in receiving_ppt that flow up to the head.
      LinkedList worklist = new LinkedList(); // element type is List[VarAndSource]
      {
	// First element in worklist is receiving_ppt
	List first = new ArrayList(nvis);
	for (int i = 0; i < nvis; i++) {
	  first.add(new VarAndSource(receiving_ppt.var_infos[i], i));
	}
	worklist.add(first);
      }

      // While worklist is non-empty, process the first element
      while (! worklist.isEmpty()) {
	List head = (List) worklist.removeFirst();

	// Add a flow from receiving_ppt to head
	PptTopLevel flow_ppt = ((VarAndSource) head.get(0)).var.ppt;
	int[] flow_transform = new int[nvis];
	Arrays.fill(flow_transform, -1);
	for (Iterator i = head.iterator(); i.hasNext(); ) {
	  VarAndSource vs = (VarAndSource) i.next();
	  Assert.assert(vs.var.ppt == flow_ppt); // all flow to the same ppt
	  Assert.assert(flow_transform[vs.source] == -1); // with no overlap
	  flow_transform[vs.source] = vs.var.varinfo_index;
	}
	dataflow_ppts.add(flow_ppt);
	dataflow_transforms.add(flow_transform);

	// Extend head using all higher nonces
	Map nonce_to_vars = new HashMap(); // [Integer -> List[VarAndSource]]
	for (Iterator i = head.iterator(); i.hasNext(); ) {
	  VarAndSource vs = (VarAndSource) i.next();
	  Collection higher_vis = vs.var.po_higher();
	  int[] higher_nonces = vs.var.po_higher_nonce();
	  int nonce_idx = 0;
	  for (Iterator j = higher_vis.iterator(); j.hasNext(); ) {
	    VarInfo higher_vi = (VarInfo) j.next();
	    Integer higher_nonce = new Integer(higher_nonces[nonce_idx++]);
	    List newpath = (List) nonce_to_vars.get(higher_nonce);
	    if (newpath == null) {
	      newpath = new ArrayList();
	      nonce_to_vars.put(higher_nonce, newpath);
	    }
	    newpath.add(new VarAndSource(higher_vi, vs.source));
	  }
	  Assert.assert(nonce_idx == higher_vis.size());
	}
	for (Iterator i = nonce_to_vars.keySet().iterator(); i.hasNext(); ) {
	  Integer nonce = (Integer) i.next();
	  List newpath = (List) nonce_to_vars.get(nonce);
	  Assert.assert(newpath != null);
	  worklist.add(newpath);
	}
      }

      // We want to flow from the highest point to the lowest
      Collections.reverse(dataflow_ppts);
      Collections.reverse(dataflow_transforms);

      // Convert results to arrays
      PptTopLevel[] ppts_array = (PptTopLevel[])
	dataflow_ppts.toArray(new PptTopLevel[dataflow_ppts.size()]);
      int[][] transforms_array = (int[][])
	dataflow_transforms.toArray(new int[dataflow_ppts.size()][]);
      // Intern to save memory (I hope?)
      for (int j=0; j < transforms_array.length; j++) {
	transforms_array[j] = Intern.intern(transforms_array[j]);
      }
      // Store result into receiving_ppt
      Assert.assert(receiving_ppt.dataflow_ppts == null);
      Assert.assert(receiving_ppt.dataflow_transforms == null);
      receiving_ppt.dataflow_ppts = ppts_array;
      receiving_ppt.dataflow_transforms = transforms_array;
    }
  }

  /**
   * @param outstream the stream to send output to
   * @param ppts the program points to dump
   *
   * Writes a textual (debugging) form of the program point hierarchy
   * and relationships to the given stream.
   **/
  public static void dump_ppts(OutputStream outstream,
			       PptMap ppts
			       )
  {
    PrintStream out = new PrintStream(outstream);

    for (Iterator iter = ppts.iterator(); iter.hasNext(); ) {
      Ppt ppt = (Ppt) iter.next();

      out.println(ppt.name);
      for (int i=0; i < ppt.var_infos.length; i++) {
	VarInfo vi = ppt.var_infos[i];
	out.println(vi.name.toString());
	out.println("  Declared type: " + vi.type.format() );
	out.println("  File rep type: " + vi.file_rep_type.format() );
	out.println("  Internal type: " + vi.rep_type.format() );
	out.println("  Comparability: " + vi.comparability );
	out.println("  PO higher:");
	int count = 0;
	for (Iterator vs = vi.po_higher().iterator(); vs.hasNext(); ) {
	  VarInfo v = (VarInfo) vs.next();
	  int nonce = vi.po_higher_nonce()[count++];
	  out.println("    " + nonce + ": " + v.name + " in " + v.ppt.name);
	}
	out.println("  PO lower:");
	count = 0;
	for (Iterator vs = vi.po_lower().iterator(); vs.hasNext(); ) {
	  VarInfo v = (VarInfo) vs.next();
	  int nonce = vi.po_lower_nonce()[count++];
	  out.println("    " + nonce + ": " + v.name + " in " + v.ppt.name);
	}
      }
      out.println();

    }
  }

}
