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
import org.apache.lucene.index.Term;
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

public class Search {

	private static String similarity = null;
	private static String lambdaormu = null;
	private static String indexPath = null;
	private static String filename = null;
	private static String openMode = null;
	private static String collPath = null;
	private static String cut = null;
	private static String top = null;
	private static String queries = null;
	private static String[] fields_list = null;
	private static String[] visual_list = null;
	private static String tq = null;
	private static String td = null;
	private static String ndr = null;
	private static String ndr2 = null;
	private static String ndjm = null;
	private static String nwjm = null;
	private static String nddir = null;
	private static String nwdir = null;
	private static String explain = null;

	private static String queriesPath = "C:\\Users\\iago_\\Desktop\\Universidad\\3.- Recuperación de Información\\P2\\Cranfield\\cran.qry";
	private static String relPath = "C:\\Users\\iago_\\Desktop\\Universidad\\3.- Recuperación de Información\\P2\\Cranfield\\cranqrel";

	private static int LAST_QUERY = 225;

	private static int firstQueryId = 0;

	private static TopDocs prset;

	private static List<String> collectArgs(String[] args, int i) {
		List<String> collectedArgs = new ArrayList<>();
		for (int j = i; j < args.length; j++) {
			if (args[j].startsWith("-"))
				break;
			collectedArgs.add(args[j]);
		}
		return collectedArgs;
	}

	private static boolean isRelevant(List<Integer> relevants, Document doc) {
		int id = Integer.parseInt(doc.getField("I").stringValue().trim());
		return relevants.contains(id);
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

	/*
	 * opt: 1-> todas 2-> sólo 1 3-> entre un rango
	 */
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
			System.err.println("Error in parameter \"-queries\"");
			System.exit(1);
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

	private static void eval(String[] fieldsproc, String indexin, List<String> queries, int first, int top,
			String[] fieldsvisual) throws IOException, ParseException {
		Directory dir = null;
		DirectoryReader indexReader = null;

		dir = FSDirectory.open(Paths.get(indexin));
		indexReader = DirectoryReader.open(dir);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldsproc, new StandardAnalyzer());
		Query query = null;
		double countP10 = 0;
		double countRecall10 = 0;
		double countP20 = 0;
		double countRecall20 = 0;
		double countAp = 0;
		TopDocs topDocs = null;
		int i, j, docId;

		for (String queryStr : queries) {
			query = parser.parse(QueryParser.escape(queryStr));
			topDocs = indexSearcher.search(query, top);
			List<Integer> relevants = relevantDocs(first);
			System.out.println("*****************************************************************************");
			System.out.println("Query " + first + ": \"" + queryStr + "\"");
			System.out.println("Top Docs:");
			for (i = 0; i < topDocs.scoreDocs.length; i++) {
				System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				docId = topDocs.scoreDocs[i].doc;
				Document tDoc = indexReader.document(docId);
				if (isRelevant(relevants, tDoc)) {
					System.out.print("(X)");
				}
				System.out.println("Document" + "\tScore: " + topDocs.scoreDocs[i].score);
				for (j = 0; j < fieldsvisual.length; j++) {
					System.out.println(fieldsvisual[j] + ": " + tDoc.getField(fieldsvisual[j]).stringValue()); //toDo
				}
			}
			countP10 += pn(topDocs, relevants, indexReader, query, first, 10);
			countP20 += pn(topDocs, relevants, indexReader, query, first, 20);
			countRecall10 += recalln(topDocs, relevants, indexReader, query, first, 10);
			countRecall20 += recalln(topDocs, relevants, indexReader, query, first, 20);
			countAp += ap(Integer.parseInt(cut), indexReader, indexSearcher, query, first++);
			first++;
		}
		double p10Mean = countP10 / (double) queries.size();
		double recall10Mean = countRecall10 / (double) queries.size();
		double p20Mean = countP20 / (double) queries.size();
		double recall20Mean = countRecall20 / (double) queries.size();
		double map = countAp / (double) queries.size();
		System.out.println("=============================================================================");
		System.out.println("p@10 Mean: " + p10Mean);
		System.out.println("p@20 Mean: " + p20Mean);
		System.out.println("recall@10 Mean: " + recall10Mean);
		System.out.println("recall@20 Mean: " + recall20Mean);
		System.out.println("MAP: " + map);
		System.out.println("=============================================================================");
	}

