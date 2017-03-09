package indexTest.indexTest;

public class Termino {
	private String term;
	private double idf;
	private int df_t;
	private int numDocs;
	long tf;
	String title;
	String pathSgm;
	

	public Termino(String term, double idf, int df_t, int numDocs, long tf, String title, String pathSgm) {
		this.term = term;
		this.idf = idf;
		this.df_t = df_t;
		this.numDocs = numDocs;
		this.tf = tf;
		this.title = title;
		this.pathSgm = pathSgm;
	}
	
	public String getTerm() {
		return term;
	}

	public double getIdf() {
		return idf;
	}
	
	public int getDf_t() {
		return df_t;
	}
	
	public int getNumDocs() {
		return numDocs;
	}

	public long getTf() {
		return tf;
	}

	public void setTf(long tf) {
		this.tf = tf;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPathSgm() {
		return pathSgm;
	}

	public void setPathSgm(String pathSgm) {
		this.pathSgm = pathSgm;
	}

	void setTerm(String term) {
		this.term = term;
	}

	public void setIdf(double idf) {
		this.idf = idf;
	}

	public void setDf_t(int df_t) {
		this.df_t = df_t;
	}
	
	public void setNumDocs(int numDocs) {
		this.numDocs = numDocs;
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

	@Override
	public String toString (){
        return term + "\tidf="+ idf;
    }
	
}
