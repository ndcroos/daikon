package daikon.split.dsaa;

import daikon.*;
import daikon.split.*;

class Sample84Splitter_main extends Splitter {

	public Sample84Splitter_main() {
	}

	VarInfo i_varinfo;
	VarInfo NUM_OF_CASES_varinfo;

	public Sample84Splitter_main(Ppt ppt) {
		i_varinfo = ppt.findVar("i");
		NUM_OF_CASES_varinfo = ppt.findVar("NUM_OF_CASES");
	}

	public boolean valid() {
		return ((i_varinfo != null) && (NUM_OF_CASES_varinfo != null) && true);
	}

	public Splitter instantiate(Ppt ppt) {
		return new Sample84Splitter_main(ppt);
	}

	public String condition() {
		return "i < NUM_OF_CASES";
	}

	public boolean test(ValueTuple vt) {
		return (i_varinfo.getIntValue(vt) < NUM_OF_CASES_varinfo.getIntValue(vt));
	}
}

