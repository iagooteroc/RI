package ri.mri_searcher;

import java.util.ArrayList;
import java.util.List;

public class WordInDoc {
	
	private String term;
	private List<Long> freq = new ArrayList<>();
	private List<Long> totalTermFreq = new ArrayList<>();
	private List<Long> docSize = new ArrayList<>();
	private List<Integer> docId = new ArrayList<>();
	private List<Float> score = new ArrayList<>();
	private float value;
	private float pd;
	private List<Float> pwd = new ArrayList<>();
	private List<Float> pqd = new ArrayList<>();
	
	public WordInDoc(String term, long freq, long totalTermFreq, long docSize, int docId, float score) {
		this.term = term;
		this.freq.add(freq);
		this.totalTermFreq.add(totalTermFreq);
		this.docSize.add(docSize);
		this.docId.add(docId);
		this.score.add(score);
	}
	
	public void addData(WordInDoc word) {
		this.freq.add(word.getFreq(0));
		this.totalTermFreq.add(word.getTotalTermFreq(0));
		this.docSize.add(word.getDocSize(0));
		this.docId.add(word.getDocId(0));
		this.score.add(word.getDocScore(0));
	}
	
	public void setPd(float pd) {
		this.pd = pd;
	}
	
	public void addPwd(float pwd) {
		this.pwd.add(pwd);
	}
	
	public void addPqd(float pqd) {
		this.pqd.add(pqd);
	}
	
	public float getPd() {
		return pd;
	}

	public float getPwd(int i) {
		return pwd.get(i);
	}

	public float getPqd(int i) {
		return pqd.get(i);
	}

	public void addValue(float f) {
		this.value += f;
	}

	public long getFreq(int i) {
		return freq.get(i);
	}
	
	public long getTotalTermFreq(int i) {
		return totalTermFreq.get(i);
	}

	public long getDocSize(int i) {
		return docSize.get(i);
	}
	
	public int getDocId(int i) {
		return docId.get(i);
	}
	
	public List<Integer> getDocsId() {
		return docId;
	}
	
	public float getDocScore(int i) {
		return score.get(i);
	}

	public float getValue() {
		return value;
	}
	
	public String getTerm() {
		return term;
	}
	
	public int getDataSize() {
		return freq.size();
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
