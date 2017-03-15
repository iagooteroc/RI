package ri.p1;

import java.util.Comparator;

class TermCComparatorTfIdf implements Comparator<TermCompact> {
	@Override
	public int compare(TermCompact a, TermCompact b) {
    	return (int) Math.signum(b.getValue() - a.getValue());
    }
}
