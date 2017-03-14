package ri.p1;

import java.util.Comparator;

class TermComparatorIdf implements Comparator<TuplaTermIdf> {
    @Override
    public int compare(TuplaTermIdf a, TuplaTermIdf b) {
    	if (a.getIdf() > b.getIdf()) {
    		return 1;
    	} else if (a.getIdf() < b.getIdf()) {
    		return -1;
    	}
    	return 0;
    }
}
