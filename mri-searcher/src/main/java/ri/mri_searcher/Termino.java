package ri.mri_searcher;

public class Termino {
	private String term;
	private double idf;
	private double tf;
	private double value;
	

	public Termino(String term, double idf, double tf) {
		this.term = term;
		this.idf = idf;
		this.tf = tf;
		this.value = idf*tf;
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

}
