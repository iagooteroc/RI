package ri.mri_searcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

//TODO: quitar throws y poner try-catch's

public class Search {

	private static String similarity = null;
	private static String indexPath = null;
	private static String cut = null;
	private static String top = null;
	private static String queries = null;

	private static String queriesPath = "C:\\Users\\iago_\\Desktop\\Universidad\\3.- Recuperación de Información\\P2\\Cranfield\\cran.qry";
	private static String relPath = "C:\\Users\\iago_\\Desktop\\Universidad\\3.- Recuperación de Información\\P2\\Cranfield\\cranqrel";

	private static int LAST_QUERY = 225;

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
			queries = searchQueries(0, LAST_QUERY);
			break;
		case 2:
			queries = searchQueries(int1, int2);
			break;
		case 3:
			queries = new ArrayList<>();
			queries.add(searchQuery(int1));
			break;
		default:
			//TODO: print error
			break;
		}
		return queries;
	}

	private static void evalQuery(String option) throws IOException {
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
		List<String> queries = obtainQueries(opt, int1, int2);

	}

	private static void search(String[] fieldsproc, String indexin, int top, List<String> queries, int first) throws ParseException, IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;

		dir = FSDirectory.open(Paths.get(indexin));
		indexReader = DirectoryReader.open(dir);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldsproc, new StandardAnalyzer());
		Query query = null;
		double countPn = 0;
		double countRecall = 0;
		for (String queryStr : queries) {
			query = parser.parse(queryStr);
			countPn += pn(top, indexSearcher, query, first);
			countRecall += recalln(top, indexSearcher, query, first++);
		}
		double pnMean = countPn/(double) queries.size();
		double recallnMean = countRecall/(double) queries.size();
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
		return count/(double)n;

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
		return count/(double)relevants.size();

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
				//wat
				break;
			case ("-fieldsvisual"):
				//wat
				break;
			default:
				break;
			}
		}
		String query = null;
		List<Integer> rDocs = null;
		try {
			//query = searchQuery(225);
			//rDocs = relevantDocs(225);
			//System.out.println(searchQueries(3, 5));

			List<String> queryList = searchQueries(1, 225);
			System.out.println(queryList.size());
			/*
			 * for (String q : queryList) { System.out.println("======\n" + q +
			 * "======\n"); }
			 */

			//System.out.println(searchQueries(1, 225));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(query);
		//System.out.println(rDocs);
	}
}
