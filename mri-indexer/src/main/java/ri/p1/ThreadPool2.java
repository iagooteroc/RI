package ri.p1;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;

public class ThreadPool2 {
	
	public static final FieldType TYPE_BODY = new FieldType();
	static final IndexOptions options = IndexOptions.DOCS_AND_FREQS;

	static {
		TYPE_BODY.setIndexOptions(options);
		TYPE_BODY.setTokenized(true);
		TYPE_BODY.setStored(true);
		TYPE_BODY.setStoreTermVectors(true);
		TYPE_BODY.setStoreTermVectorPositions(true);
		TYPE_BODY.freeze();
	}

	public static void process(IndexWriter writer, DirectoryReader indexReader, IndexSearcher indexSearcher, int index,
			int count) throws IOException {
		String docTitle = null;
		QueryParser parserTitle = new QueryParser("TITLE", new StandardAnalyzer());
		QueryParser parserBody = new QueryParser("BODY", new StandardAnalyzer());
		
		for (int i = index; i < (index + count); i++) {
			if (i % 100 == 0)
				System.out.println(Thread.currentThread().getName() + "\tindex = " + i + "\t" + (index + count - i) + " to go");
			Document doc = indexReader.document(i);
			docTitle = doc.get("TITLE");
			Query queryTitle = null;
			Query queryBody = null;
			try {
				queryTitle = parserTitle.parse(QueryParser.escape(docTitle));
				queryBody = parserBody.parse(QueryParser.escape(docTitle));
			} catch (ParseException e) {
				writer.addDocument(doc);
				continue;
			}
			BooleanQuery booleanQuery = new BooleanQuery.Builder()
					.add(queryTitle, BooleanClause.Occur.SHOULD)
					.add(queryBody, BooleanClause.Occur.SHOULD).build();
			// 2 porque probablemente saque el mismo doc
			TopDocs topDocs = indexSearcher.search(booleanQuery, 2);
			int top1 = topDocs.scoreDocs[0].doc;
			if (topDocs.scoreDocs.length > 1) {
				int top2 = topDocs.scoreDocs[1].doc;
				Document doc2 = indexSearcher.doc(top2);
				String title2 = doc2.get("TITLE");
				String body2 = doc2.get("BODY");
				String pathSgm2 = doc2.get("PathSgm");
				doc.add(new TextField("SimTitle2", title2, Field.Store.NO));
				doc.add(new TextField("SimBody2", body2, Field.Store.NO));
				doc.add(new StringField("SimPathSgm2", pathSgm2, Field.Store.YES));
			}
			Document doc1 = indexSearcher.doc(top1);

			String title1 = doc1.get("TITLE");
			String body1 = doc1.get("BODY");
			String pathSgm1 = doc1.get("PathSgm");
			doc.add(new TextField("SimTitle1", title1, Field.Store.NO));
			doc.add(new TextField("SimBody1", body1, Field.Store.NO));
			doc.add(new StringField("SimPathSgm1", pathSgm1, Field.Store.YES));
			//TODO: actualizar campo thread?
			doc.removeField("Thread");
			doc.add(new StringField("Thread", Thread.currentThread().getName(), Field.Store.YES));
			writer.addDocument(doc);
		}

	}
	
