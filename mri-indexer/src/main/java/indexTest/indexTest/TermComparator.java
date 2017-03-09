package indexTest.indexTest;

import java.util.Comparator;

class TermComparator implements Comparator<Termino> {
    @Override
    public int compare(Termino a, Termino b) {
    	if (a.getIdf() > b.getIdf()) {
    		return 1;
    	} else if (a.getIdf() < b.getIdf()) {
    		return -1;
    	}
    	return 0;
    }
}
