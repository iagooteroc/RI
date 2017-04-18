package ri.mri_searcher;

import java.util.Comparator;

class ComparadorDeTerminosTfIdf  implements Comparator<Termino> {
	@Override
	public int compare(Termino a, Termino b) {
    	return (int) Math.signum(b.getValue() - a.getValue());
    }
}
