package ri.mri_searcher;

import java.util.Comparator;

class ComparadorDeTerminosIdf  implements Comparator<Termino> {
	@Override
	public int compare(Termino a, Termino b) {
    	return (int) Math.signum(b.getIdf() - a.getIdf());
    }
}