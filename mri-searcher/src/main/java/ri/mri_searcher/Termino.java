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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((term == null) ? 0 : term.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Termino other = (Termino) obj;
		if (term == null) {
			if (other.term != null)
				return false;
		} else if (!term.equals(other.term))
			return false;
		return true;
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
		if (value==0)
			return term + " idf=" + idf;
		return term + " idf=" + idf + ", tf=" + tf;
	}
	
	

}
