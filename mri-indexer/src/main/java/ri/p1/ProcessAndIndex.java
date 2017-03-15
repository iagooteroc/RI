package ri.p1;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

public class ProcessAndIndex {

	private static String indexin = null;
	private static String indexout = null;
	private static String field = null;
	private static String term = null;
	private static String query = null;
	private static String hilos1 = null;
	private static String hilos2 = null;
	private static String n = null;

	private static void validateArgs() {
		if (indexin == null) {
			System.err.println("At least indexin: " + usage);
			System.exit(1);
		}

		if ((field == null) && (query == null) && (hilos1 == null) && (hilos2 == null)) {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}

		if (((hilos1 != null) || (hilos2 != null)) && (indexout == null)) {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}
	}

	private static String usage = "java ri.p1.ProcessAndIndex"
			+ " [-indexin INDEXFILE] [-indexout INDEXFILE] [-deldocsterm TERM] [-deldocsquery QUERY]\n"
			+ " [-mostsimilardoc_title Hilos]\n0" + " [-mostsimilardoc_title N HEADER]\n\n";

	public static void mostSimilarDocTitle(String indexin, String indexout, int h) throws IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;

		Directory dir2 = FSDirectory.open(Paths.get(indexout));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dir2, iwc);

		try {
			dir = FSDirectory.open(Paths.get(indexin));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		}
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		int n = indexReader.numDocs();
		if (h > 1) {
			double threads = h;
			double docs = n;
			double docsThread = Math.ceil(docs / threads);

			final ExecutorService executor = Executors.newFixedThreadPool(h);
			System.out.println("Creating " + h + " threads");
			int index = 0;
			for (int i = 0; i < h - 1; i++) {
				final Runnable worker = new ThreadPool2.WorkerThread(writer, indexReader, indexSearcher, index,
						(int) docsThread, true, 0);
				executor.execute(worker);
				index += docsThread;
			}
			final Runnable worker = new ThreadPool2.WorkerThread(writer, indexReader, indexSearcher, index,
					(n - ((int) docsThread) * (h - 1)), true, 0);
			executor.execute(worker);
			executor.shutdown();

			/*
			 * Wait up to 1 hour to finish all the previously submitted jobs
			 */
			try {
				executor.awaitTermination(1, TimeUnit.HOURS);
			} catch (final InterruptedException e) {
				e.printStackTrace();
				System.exit(-2);
			}
		} else {
			ThreadPool2.process(writer, indexReader, indexSearcher, 0, n);
		}

		System.out.println("Index created successfully");
		writer.close();
	}

	public static void mostSimilarDocBody(String indexin, String indexout, int top, int h) throws IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;

		Directory dir2 = FSDirectory.open(Paths.get(indexout));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dir2, iwc);

		try {
			dir = FSDirectory.open(Paths.get(indexin));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		}
		int n = indexReader.numDocs();
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		if (h > 1) {
			double threads = h;
			double docs = n;
			double docsThread = Math.ceil(docs / threads);
			//TODO: Hacer que funcione con 1 thread (y que cuente bien los threads omg)
			final ExecutorService executor = Executors.newFixedThreadPool(h);
			System.out.println("Creating " + h + " threads");
			int index = 0;
			for (int i = 0; i < h - 1; i++) {
				final Runnable worker = new ThreadPool2.WorkerThread(writer, indexReader, indexSearcher, index,
						(int) docsThread, false, top);
				executor.execute(worker);
				System.out.println("index: " + index + "\tdocsThreads: " + docsThread);
				index += docsThread;
			}
			System.out.println("index: " + index + "\tdocsThreads: " + (n - ((int) docsThread) * (h - 1)));
			final Runnable worker = new ThreadPool2.WorkerThread(writer, indexReader, indexSearcher, index,
					(n - ((int) docsThread) * (h - 1)), false, top);
			executor.execute(worker);
			executor.shutdown();

			/*
			 * Wait up to 1 hour to finish all the previously submitted jobs
			 */
			try {
				executor.awaitTermination(1, TimeUnit.HOURS);
			} catch (final InterruptedException e) {
				e.printStackTrace();
				System.exit(-2);
			}
		} else {
			ThreadPool2.process2(writer, indexReader, indexSearcher, 0, n, top);
		}
		System.out.println("Index created successfully");
		writer.close();
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
		} catch (org.apache.lucene.queryparser.classic.ParseException e) {
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

	public static void delDocsTerm(String indexFolder, String field, String term) {
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

	public static void main(String[] args) throws NumberFormatException, ParseException, IOException {

		if (args.length <= 1) {
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
				hilos1 = args[i + 1];
				i++;
				break;
			case ("-mostsimilardoc_body"):
				n = args[i + 1];
				hilos2 = args[i + 2];
				i++;
				break;
			default:
				break;
			}
		}

		validateArgs();
		Date start = new Date();
		if (field != null) {
			delDocsTerm(indexin, field, term);
		} else if (query != null) {
			delDocsQuery(indexin, query);
		} else if (hilos1 != null) {
			mostSimilarDocTitle(indexin, indexout, Integer.parseInt(hilos1));
		} else if (hilos2 != null) {
			mostSimilarDocBody(indexin, indexout, Integer.parseInt(n), Integer.parseInt(hilos2));
		}
		Date end = new Date();
		System.out.println(end.getTime() - start.getTime() + " total milliseconds");
	}
	
	
}