	public static void process2(IndexWriter writer, IndexReader indexReader, IndexSearcher indexSearcher, int index,
			int count, int top) throws IOException {
		QueryParser parserTitle = new QueryParser("TITLE", new StandardAnalyzer());
		QueryParser parserBody = new QueryParser("BODY", new StandardAnalyzer());
		int n = indexReader.numDocs();

		ArrayList<TermCompact> tl = new ArrayList<>();

		for (int i = index; i < (index + count); i++) {
			if (i % 100 == 0)
				System.out.println(Thread.currentThread().getName() + "\tindex = " + i + "\t" + (index + count - i) + " to go");
			Document doc = indexReader.document(i);
			Terms vector = indexReader.getTermVector(i, "BODY");
			if (vector==null) {
				writer.addDocument(doc);
				continue;
			}
			TermsEnum termsEnum = null;
			termsEnum = vector.iterator();
			while (termsEnum.next() != null) {
				BytesRef br = termsEnum.term();
				final String tt = br.utf8ToString();
				double df_t = termsEnum.docFreq();
				double idf = Math.log(n / df_t);
				double tf = 1 + Math.log(termsEnum.totalTermFreq());
				
				tl.add(new TermCompact(tt, idf, tf));
			}
			Collections.sort(tl, new TermCComparatorTfIdf());
			String query = "";
			for (int j = 1; j <= Math.min(n, tl.size()); j++) {
				query +=(tl.get(j - 1).getTerm() + " ");
			}
			
			Query queryTitle = null;
			Query queryBody = null;
			try {
				queryTitle = parserTitle.parse(QueryParser.escape(query));
				queryBody = parserBody.parse(QueryParser.escape(query));
			} catch (ParseException e) {
				continue;
			}
			
			BooleanQuery booleanQuery = new BooleanQuery.Builder().add(queryTitle, BooleanClause.Occur.SHOULD)
					.add(queryBody, BooleanClause.Occur.SHOULD).build();
			
			doc.add(new StringField("SimQuery", query, Field.Store.YES));
			
			TopDocs topDocs = indexSearcher.search(booleanQuery, 2); // 2 porque probablemente saque el mismo doc
			for (int k = 0; k < Math.min(2, topDocs.scoreDocs.length); k++) {
				Document tDoc = indexReader.document(topDocs.scoreDocs[k].doc);
				doc.add(new TextField("SimTitle", tDoc.get("TITLE"), Field.Store.NO));
				doc.add(new TextField("SimBody", tDoc.get("BODY"), Field.Store.NO));
				doc.add(new StringField("SimPathSgm", tDoc.get("PathSgm"), Field.Store.YES));
			}
			writer.addDocument(doc);
		}

	}

	/**
	 * This Runnable takes a folder and prints its path.
	 */
	public static class WorkerThread implements Runnable {

		private final IndexWriter writer;
		private final DirectoryReader indexReader;
		private final IndexSearcher indexSearcher;
		private final int index;
		private final int count;
		private final boolean option;
		private final int top;

		public WorkerThread(IndexWriter writer, DirectoryReader indexReader, IndexSearcher indexSearcher, int index,
				int count, boolean option, int top) {
			this.writer = writer;
			this.indexReader = indexReader;
			this.indexSearcher = indexSearcher;
			this.index = index;
			this.count = count;
			this.option = option;
			this.top = top;
		}

		/**
		 * This is the work that the current thread will do when processed by
		 * the pool. In this case, it will only print some information.
		 */
		@Override
		public void run() {
			// System.out.println(String.format("I am the thread '%s' and I am
			// responsible for folder '%s'",
			// Thread.currentThread().getName(), folder));
			System.out.println(String.format("I am the thread '%s'", Thread.currentThread().getName()));
			try {
				if (option) {
					process(writer, indexReader, indexSearcher, index, count);
				} else {
					process2(writer, indexReader, indexSearcher, index, count, top);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static void main(final String[] args) {

		if (args.length != 1) {
			System.out.println("Usage: java ThreadPool folder");
			return;
		}

		/*
		 * Create a ExecutorService (ThreadPool is a subclass of
		 * ExecutorService) with so many thread as cores in my machine. This can
		 * be tuned according to the resources needed by the threads.
		 */
		final int numCores = Runtime.getRuntime().availableProcessors();
		final ExecutorService executor = Executors.newFixedThreadPool(numCores);

		/*
		 * We use Java 7 NIO.2 methods for input/output management. More info
		 * in: http://docs.oracle.com/javase/tutorial/essential/io/fileio.html
		 *
		 * We also use Java 7 try-with-resources syntax. More info in:
		 * https://docs.oracle.com/javase/tutorial/essential/exceptions/
		 * tryResourceClose.html
		 */
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(args[0]))) {

			/* We process each subfolder in a new thread. */
			for (final Path path : directoryStream) {
				if (Files.isDirectory(path)) {
					//final Runnable worker = new WorkerThread(path);
					/*
					 * Send the thread to the ThreadPool. It will be processed
					 * eventually.
					 */
					//executor.execute(worker);
				}
			}

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		/*
		 * Close the ThreadPool; no more jobs will be accepted, but all the
		 * previously submitted jobs will be processed.
		 */
		executor.shutdown();

		/* Wait up to 1 hour to finish all the previously submitted jobs */
		try {
			executor.awaitTermination(1, TimeUnit.HOURS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
			System.exit(-2);
		}

		System.out.println("Finished all threads");

	}

}