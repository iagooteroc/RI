package ri.p1;

public class TermCompact {
	private String term;
	private double value;
	

	public TermCompact(String term, double idf, double tf) {
		this.term = term;
		this.value = idf*tf;
	}
	public double getValue() {
		return value;
	}
	
	public String getTerm() {
		return term;
	}

	
}
