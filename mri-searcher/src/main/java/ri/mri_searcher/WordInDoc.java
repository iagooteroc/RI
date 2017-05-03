package ri.mri_searcher;

public class WordInDoc {
	
	private String term;
	private long freq;
	private long totalTermFreq;
	private long docSize;
	private int docId;
	private float score;
	private float value;
	
	public WordInDoc(String term, long freq, long totalTermFreq, long docSize, int docId, float score) {
		this.term = term;
		this.freq = freq;
		this.totalTermFreq = totalTermFreq;
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
	
	public long getTotalTermFreq() {
		return totalTermFreq;
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
		return "WordInDoc [term=" + term + ", freq=" + freq + ", docSize=" + docSize + ", docId=" + docId + ", score="
				+ score + ", value=" + value + "]";
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
		WordInDoc other = (WordInDoc) obj;
		if (term == null) {
			if (other.term != null)
				return false;
		} else if (!term.equals(other.term))
			return false;
		return true;
	}


	/*
	@Override
	public String toString() {
		return "WordInDoc [term=" + term + ", value=" + value + "]";
	}*/
	
	
}
