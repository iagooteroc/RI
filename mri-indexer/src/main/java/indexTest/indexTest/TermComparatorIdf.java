package indexTest.indexTest;

import java.util.Comparator;

class TermComparatorIdf implements Comparator<TermIdf> {
    @Override
    public int compare(TermIdf a, TermIdf b) {
    	if (a.getIdf() > b.getIdf()) {
    		return 1;
    	} else if (a.getIdf() < b.getIdf()) {
    		return -1;
    	}
    	return 0;
    }
}
