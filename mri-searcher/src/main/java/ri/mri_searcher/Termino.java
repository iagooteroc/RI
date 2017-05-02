package ri.mri_searcher;

public class Termino {
	private String term;
	private double idf;
	private double tf;
	private double value;
	private String doc;

	public Termino(String term, double idf, double tf, String doc) {
		this.term = term;
		this.idf = idf;
		this.tf = tf;
		this.value = idf*tf;
		this.doc = doc;
	}
	
	public String getDoc() {
		return doc;
	}
	
	public double getIdf() {
		return idf;
	}
	
	public double getValue() {
		return value;
	}
	
	public String getTerm() {
		return term;
	}
	
	public double getTf() {
		return tf;
	}

	@Override
	public String toString() {
		return "Termino [term=" + term + ", idf=" + idf + ", value=" + value + "]";
	}
	
	

}
