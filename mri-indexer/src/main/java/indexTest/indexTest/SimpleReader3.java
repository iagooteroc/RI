package indexTest.indexTest;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class SimpleReader3 {
	
	/*
	 * @param reversed: true prints the best values first
	 * 
	 * @param top: number of total terms to show
	 */
	public static void printIdf(String path, String field, boolean reversed, int top) throws IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;
		ArrayList<TermIdf> ti = new ArrayList<>();

		try {
			dir = FSDirectory.open(Paths.get(path));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		}
		int n = indexReader.numDocs();
		final Terms terms = MultiFields.getTerms(indexReader, field);
		final TermsEnum termsEnum = terms.iterator();

		while (termsEnum.next() != null) {
			BytesRef br = termsEnum.term();
			final String tt = br.utf8ToString();
			int df_t = termsEnum.docFreq();
			double idf = Math.log(n / df_t);
			System.out.println("n: " + n + "\tdft: " + df_t + "\tidf: " + idf);
			ti.add(new TermIdf(tt, idf));
		}
		indexReader.close();
		Collections.sort(ti, new TermComparatorIdf());
		if (reversed)
			Collections.reverse(ti);
		for (int i = 1; i <= Math.min(top,ti.size()); i++) {
			TermIdf tidf = ti.get(i-1);
			System.out.println("Nº" + i + "\t" + tidf.getTerm() + "\tidf: " + tidf.getIdf());
		}
	}
	
	/*
	 * @param reversed: true prints the best values first
	 * 
	 * @param top: number of total terms to show
	 */
	public static void printTfIdf(String path, String field, boolean reversed, int top) throws IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;
		ArrayList<Termino> tl = new ArrayList<>();

		try {
			dir = FSDirectory.open(Paths.get(path));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		}
		double n = indexReader.numDocs();
		final Terms terms = MultiFields.getTerms(indexReader, field);
		final TermsEnum termsEnum = terms.iterator();

		while (termsEnum.next() != null) {
			BytesRef br = termsEnum.term();
			final String tt = br.utf8ToString();
			double df_t = termsEnum.docFreq();
			double idf = Math.log(n / df_t);
			PostingsEnum pe = MultiFields.getTermPositionsEnum(indexReader, field, br);
			while (pe.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
				int docId = pe.docID();
				Document doc = indexReader.document(docId);
				String title = doc.get("TITLE");
				String pathSgm = doc.get("PathSgm");
				long freq = pe.freq();
				double tf = 0;
				if (freq!=0) {
					tf = 1 + Math.log(freq);
				}
				//System.out.println("\t" + tt + "/" + pathSgm + "/" + title);
				tl.add(new Termino(tt, idf, df_t, n, tf, title, pathSgm));
			}
		}
		indexReader.close();
		Collections.sort(tl, new TermComparatorTfIdf());
		if (reversed)
			Collections.reverse(tl);
		for (int i = 1; i <= Math.min(top,tl.size()); i++) {
			Termino term = tl.get(i-1);
			System.out.println("Nº" + i + "\t" + term.getValue() + "\t" + term.getTerm() + "\tTf: " + term.getTf() + 
					"\tDf_t: " + term.getDf_t());
			/*
			System.out.println("Nº" + i + "\t" + term.getValue() + "\t" + term.getTerm() + "/" + term.getPathSgm() + "/" + term.getTitle()
					+ "\ttf: " + term.getTf() + "\tidf: " + term.getIdf());*/
		}
		
	}

	/**
	 * Los índices de Lucene se almacenan en forma de segmentos, cada segmento
	 * se considera una hoja (leaf) del índice. Este ejemplo lee los contenidos
	 * de un índice usando AtomicReaders. Un AtomicReader lee los contenidos de
	 * un segmento o leaf.
	 * 
	 * IndexReader instances for indexes on disk are usually constructed with a
	 * call to one of the static DirectoryReader.open() methods, e.g.
	 * DirectoryReader.open(Directory). DirectoryReader implements the
	 * CompositeReader interface, it is not possible to directly get postings.
	 * 
	 * LeafReader: These indexes do not consist of several sub-readers, they are
	 * atomic. They support retrieval of stored fields, doc values, terms, and
	 * postings.
	 *
	 * Si nuestro índice es pequeño solamente tendrá un segmento por lo que la
	 * llamada indexReader.getContext().leaves() nos dará una lista con un único
	 * LeafReaderContext (objeto del cual se obtiene el LeafReader).
	 */
	public static void main(final String[] args) throws IOException {
		Date start = new Date();
		//printIdf(args[0], "BODY", false, 50);
		printTfIdf(args[0], "BODY", false, 50);
		Date end = new Date();
		System.out.println(end.getTime() - start.getTime() + " total milliseconds");
	}

}