package ri.p1;

import java.util.Comparator;

class TermComparatorTfIdf implements Comparator<Termino> {
	@Override
	public int compare(Termino a, Termino b) {
    	return (int) Math.signum(b.getValue() - a.getValue());
    }
}
