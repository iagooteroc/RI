package indexTest.indexTest;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

public class SimpleDeleting {
	
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
		//QueryParser parser = new QueryParser(null, new StandardAnalyzer());
		
		try {
			query = parser.parse(queryStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		try {
			//System.out.println("Deleting documents with query: " + query);
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
	
	public static void delDocSterm (String indexFolder, String field, String term) {
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
	
	
	/**
	 * Project testlucene6_3_0 SimpleDeleting class opens the Simple Index and
	 * delete documents containing a term
	 */
	public static void main(String[] args) {

		/*
		if (args.length != 1) {
			System.out.println("Usage: java SimpleDeleting indexFolder");
			return;
		}*/

		String indexFolder = args[0];
		String query = args[1];
		delDocsQuery(indexFolder, query);
		System.exit(1);

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
			writer.deleteDocuments(new Term("modelDescription", "boolean"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			writer.deleteDocuments(new Term("modelDescription", "vector"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			writer.forceMergeDeletes();
			// Forces merging of all segments that have deleted documents.
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
}