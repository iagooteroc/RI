package ri.p1;

import java.util.Comparator;

class TermComparatorIdf implements Comparator<TuplaTermIdf> {
    @Override
    public int compare(TuplaTermIdf a, TuplaTermIdf b) {
    	return (int) Math.signum(b.getIdf() - a.getIdf());
    }
}
