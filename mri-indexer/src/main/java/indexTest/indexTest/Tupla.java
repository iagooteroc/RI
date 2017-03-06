package indexTest.indexTest;

//public class Tupla implements Comparable<Tupla> {
public class Tupla {
	private String term;
	private double idf;
	
	public Tupla(String term, double idf) {
		this.term = term;
		this.idf = idf;
	}

	public String getTerm() {
		return term;
	}

	public double getIdf() {
		return idf;
	}
/*
	@Override
	public int compareTo(Tupla t) {
		return (int) (this.idf - t.getIdf());
	}*/
	
	@Override
	public String toString (){
        return "\n" + term + "idf= "+ idf;
    }
	
}
