package ri.p1;

import java.util.Comparator;

class TermComparatorTfIdf implements Comparator<Termino> {
    @Override
    public int compare(Termino a, Termino b) {
    	if (a.getValue() > b.getValue()) {
    		return 1;
    	} else if (a.getValue() < b.getValue()) {
    		return -1;
    	}
    	return 0;
    }
}
