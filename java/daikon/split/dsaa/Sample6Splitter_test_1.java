package daikon.split.dsaa;

import daikon.*;
import daikon.split.*;

class Sample6Splitter_test_1 extends Splitter {

	public Sample6Splitter_test_1() {
	}

	VarInfo i_varinfo;
	VarInfo size_varinfo;

	public Sample6Splitter_test_1(Ppt ppt) {
		i_varinfo = ppt.findVar("i");
		size_varinfo = ppt.findVar("size");
	}

	public boolean valid() {
		return ((i_varinfo != null) && (size_varinfo != null) && true);
	}

	public Splitter instantiate(Ppt ppt) {
		return new Sample6Splitter_test_1(ppt);
	}

	public String condition() {
		return "i < size";
	}

	public boolean test(ValueTuple vt) {
		return (i_varinfo.getIntValue(vt) < size_varinfo.getIntValue(vt));
	}
}

