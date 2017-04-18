package ri.mri_searcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

//TODO: quitar throws y poner try-catch's

public class Search {

	private static String similarity = null;
	private static String indexPath = null;
	private static String cut = null;
	private static String top = null;
	private static String queries = null;
	private static String[] fields_list = null;
	private static String ndr = null;

	private static String queriesPath = "C:\\Users\\iago_\\Desktop\\Universidad\\3.- Recuperación de Información\\P2\\Cranfield\\cran.qry";
	private static String relPath = "C:\\Users\\iago_\\Desktop\\Universidad\\3.- Recuperación de Información\\P2\\Cranfield\\cranqrel";

	private static int LAST_QUERY = 225;

	private static int firstQueryId = 0;

	private static List<String> collectArgs(String[] args, int i) {
		List<String> collectedArgs = new ArrayList<>();
		for (int j = i; j < args.length; j++) {
			if (args[j].startsWith("-"))
				break;
			collectedArgs.add(args[j]);
		}
		return collectedArgs;
	}

	private static boolean isRelevant(List<Integer> relevants, int obtained) {
		return relevants.contains(obtained);
	}

	private static int countRelevants(List<Integer> relevants, List<Integer> obtained) {
		int count = 0;
		for (int i = 0; i < obtained.size(); i++) {
			if (relevants.contains(obtained.get(i)))
				count++;
		}
		return count;
	}

	private static List<Integer> relevantDocs(int num) throws IOException {
		InputStream stream = Files.newInputStream(Paths.get(relPath));
		List<Integer> rDocs = new ArrayList<>();
		String str = IOUtils.toString(stream, "UTF-8");
		StringBuffer strBuffer = new StringBuffer(str);
		String text = strBuffer.toString();
		String[] lines = text.split("\n");
		String numStr = Integer.toString(num);

		int i = 0;
		while (i < lines.length) {
			while (lines[i].startsWith(numStr.concat(" "))) {
				String[] numbers = lines[i++].split(" ");
				rDocs.add(Integer.parseInt(numbers[1]));
				if (i >= lines.length)
					return rDocs;
			}
			i++;
		}
		return rDocs;
	}

	private static String searchQuery(int num) throws IOException {
		InputStream stream = Files.newInputStream(Paths.get(queriesPath));
		String str = IOUtils.toString(stream, "UTF-8");
		StringBuffer strBuffer = new StringBuffer(str);
		String text = strBuffer.toString();
		String[] lines = text.split("\n");
		StringBuilder query = new StringBuilder();

		int n = 0;
		int i = 0;
		while (i < lines.length) {
			if (lines[i].startsWith(".I")) {
				if (++n == num) {
					i += 2;
					while (!lines[i].startsWith(".I")) {
						query.append(lines[i++]);
						if (i >= lines.length)
							return query.toString();
					}
					break;
				}
			}
			i++;
		}
		return query.toString();
	}

	private static List<String> searchQueries(int num1, int num2) throws IOException {
		InputStream stream = Files.newInputStream(Paths.get(queriesPath));
		String str = IOUtils.toString(stream, "UTF-8");
		StringBuffer strBuffer = new StringBuffer(str);
		String text = strBuffer.toString();
		String[] lines = text.split("\n");
		StringBuilder query = new StringBuilder();
		List<String> queryList = new ArrayList<>();

		int min = Math.min(num1, num2);
		int max = Math.max(num1, num2);
		int n = 0;
		int i = 0;
		while (i < lines.length) {
			if (lines[i].startsWith(".I")) {
				n++;
				if ((n >= min) && (n <= max)) {
					i += 2;
					while (!lines[i].startsWith(".I")) {
						query.append(lines[i++]);
						if (i >= lines.length) {
							queryList.add(query.toString());
							return queryList;
						}
					}
					queryList.add(query.toString());
					query = new StringBuilder();
					i--;
				} else if (n > max)
					break;
			}
			i++;
		}
		return queryList;
	}

