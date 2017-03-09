package indexTest.indexTest;

import java.util.ArrayList;
import java.util.Collections;

public class TermList {

	private ArrayList<Termino> lTerminos = null;
	private boolean reversed; // if true de mayor a menor
	private TermComparator tc = null;
	private int n;

	public TermList(boolean reversed, int n) {
		lTerminos = new ArrayList<>();
		this.reversed = reversed;
		tc = new TermComparator();
		this.n = n;
	}

	public void addTerm(Termino term) {
		int index = lTerminos.indexOf(term); 
		if (index==-1){
			lTerminos.add(term);
			Collections.sort(lTerminos, tc);
			if (reversed)
				Collections.reverse(lTerminos);
			if (lTerminos.size() > n)
				lTerminos.remove(n);
		} else {
			Termino storedTerm = lTerminos.get(index);
			int df_t = storedTerm.getDf_t() + term.getDf_t();
			double idf = Math.log(term.getNumDocs()/df_t);
			storedTerm.setDf_t(df_t);
			storedTerm.setIdf(idf);
		}
	}
	
	public void printTerms(){
		for (int i = 1; i <= n; i++) {
			System.out.println("Nº" + i + "\t" + lTerminos.get(i-1));
		}
	}

	@Override
	public String toString() {
		return lTerminos.toString();
	}

}