	private static double pn(TopDocs topDocs, List<Integer> relevants, DirectoryReader indexReader, Query query,
			int queryId, int n) throws IOException {

		Document doc = null;
		double count = 0;
		for (int k = 0; k < Math.min(n, topDocs.scoreDocs.length); k++) {
			doc = indexReader.document(topDocs.scoreDocs[k].doc);
			if (isRelevant(relevants, doc)) {
				count++;
			}
		}
		return count / (double) n;

	}

	private static double recalln(TopDocs topDocs, List<Integer> relevants, DirectoryReader indexReader, Query query,
			int queryId, int n) throws IOException {

		double count = 0;
		for (int k = 0; k < Math.min(n, topDocs.scoreDocs.length); k++) {
			Document doc = indexReader.document(topDocs.scoreDocs[k].doc);

			if (isRelevant(relevants, doc)) {
				count++;
			}
		}
		return count / (double) relevants.size();

	}

	private static double ap(int n, DirectoryReader indexReader, IndexSearcher indexSearcher, Query query, int queryId)
			throws IOException {

		TopDocs topDocs = null;
		topDocs = indexSearcher.search(query, n);
		List<Integer> relevants = relevantDocs(queryId);

		double relevantCount = 0;
		double precisionCount = 0;
		for (int k = 0; k < topDocs.scoreDocs.length; k++) {
			Document doc = indexReader.document(topDocs.scoreDocs[k].doc);
			if (isRelevant(relevants, doc)) {
				relevantCount++;
				precisionCount += relevantCount / (k + 1);
			}
		}
		return relevantCount == 0 ? 0 : (precisionCount / relevants.size());

	}

