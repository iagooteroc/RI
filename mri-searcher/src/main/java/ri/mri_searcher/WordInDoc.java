package ri.mri_searcher;

public class WordInDoc {
	
	private String term;
	private long freq;
	private long docSize;
	private int docId;
	private float score;
	private float value;
	
	public WordInDoc(String term, long freq, long docSize, int docId, float score) {
		this.term = term;
		this.freq = freq;
		this.docSize = docSize;
		this.docId = docId;
		this.score = score;
	}
	
	public void setValue(float f) {
		this.value = f;
	}

	public long getFreq() {
		return freq;
	}

	public long getDocSize() {
		return docSize;
	}
	
	public int getDocId() {
		return docId;
	}
	
	public float getDocScore() {
		return score;
	}

	public float getValue() {
		return value;
	}
	
	public String getTerm() {
		return term;
	}

	@Override
	public String toString() {
		return "WordInDoc [term=" + term + ", value=" + value + "]";
	}
	
	
}
