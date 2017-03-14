package ri.p1;

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

public class Process {

	/*
	 * @param reversed: true prints the best values first
	 * 
	 * @param top: number of total terms to show
	 */
	public static void printIdf(String path, String field, boolean reversed, int top) throws IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;
		ArrayList<TuplaTermIdf> ti = new ArrayList<>();

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
			double df_t = termsEnum.docFreq();
			double idf = Math.log(n / df_t);
			System.out.println("n: " + n + "\tdft: " + df_t + "\tidf: " + idf);
			ti.add(new TuplaTermIdf(tt, idf));
		}
		indexReader.close();
		Collections.sort(ti, new TermComparatorIdf());
		if (reversed)
			Collections.reverse(ti);
		for (int i = 1; i <= Math.min(top, ti.size()); i++) {
			TuplaTermIdf tidf = ti.get(i - 1);
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
				if (freq != 0) {
					tf = 1 + Math.log(freq);
				}
				// System.out.println("\t" + tt + "/" + pathSgm + "/" + title);
				tl.add(new Termino(tt, idf, df_t, n, tf, title, pathSgm));
			}
		}
		indexReader.close();
		Collections.sort(tl, new TermComparatorTfIdf());
		if (reversed)
			Collections.reverse(tl);
		for (int i = 1; i <= Math.min(top, tl.size()); i++) {
			Termino term = tl.get(i - 1);
			System.out.println("Nº" + i + "\t" + term.getValue() + "\t" + term.getTerm() + "\tTf: " + term.getTf()
					+ "\tDf_t: " + term.getDf_t());
			// TODO: poner formato bien
			/*
			 * System.out.println("Nº" + i + "\t" + term.getValue() + "\t" +
			 * term.getTerm() + "/" + term.getPathSgm() + "/" + term.getTitle()
			 * + "\ttf: " + term.getTf() + "\tidf: " + term.getIdf());
			 */
		}

	}

	private static String usage = "java ri.p1.Process"
			+ " [-indexin INDEXFILE] [-best_idfterms FIELD N] [-poor_idfterms FIELD N]\n"
			+ " [-best_tfidfterms FIELD N] [-best_tfidfterms FIELD N]\n\n";

	public static void main(final String[] args) throws IOException {
		String indexFile = null;
		String option = null;
		String field = null;
		String n = null;
		if (args.length != 5)
			System.err.println("Invalid arguments: " + usage);
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-indexin":
				indexFile = args[i + 1];
				i++;
				break;
			default:
				option = args[i];
				field = args[i + 1];
				n = args[i + 2];
				i += 2;
				break;
			}
			Date start = new Date();
			switch (option) {
			case "-best_idfterms":
				printIdf(indexFile, field, true, Integer.parseInt(n));
				break;
			case "-poor_idfterms":
				printIdf(indexFile, field, false, Integer.parseInt(n));
				break;
			case "-best_tfidfterms":
				printTfIdf(indexFile, field, true, Integer.parseInt(n));
				break;
			case "-poor_tfidfterms":
				printTfIdf(indexFile, field, false, Integer.parseInt(n));
				break;
			default:
				System.err.println("Invalid arguments (" + option + "): " + usage);
				System.exit(1);
				break;
			}
			// printIdf(args[0], "BODY", false, 50);
			// printTfIdf(args[0], "BODY", false, 50);
			Date end = new Date();
			System.out.println(end.getTime() - start.getTime() + " total milliseconds");
		}
	}

}