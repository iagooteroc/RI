package indexTest.indexTest;

import java.util.Comparator;

class TuplaComparator implements Comparator<Tupla> {
    @Override
    public int compare(Tupla a, Tupla b) {
    	if (a.getIdf() > b.getIdf()) {
    		return 1;
    	} else if (a.getIdf() < b.getIdf()) {
    		return -1;
    	}
    	return 0;
    }
}