	private static List<String> obtainQueries(int opt, int int1, int int2) throws IOException {
		List<String> queries = null;
		switch (opt) {
		case 1:
			queries = searchQueries(1, LAST_QUERY);
			firstQueryId = 1;
			break;
		case 2:
			queries = new ArrayList<>();
			queries.add(searchQuery(int1));
			firstQueryId = int1;
			break;
		case 3:
			queries = searchQueries(int1, int2);
			firstQueryId = Math.min(int1, int2);
			break;
		default:
			//TODO: print error
			break;
		}
		return queries;
	}

	private static List<String> evalQuery(String option) throws IOException {
		int opt = 0;
		int int1 = 0, int2 = 0;
		if (option.equals("all")) {
			opt = 1;
		} else if (option.contains("-")) {
			opt = 3;
			String[] numbers = option.split("-");
			int1 = Integer.parseInt(numbers[0]);
			int2 = Integer.parseInt(numbers[1]);
		} else {
			opt = 2;
			int1 = Integer.parseInt(option);
		}
		return obtainQueries(opt, int1, int2);
	}

	private static void search(String[] fieldsproc, String indexin, int top, List<String> queries, int first)
			throws ParseException, IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;

		dir = FSDirectory.open(Paths.get(indexin));
		indexReader = DirectoryReader.open(dir);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldsproc, new StandardAnalyzer());
		Query query = null;
		double countPn = 0;
		double countRecall = 0;
		double countAp = 0;
		for (String queryStr : queries) {
			query = parser.parse(QueryParser.escape(queryStr));
			countPn += pn(top, indexSearcher, query, first);
			countRecall += recalln(top, indexSearcher, query, first);
			countAp += ap(Integer.parseInt(cut), indexSearcher, query, first++);
		}
		double pnMean = countPn / (double) queries.size();
		double recallnMean = countRecall / (double) queries.size();
		double map = countAp / (double) queries.size();
		System.out.println("pnMean: " + pnMean);
		System.out.println("recallnMean: " + recallnMean);
		System.out.println("MAP: " + map);
	}

	private static double pn(int n, IndexSearcher indexSearcher, Query query, int queryId) throws IOException {

		TopDocs topDocs = null;
		topDocs = indexSearcher.search(query, n);
		List<Integer> relevants = relevantDocs(queryId);

		double count = 0;
		for (int k = 0; k < Math.min(n, topDocs.scoreDocs.length); k++) {
			if (isRelevant(relevants, topDocs.scoreDocs[k].doc))
				count++;
		}
		return count / (double) n;

	}

	private static double recalln(int n, IndexSearcher indexSearcher, Query query, int queryId) throws IOException {

		TopDocs topDocs = null;
		topDocs = indexSearcher.search(query, n);
		List<Integer> relevants = relevantDocs(queryId);

		double count = 0;
		for (int k = 0; k < Math.min(n, topDocs.scoreDocs.length); k++) {
			if (isRelevant(relevants, topDocs.scoreDocs[k].doc))
				count++;
		}
		return count / (double) relevants.size();

	}

	private static double ap(int n, IndexSearcher indexSearcher, Query query, int queryId) throws IOException {

		TopDocs topDocs = null;
		topDocs = indexSearcher.search(query, n);
		List<Integer> relevants = relevantDocs(queryId);

		double relevantCount = 0;
		double precisionCount = 0;
		for (int k = 0; k < topDocs.scoreDocs.length; k++) {
			if (isRelevant(relevants, topDocs.scoreDocs[k].doc)) {
				relevantCount++;
				precisionCount += relevantCount / (k + 1);
			}
		}
		return relevantCount == 0 ? 0 : (precisionCount / relevantCount);

	}

	public static void main(final String[] args) {

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case ("-search"):
				similarity = args[i + 1];
				i++;
				break;
			case ("-indexin"):
				indexPath = args[i + 1];
				i++;
				break;
			case ("-cut"):
				cut = args[i + 1];
				i++;
				break;
			case ("-top"):
				top = args[i + 1];
				break;
			case ("-queries"):
				queries = args[i + 1];
				i++;
				break;
			case ("-fieldsproc"):
				fields_list = collectArgs(args, i + 1).toArray(new String[0]);
				i += fields_list.length;
				break;
			case ("-fieldsvisual"):
				//wat
				break;
			default:
				break;
			}
		}
		List<String> queryList = null;
		try {
			queryList = evalQuery(queries);
			search(fields_list, indexPath, 10, queryList, firstQueryId);
			search(fields_list, indexPath, 20, queryList, firstQueryId);
			//for (String query : queryList) {
			//	System.out.println(query);
			//}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	//---------------------------------------------
	/*
	 * Obtiene una lista de los Terms de los mejores n documentos relevantes
	 * para la queryId
	 */
	private static List<Terms> obtainTermsFromDocs(DirectoryReader indexReader, int queryId, int n) throws IOException {
		List<Terms> lTerms = new ArrayList<>();
		List<Integer> lRelDoc = relevantDocs(queryId);
		for (int i = 0; i < (Math.min(lRelDoc.size(), n)); i++) {
			lTerms.add(indexReader.getTermVector(lRelDoc.get(i), "W"));
		}
		return lTerms;
	}

	/*
	 * Antes de que me olvide: Para cada query. Cogemos los ndr primeros
	 * documentos relevantes según el cranqrel. Cogemos los Terms de esos
	 * documentos con .getTermVector() y obtenemos los mejores por tf*idf
	 */
	private static void mierdas(String query, int queryId) throws IOException {
		String[] queryTerms = query.split(" ");
		List<String> lTerms = new ArrayList<String>(Arrays.asList(queryTerms)); //Damn the hacks

		Directory dir = null;
		DirectoryReader indexReader = null;
		dir = FSDirectory.open(Paths.get(indexPath));
		indexReader = DirectoryReader.open(dir);
		List<Termino> tlIdf = new ArrayList<>();
		List<Termino> tlTfIdf = new ArrayList<>();

		double n = indexReader.maxDoc();
		
		// Primero, la lista tlIdf con los mejores términos de la query por Idf
		Terms terms = MultiFields.getTerms(indexReader, "W");
		TermsEnum termsEnum = terms.iterator();

		while (termsEnum.next() != null) {
			BytesRef br = termsEnum.term();
			final String tt = br.utf8ToString();
			if (!lTerms.contains(tt))
				continue;
			double df_t = termsEnum.docFreq();
			double idf = Math.log(n / df_t);
			tlIdf.add(new Termino(tt, idf, 0));
		}
		Collections.sort(tlIdf, new ComparadorDeTerminosIdf());

		// Segundo, la lista tlTfIdf con los mejores términos de los documentos relevantes
		// para la query por TfIdf
		List<Terms> termList = obtainTermsFromDocs(indexReader, queryId, Integer.parseInt(ndr));

		for (Terms terms2 : termList) {
			termsEnum = terms2.iterator();
			while (termsEnum.next() != null) {
				BytesRef br = termsEnum.term();
				final String tt = br.utf8ToString();
				double df_t = termsEnum.docFreq();
				double idf = Math.log(n / df_t);
				PostingsEnum pe = MultiFields.getTermPositionsEnum(indexReader, "W", br);
				while (pe.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
					long freq = pe.freq();
					double tf = 0;
					if (freq != 0) {
						tf = 1 + Math.log(freq);
					}
					tlTfIdf.add(new Termino(tt, idf, tf));
				}
			}
		}
		indexReader.close();
		Collections.sort(tlTfIdf, new ComparadorDeTerminosTfIdf());
		// Ahora coger los tq primeros de tlIdf y los td primeros de tlTfIdf
	}

	private static List<Terms> queryToTermsList(String query) {
		List<Terms> termList = new ArrayList<>();
		String[] terms = query.split(" ");
		for (int i = 0; i < terms.length; i++) {
			//TODO: Ayy lmao
		}
		return null;
	}

	private static void relevanceFeedback(String query) {
		Directory dir = null;
		DirectoryReader indexReader = null;

		try {
			dir = FSDirectory.open(Paths.get(indexPath));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		}
	}

}
