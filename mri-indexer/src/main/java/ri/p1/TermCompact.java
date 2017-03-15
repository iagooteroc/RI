package ri.p1;

public class TermCompact {
	private String term;
	private double idf;
	private double df_t;
	private double tf;
	private double value;
	

	public TermCompact(String term, double idf, double df_t, double tf) {
		this.term = term;
		this.idf = idf;
		this.df_t = df_t;
		this.tf = tf;
		this.value = idf*tf;
	}
	public double getValue() {
		return value;
	}
	
	public String getTerm() {
		return term;
	}

	
}