	public static void main(final String[] args) throws ParseException, IOException {

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case ("-search"):
				similarity = args[i + 1];
				i++;
				break;
			case ("-index"):
				indexPath = args[i + 1];
				i++;
				break;
			case ("-filename"):
				filename = args[i + 1];
				i++;
				break;
			case ("-openmode"):
				openMode = args[i + 1];
				i++;
				break;
			case ("-indexingmodel"):
				similarity = args[i + 1];
				lambdaormu = args[i + 2];
				i += 2;
				break;
			case ("-coll"):
				collPath = args[i + 1];
				i++;
				break;
			case ("-cut"):
				cut = args[i + 1];
				i++;
				break;
			case ("-top"):
				top = args[i + 1];
				i++;
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
				visual_list = collectArgs(args, i + 1).toArray(new String[0]);
				i += visual_list.length;
				break;
			case ("-rf1"):
				tq = args[i + 1];
				td = args[i + 2];
				ndr = args[i + 3];
				i += 3;
				break;
			case ("-rf2"):
				ndr2 = args[i + 1];
				i++;
				break;
			case ("-prfjm"):
				ndjm = args[i + 1];
				nwjm = args[i + 2];
				i += 2;
				break;
			case ("-prfdir"):
				nddir = args[i + 1];
				nwdir = args[i + 2];
				i += 2;
				break;
			case ("-explain"):
				explain = args[i + 1];
				i++;
				break;
			default:
				break;
			}
		}
		Index.indexation(collPath, indexPath, filename, openMode, similarity, lambdaormu);
		List<String> queryList = null;
		queryList = evalQuery(queries);

		List<String> lRf1 = null;
		List<String> lRf2 = null;
		List<String> lPrfjm = null;
		List<String> lPrfdir = null;
		int queryId = firstQueryId;
		int ntop = Integer.parseInt(top);
		if (tq != null) {
			lRf1 = rf1(queryList, queryId, Integer.parseInt(tq), Integer.parseInt(td), Integer.parseInt(ndr),
					Integer.parseInt(top), fields_list, Boolean.parseBoolean(explain));
		}
		if (ndr2 != null) {
			lRf2 = rf2(queryList, queryId, Integer.parseInt(ndr2), Integer.parseInt(top));
		}
		if (ndjm != null) {
			lPrfjm = prfjm(queryList, queryId, Integer.parseInt(ndjm), Integer.parseInt(nwjm),
					Boolean.parseBoolean(explain));
		}

		if (nddir != null) {
			lPrfdir = prfdir(queryList, queryId, Integer.parseInt(nddir), Integer.parseInt(nwdir),
					Boolean.parseBoolean(explain));
		}

		try {
			queryList = evalQuery(queries);
			System.out.println("////////////////////////////\tQuery Original\t////////////////////////////");
			eval(fields_list, indexPath, queryList, firstQueryId, ntop, visual_list);

			if (lRf1 != null) {
				System.out.println("////////////////////////////\tRF1\t////////////////////////////");
				eval(fields_list, indexPath, lRf1, firstQueryId, ntop, visual_list);
			}
			if (lRf2 != null) {
				System.out.println("////////////////////////////\tRF2\t////////////////////////////");
				eval(fields_list, indexPath, lRf2, firstQueryId, ntop, visual_list);
			}
			if (lPrfjm != null) {
				System.out.println("////////////////////////////\tPRFJM\t////////////////////////////");
				eval(fields_list, indexPath, lPrfjm, firstQueryId, ntop, visual_list);
			}
			if (lPrfdir != null) {
				System.out.println("////////////////////////////\tPRFDIR\t////////////////////////////");
				eval(fields_list, indexPath, lPrfdir, firstQueryId, ntop, visual_list);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}

	private static List<String> rf1(List<String> queryList, int queryId, int tq, int td, int ndr, int top,
			String[] fieldprocs, boolean explain) throws IOException, ParseException {
		String newQuery = null;
		List<String> lista = new ArrayList<>();
		for (int i = 0; i < queryList.size(); i++) {
			newQuery = buildNewQuery(queryList.get(i), queryId++, tq, td, ndr, top, fieldprocs, explain);
			lista.add(newQuery);
		}
		return lista;
	}

	private static List<String> rf2(List<String> queryList, int queryId, int ndr, int top)
			throws IOException, ParseException {
		String newQuery = null;
		List<String> lista = new ArrayList<>();
		for (int i = 0; i < queryList.size(); i++) {
			newQuery = buildNewQuery2(queryList.get(i), queryId++, ndr, top);
			lista.add(newQuery);
		}
		return lista;
	}

	//---------------------------------------------
	/*
	 * Obtiene una lista de los Terms de los mejores n documentos relevantes
	 * para la queryId
	 */
	private static List<Terms> obtainTermsFromRelevantDocs(DirectoryReader indexReader, IndexSearcher indexSearcher,
			String queryStr, int queryId, String[] fieldsproc, int n, int top) throws IOException, ParseException {
		List<Terms> lTerms = new ArrayList<>();
		MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldsproc, new StandardAnalyzer());
		Query query = parser.parse(QueryParser.escape(queryStr));
		List<Integer> relevantDocs = relevantDocs(queryId);
		TopDocs topDocs = null;
		topDocs = indexSearcher.search(query, top);
		int i = 0;
		int rel = 0;
		while ((i < topDocs.scoreDocs.length) && (rel < n)) {
			Document tDoc = indexReader.document(topDocs.scoreDocs[i].doc);

			if (isRelevant(relevantDocs, tDoc)) {
				rel++;
				for (int j = 0; j < fieldsproc.length; j++) {
					lTerms.add(indexReader.getTermVector(topDocs.scoreDocs[i].doc, fieldsproc[j]));
				}
			}
			i++;
		}
		return lTerms;
	}

	/*
	 * Función que recorre los términos de la colección hasta encontrar term y
	 * obtenga su df_t
	 */
	private static double computeDf_t(BytesRef term, DirectoryReader indexReader) throws IOException {
		Terms terms = MultiFields.getTerms(indexReader, "W");
		TermsEnum termsEnum = terms.iterator();
		while (termsEnum.next() != null) {
			BytesRef br = termsEnum.term();
			if (!br.equals(term))
				continue;

			return termsEnum.docFreq();
		}
		return 0;
	}

	/*
	 * Construye una nueva query con los mejores términos obtenidos a partir de
	 * la query que recibe
	 */
	private static String buildNewQuery(String query, int queryId, int tq, int td, int ndr, int top,
			String[] fieldsproc, boolean explain) throws IOException, ParseException {
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
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		String[] queryTerms = query.split(" ");
		List<String> lTerms = new ArrayList<String>(Arrays.asList(queryTerms));

		List<Termino> tlIdf = new ArrayList<>();
		List<Termino> tlTfIdf = new ArrayList<>();

		StringBuilder newQuery = new StringBuilder();
		newQuery.append(query);

		double n = indexReader.maxDoc();

		// Primero, la lista tlIdf con los mejores términos de la query por Idf
		for (int i = 0; i < fieldsproc.length; i++) {
			Terms terms = MultiFields.getTerms(indexReader, fieldsproc[i]);
			TermsEnum termsEnum = terms.iterator();

			while (termsEnum.next() != null) {
				BytesRef br = termsEnum.term();
				final String tt = br.utf8ToString();
				if (!lTerms.contains(tt))
					continue;
				double df_t = termsEnum.docFreq();
				double idf = Math.log(n / df_t);
				tlIdf.add(new Termino(tt, idf, 0, null));
			}
		}
		Collections.sort(tlIdf, new ComparadorDeTerminosIdf());

		// Segundo, la lista tlTfIdf con los mejores términos de los documentos relevantes
		// para la query por TfIdf
		List<Terms> termList = obtainTermsFromRelevantDocs(indexReader, indexSearcher, query, queryId, fields_list, ndr,
				top);

		List<Integer> relevantDocs = relevantDocs(queryId);
		for (Terms terms2 : termList) {
			TermsEnum termsEnum = terms2.iterator();
			while (termsEnum.next() != null) {
				BytesRef br = termsEnum.term();
				final String tt = br.utf8ToString();

				//double df_t = termsEnum.docFreq();
				double df_t = computeDf_t(br, indexReader);
				double idf = Math.log(n / df_t);
				for (int i = 0; i < fieldsproc.length; i++) {
					PostingsEnum pe = MultiFields.getTermPositionsEnum(indexReader, fieldsproc[i], br);
					while (pe.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
						int docId = pe.docID();
						Document doc = indexReader.document(docId);
						if (!isRelevant(relevantDocs, doc))
							continue;
						String title = doc.get("T");
						long freq = pe.freq();
						double tf = 0;
						if (freq != 0) {
							tf = 1 + Math.log(freq);
						}
						Termino t = new Termino(tt, idf, tf, title);
						if (tlTfIdf.contains(t)) {
							int index = tlTfIdf.indexOf(t);
							if (tlTfIdf.get(index).getValue() < t.getValue())
								tlTfIdf.set(index, t);
						} else {
							tlTfIdf.add(t);
						}
					}
				}
			}
		}
		indexReader.close();
		Collections.sort(tlTfIdf, new ComparadorDeTerminosTfIdf());
		// Ahora coger los tq primeros de tlIdf y los td primeros de tlTfIdf
		//System.out.println("Idf:");
		if (explain)
			System.out.println("========RF1 EXPANDED QUERY " + queryId + "=========");
		for (int i = 0; i < Math.min(tq, tlIdf.size()); i++) {
			newQuery.append(tlIdf.get(i).getTerm() + " ");
			if (explain)
				System.out.println(tlIdf.get(i));
		}

		//System.out.println("-----------------\nTf*Idf:");
		if (explain)
			System.out.println("--------------------------------");
		for (int i = 0; i < Math.min(td, tlTfIdf.size()); i++) {
			newQuery.append(tlTfIdf.get(i).getTerm() + " ");
			if (explain)
				System.out.println(tlTfIdf.get(i));
			//System.out.println(tlTfIdf.get(i) + " tf: " + tlTfIdf.get(i).getTf() + "doc: " + tlTfIdf.get(i).getDoc());
			//System.out.println(tlTfIdf.get(i) + " - " + tlTfIdf.get(i).getDoc());
		}
		return newQuery.toString();
	}

	/*
	 * Obtiene una lista de los títulos de los mejores n documentos relevantes
	 * para la queryId
	 */
	private static List<String> obtainTitlesFromDocs(DirectoryReader indexReader, IndexSearcher indexSearcher,
			String queryStr, int queryId, String[] fieldsproc, int n, int top) throws IOException, ParseException {
		List<String> lTitles = new ArrayList<>();
		MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldsproc, new StandardAnalyzer());
		Query query = parser.parse(QueryParser.escape(queryStr));
		List<Integer> relevantDocs = relevantDocs(queryId);
		TopDocs topDocs = null;
		topDocs = indexSearcher.search(query, top);
		int i = 0;
		int rel = 0;
		while ((i < topDocs.scoreDocs.length) && (rel < n)) {
			Document tDoc = indexReader.document(topDocs.scoreDocs[i].doc);
			if (isRelevant(relevantDocs, tDoc)) {
				rel++;
				lTitles.add(tDoc.getField("T").stringValue().trim());
			}
			i++;
		}

		return lTitles;
	}

	/*
	 * Construye una nueva query con los títulos obtenidos a partir de la query
	 * que recibe
	 */
	private static String buildNewQuery2(String query, int queryId, int ndr, int top)
			throws IOException, ParseException {
		Directory dir = null;
		DirectoryReader indexReader = null;

		StringBuilder newQuery = new StringBuilder();
		newQuery.append(query);

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
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		List<String> titleList = obtainTitlesFromDocs(indexReader, indexSearcher, query, queryId, fields_list, ndr,
				top);

		for (String title : titleList) {
			newQuery.append(title + " ");
		}
		return newQuery.toString();
	}

	//==========================================================//
	//---------------------FARLANDS-----------------------------//
	//==========================================================//
	/*
	 * Obtiene una lista de los Terms de los mejores n documentos relevantes
	 * para la queryId
	 */
	private static List<Terms> obtainTermsFromDocs(DirectoryReader indexReader, IndexSearcher indexSearcher,
			String queryStr, int queryId, String[] fieldsproc, int n) throws IOException, ParseException {
		List<Terms> lTerms = new ArrayList<>();
		MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldsproc, new StandardAnalyzer());
		Query query = parser.parse(QueryParser.escape(queryStr));
		TopDocs topDocs = null;
		topDocs = indexSearcher.search(query, n);

		prset = topDocs;
		for (int j = 0; j < fieldsproc.length; j++) {
			for (int i = 0; i < (Math.min(topDocs.scoreDocs.length, n)); i++) {
				Document tDoc = indexReader.document(topDocs.scoreDocs[i].doc);
				int id = Integer.parseInt(tDoc.getField("I").stringValue().trim());
				lTerms.add(indexReader.getTermVector(id, fieldsproc[j]));

			}
		}
		return lTerms;
	}

	private static List<String> prfjm(List<String> queryList, int queryId, int nd, int nw, boolean explain)
			throws IOException, ParseException {
		String newQuery = null;
		List<String> lista = new ArrayList<>();
		for (int i = 0; i < queryList.size(); i++) {
			newQuery = buildNewQueryPrf(queryList.get(i), queryId++, nd, nw, true, explain);
			lista.add(newQuery);
		}
		return lista;

	}

	private static List<String> prfdir(List<String> queryList, int queryId, int nd, int nw, boolean explain)
			throws IOException, ParseException {
		String newQuery = null;
		List<String> lista = new ArrayList<>();
		for (int i = 0; i < queryList.size(); i++) {
			newQuery = buildNewQueryPrf(queryList.get(i), queryId++, nd, nw, false, explain);
			lista.add(newQuery);
		}
		return lista;

	}

	private static String buildNewQueryPrf(String query, int queryId, int nd, int nw, boolean prf, boolean explain)
			throws IOException, ParseException {
		Directory dir = null;
		DirectoryReader indexReader = null;

		StringBuilder newQuery = new StringBuilder();
		// #concat
		//newQuery.append(query);

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
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		long n = indexReader.getSumTotalTermFreq("W");

		List<Terms> termList = obtainTermsFromDocs(indexReader, indexSearcher, query, queryId, fields_list, nd);

		List<WordInDoc> wordList = new ArrayList<>();
		List<WordInDoc> result = new ArrayList<>();

		for (Terms terms : termList) {
			TermsEnum termsEnum = terms.iterator();
			while (termsEnum.next() != null) {
				BytesRef br = termsEnum.term();
				final String term = br.utf8ToString();
				//double df_t = termsEnum.docFreq();
				//wordList = new ArrayList<>();
				PostingsEnum pe = MultiFields.getTermPositionsEnum(indexReader, "W", br);
				long totalTerm = indexReader.totalTermFreq(new Term("W", br));
				while (pe.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
					int docId = pe.docID();
					//Document doc = indexReader.document(docId);
					if (!isInTopDocs(prset, docId))
						continue;
					Terms vector = indexReader.getTermVector(docId, "W");
					long docSize = countDocSize(vector, docId, indexReader); // |D|
					long freq = pe.freq(); // nº de apariciones de w en D
					//tlTfIdf.add(new Termino(tt, idf, tf, title));
					float score = getScore(prset, docId);
					WordInDoc word = new WordInDoc(term, freq, totalTerm, docSize, docId, score);
					if (wordList.contains(word)) {
						int index = wordList.indexOf(word);
						WordInDoc w = wordList.get(index);
						if (w.getDocsId().contains(word.getDocId(0)))
							continue;
						wordList.get(index).addData(word);
					} else {
						wordList.add(word);
					}
					//wordList.add(new WordInDoc(term, freq, docSize, docId, score));
					//System.out.println(score);
				}

			}
		}
		if (prf) {
			result.addAll(computeWordsJr(wordList, n, nd));
		} else {
			result.addAll(computeWordsDir(wordList, n, nd));
		}
		Collections.sort(result, new WordComparator());

		System.out.println("========PRF EXPANDED QUERY " + queryId + "=========");

		for (int i = 0; i < Math.min(nw, result.size()); i++) {
			newQuery.append(result.get(i).getTerm() + " ");
			WordInDoc word = result.get(i);
			if (explain) {
				System.out.println(word.getTerm() + ": ");
				for (int j = 0; j < word.getDataSize(); j++)
					
				System.out.println("P(D) = " + word.getPd() + "\tP(w|D) = " + word.getPwd(j)
						+ "\tsum(P(qi|D)) = " + word.getPqd(j));
			}
		}
		return newQuery.toString();
	}

	private static long countDocSize(Terms terms, int docId, DirectoryReader indexReader) throws IOException {
		int docSize = 0;
		TermsEnum termsEnum = terms.iterator();
		while (termsEnum.next() != null) {
			BytesRef br = termsEnum.term();
			//double df_t = termsEnum.docFreq();
			//wordList = new ArrayList<>();
			PostingsEnum pe = MultiFields.getTermPositionsEnum(indexReader, "W", br);
			while (pe.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
				if (pe.docID() != docId)
					continue;
				docSize += pe.freq();
				break;
			}
		}
		return docSize;
	}

	private static boolean isInTopDocs(TopDocs topDocs, int docId) {
		for (int i = 0; i < topDocs.scoreDocs.length; i++) {
			if (topDocs.scoreDocs[i].doc == docId)
				return true;
		}
		return false;
	}

	/*
	 * 
	 */
	private static List<WordInDoc> computeWordsJr(List<WordInDoc> wordList, long sumTotalTermFreq, int nd) {
		float min = Math.min(prset.scoreDocs.length, nd);
		float lambda = Float.parseFloat(lambdaormu);
		float pd = 1 / min;
		float pwd;
		float pqd;
		for (WordInDoc word : wordList) {
			word.setPd(pd);
			for (int i = 0; i < word.getDataSize(); i++) {
				pwd = (float) (1 - lambda) * ((float) word.getFreq(i) / (float) word.getDocSize(i))
						+ (float) lambda * ((float) word.getTotalTermFreq(i) / (float) sumTotalTermFreq);
				pqd = word.getDocScore(i);
				word.addPwd(pwd);
				word.addPqd(pqd);
				word.addValue(pd * pwd * pqd);
			}
		}
		return wordList;
	}

	private static List<WordInDoc> computeWordsDir(List<WordInDoc> wordList, long sumTotalTermFreq, int nd) {
		float min = Math.min(prset.scoreDocs.length, nd);
		float mu = Float.parseFloat(lambdaormu);
		float pd = 1 / min;
		float pwd;
		float pqd;
		for (WordInDoc word : wordList) {
			word.setPd(pd);
			for (int i = 0; i < word.getDataSize(); i++) {
				pwd = ((word.getFreq(i) + mu * (word.getTotalTermFreq(i) / sumTotalTermFreq))
						/ (float) (word.getDocSize(i) + mu));
				pqd = word.getDocScore(i);
				word.addPwd(pwd);
				word.addPqd(pqd);
				word.addValue(pd * pwd * pqd);
			}
		}
		return wordList;
	}

	private static float getScore(TopDocs topDocs, int docId) {
		for (int i = 0; i < topDocs.scoreDocs.length; i++) {
			if (topDocs.scoreDocs[i].doc == docId)
				return topDocs.scoreDocs[i].score;
		}
		return 0;
	}

	/*
	 * Para computar el modelo de relevancia: Obtener todas las palabras de los
	 * nd primeros documentos del ranking -> w Para cada w sumar Para cada D
	 * multiplicar 1/(nd o menos) (1-lambda)*(nº de apariciones de w en D (freq)
	 * / |D|) + lambda*(nº de apariciones de w en la colección / |C|) score del
	 * documento
	 */

}
