package ri.p1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

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
			for (int k = 0; k < Math.min(2, topDocs.scoreDocs.length); k++) {
				Document tDoc = indexReader.document(topDocs.scoreDocs[k].doc);
				doc.add(new TextField("SimTitle", tDoc.get("TITLE"), Field.Store.YES));
				doc.add(new TextField("SimBody", tDoc.get("BODY"), Field.Store.NO));
				doc.add(new StringField("SimPathSgm", tDoc.get("PathSgm"), Field.Store.YES));
			}
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
			for (int j = 1; j <= Math.min(top, tl.size()); j++) {
				query +=(tl.get(j - 1).getTerm() + " ");
			}
			
			Query queryTitle = null;
			Query queryBody = null;
			try {
				queryTitle = parserTitle.parse(query);
				queryBody = parserBody.parse(query);
			} catch (ParseException e) {
				writer.addDocument(doc);
				continue;
			}
			
			BooleanQuery booleanQuery = new BooleanQuery.Builder().add(queryTitle, BooleanClause.Occur.SHOULD)
					.add(queryBody, BooleanClause.Occur.SHOULD).build();
			
			doc.add(new StringField("SimQuery", query, Field.Store.YES));
			
			TopDocs topDocs = indexSearcher.search(booleanQuery, 2); // 2 porque probablemente saque el mismo doc
			for (int k = 0; k < Math.min(2, topDocs.scoreDocs.length); k++) {
				Document tDoc = indexReader.document(topDocs.scoreDocs[k].doc);
				doc.add(new TextField("SimTitle", tDoc.get("TITLE"), Field.Store.YES));
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
			System.out.println(String.format("I am the thread '%s'", Thread.currentThread().getName()));
			try {
				if (option) {
					process(writer, indexReader, indexSearcher, index, count);
				} else {
					process2(writer, indexReader, indexSearcher, index, count, top);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}