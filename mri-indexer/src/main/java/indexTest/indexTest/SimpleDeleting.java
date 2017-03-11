package indexTest.indexTest;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

public class SimpleDeleting {

	private static String indexin = null;
	private static String indexout = null;
	private static String field = null;
	private static String term = null;
	private static String query = null;
	private static String header1 = null;
	private static String header2 = null;
	private static String n = null;

	/**
	 * Project testlucene6_3_0 SimpleDeleting class opens the Simple Index and
	 * delete documents containing a term
	 */

	private static String usage = "java org.apache.lucene.demo.SimpleDeleting"
			+ " [-indexin INDEXFILE] [-indexout INDEXFILE] [-deldocsterm TERM] \n"
			+ " [-deldocsquery QUERY] [-mostsimilardoc_title Hilos]\n0" + " [-mostsimilardoc_title N HEADER]\n\n";

	private static void validateArgs() {
		// Check if index is provided
		if (indexin == null) {
			System.err.println("at least indexin: " + usage);
			System.exit(1);
		}

		//
		if (collPath == null && collsPath.isEmpty()) {
			System.err.println("coll or colls are required: " + usage);
			System.exit(1);
		}

		// if indexes1 or 2 were provided, check if colls is valid
		if (indexes1Path != null || indexes2Path != null) {
			if (collsPath == null) {
				System.err.println("if indexes1 or 2 are provided, colls is required: " + usage);
				System.exit(1);
			}
		}

		// if indexes1 is provided, check if it has at least 1 index
		if (indexes1Path != null && indexes1Path.size() < 2) {
			System.err.println("indexes1 must have at least one PATH apart from PATH0: " + usage);
			System.exit(1);
		}
	}

	public static void delDocsQuery(String indexFolder, String queryStr) {
		IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
		config.setOpenMode(OpenMode.APPEND);

		IndexWriter writer = null;
		Query query = null;

		try {
			writer = new IndexWriter(FSDirectory.open(Paths.get(indexFolder)), config);
		} catch (CorruptIndexException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		} catch (LockObtainFailedException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		}
		QueryParser parser = new QueryParser("BODY", new StandardAnalyzer());
		// QueryParser parser = new QueryParser(null, new StandardAnalyzer());

		try {
			query = parser.parse(queryStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		try {
			// System.out.println("Deleting documents with query: " + query);
			writer.deleteDocuments(query);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			writer.commit();
			writer.close();
		} catch (CorruptIndexException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}
	}

	public static void delDocSterm(String indexFolder, String field, String term) {
		IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
		config.setOpenMode(OpenMode.APPEND);

		IndexWriter writer = null;

		try {
			writer = new IndexWriter(FSDirectory.open(Paths.get(indexFolder)), config);
		} catch (CorruptIndexException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		} catch (LockObtainFailedException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		}

		try {
			writer.deleteDocuments(new Term(field, term));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			writer.commit();
			writer.close();
		} catch (CorruptIndexException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		if (args.length != 1) {
			System.out.println(usage);
			return;
		}

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case ("-indexin"):
				indexin = args[i + 1];
				i++;
				break;
			case ("-indexout"):
				indexout = args[i + 1];
				i++;
				break;
			case ("-deldocsterm"):
				field = args[i + 1];
				term = args[i + 2];
				i++;
				break;
			case ("-deldocsquery"):
				query = args[i + 1];
				i++;
				break;
			case ("-mostsimilardoc_title"):
				header1 = args[i + 1];
				i++;
				break;
			case ("-mostsimilardoc_body"):
				n = args[i + 1];
				header2 = args[i + 2];
				i++;
				break;
			default:
				break;
			}
		}
		
		if (indexin == null) {
			System.err.println("At least indexin: " + usage);
			System.exit(1);
		} 
		
		if ((field!=null)&&(term!=null)){
			delDocSterm(indexin,field,term);
			System.exit(0);
		} else {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}
		
		if (query!=null){
			delDocsQuery(indexin,query);
			System.exit(0);
		} else {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}
		
		if ((header1!=null)&&(indexout!=null)){
			//MAGIAPOTAJIA
			System.exit(0);
		} else {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}
		
		if ((header2!=null)&&(indexout!=null)&&(n!=null)){
			//MAGIAPOTAJIA
			System.exit(0);
		} else {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}
		
	}
}