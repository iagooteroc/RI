package ri.mri_searcher;

import java.util.Comparator;

public class WordComparator   implements Comparator<WordInDoc> {
	@Override
	public int compare(WordInDoc a, WordInDoc b) {
    	return (int) Math.signum(b.getValue() - a.getValue());
    }
}
