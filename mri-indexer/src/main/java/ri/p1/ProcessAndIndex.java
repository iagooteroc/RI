package ri.p1;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.BytesRef;

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

	public static void mostSimilarDocTitle(String indexin, String indexout, int h) throws IOException {
		String docTitle = null;
		Directory dir = null;
		DirectoryReader indexReader = null;

		Directory dir2 = FSDirectory.open(Paths.get(indexout));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		IndexWriter writer = new IndexWriter(dir2, iwc);
		List<Document> docList = new LinkedList<>();
		// TODO: hacerlo con h threads

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
		QueryParser parserTitle = new QueryParser("TITLE", new StandardAnalyzer());
		QueryParser parserBody = new QueryParser("BODY", new StandardAnalyzer());

		int n = indexReader.maxDoc(); // TODO: porque cuentan los eliminados,
										// no?
		Document nDoc = new Document();
		int seqDocNumber = 1;
		for (int i = 0; i < n; i++) {
			System.out.println(i);
			Document doc = indexReader.document(i);
			docTitle = doc.get("TITLE");
			Query queryTitle = null;
			Query queryBody = null;
			try {
				queryTitle = parserTitle.parse(QueryParser.escape(docTitle));
				queryBody = parserBody.parse(QueryParser.escape(docTitle));
			} catch (ParseException e) {
				continue;
			}
			BooleanQuery booleanQuery = new BooleanQuery.Builder().add(queryTitle, BooleanClause.Occur.SHOULD)
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
				nDoc.add(new TextField("SimTitle2", title2, Field.Store.YES));
				nDoc.add(new TextField("SimBody2", body2, Field.Store.YES));
				nDoc.add(new StringField("SimPathSgm2", pathSgm2, Field.Store.YES));
			}
			Document doc1 = indexSearcher.doc(top1);

			String title1 = doc1.get("TITLE");
			String body1 = doc1.get("BODY");
			String pathSgm1 = doc1.get("PathSgm");

			nDoc.add(new TextField("TITLE", docTitle, Field.Store.YES));
			nDoc.add(new Field("BODY", doc.get("BODY"), TYPE_BODY));
			nDoc.add(new TextField("TOPICS", doc.get("TOPICS"), Field.Store.YES));
			nDoc.add(new StringField("DATELINE", doc.get("DATELINE"), Field.Store.YES));
			nDoc.add(new StringField("DATE", doc.get("DATE"), Field.Store.YES));
			nDoc.add(new StringField("PathSgm", doc.get("PathSgm"), Field.Store.YES));
			nDoc.add(new IntPoint("SeqDocNumer", seqDocNumber++));
			nDoc.add(new StringField("Hostname", doc.get("Hostname"), Field.Store.YES));
			nDoc.add(new StringField("Thread", Thread.currentThread().getName(), Field.Store.YES));
			nDoc.add(new TextField("SimTitle1", title1, Field.Store.YES));
			nDoc.add(new TextField("SimBody1", body1, Field.Store.YES));
			nDoc.add(new StringField("SimPathSgm1", pathSgm1, Field.Store.YES));
			docList.add(nDoc);
		}
		int i = 0;
		for (Document docf : docList) {
			System.out.println("Adding doc nº" + i);
			writer.addDocument(docf);
			i++;
		}
		System.out.println("Index created successfully");
		writer.close();
	}

	public static void mostSimilarDocBody(String indexin, String indexout, int n, int h) throws IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;
		ArrayList<Termino> tl = new ArrayList<>();

		Directory dir2 = FSDirectory.open(Paths.get(indexout));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		IndexWriter writer = new IndexWriter(dir2, iwc);
		List<Document> docList = new LinkedList<>();
		// TODO: hacerlo con h threads

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
		QueryParser parserTitle = new QueryParser("TITLE", new StandardAnalyzer());
		QueryParser parserBody = new QueryParser("BODY", new StandardAnalyzer());

		int nDocs = indexReader.maxDoc(); // TODO: porque cuentan los eliminados, no?
		Document nDoc = new Document();
		int seqDocNumber = 1;
		for (int i = 0; i < nDocs; i++) {
			System.out.println(i);
			Terms vector = indexReader.getTermVector(i, "BODY");
			TermsEnum termsEnum = null;
			termsEnum = vector.iterator();
			while (termsEnum.next() != null) {
				BytesRef br = termsEnum.term();
				final String tt = br.utf8ToString();
				double df_t = termsEnum.docFreq();
				double idf = Math.log(n / df_t);
				PostingsEnum pe = MultiFields.getTermPositionsEnum(indexReader, "BODY", br);
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
			Collections.sort(tl, new TermComparatorTfIdf());
			Collections.reverse(tl);
			String query = "";
			for (int j = 1; j <= Math.min(n, tl.size()); j++) {
				Termino term = tl.get(j - 1);
				query = query.concat(term.getTerm() + " ");
			}
			Document doc = indexReader.document(i);
			Query queryTitle = null;
			Query queryBody = null;
			try {
				queryTitle = parserTitle.parse(query);
				queryBody = parserBody.parse(query);
			} catch (ParseException e) {
				continue;
			}
			
			BooleanQuery booleanQuery = new BooleanQuery.Builder().add(queryTitle, BooleanClause.Occur.SHOULD)
					.add(queryBody, BooleanClause.Occur.SHOULD).build();
			
			nDoc.add(new StringField("SimQuery", query, Field.Store.YES));
			
			TopDocs topDocs = indexSearcher.search(booleanQuery, 2); // 2 porque probablemente saque el mismo doc
			int top1 = topDocs.scoreDocs[0].doc;
			if (topDocs.scoreDocs.length > 1) {
				int top2 = topDocs.scoreDocs[1].doc;
				Document doc2 = indexSearcher.doc(top2);
				String title2 = doc2.get("TITLE");
				String body2 = doc2.get("BODY");
				String pathSgm2 = doc2.get("PathSgm");
				nDoc.add(new TextField("SimTitle2", title2, Field.Store.YES));
				nDoc.add(new TextField("SimBody2", body2, Field.Store.YES));
				nDoc.add(new StringField("SimPathSgm2", pathSgm2, Field.Store.YES));
			}
			Document doc1 = indexSearcher.doc(top1);

			String title1 = doc1.get("TITLE");
			String body1 = doc1.get("BODY");
			String pathSgm1 = doc1.get("PathSgm");

			nDoc.add(new TextField("TITLE", doc.get("TITLE"), Field.Store.YES));
			nDoc.add(new Field("BODY", doc.get("BODY"), TYPE_BODY));
			nDoc.add(new TextField("TOPICS", doc.get("TOPICS"), Field.Store.YES));
			nDoc.add(new StringField("DATELINE", doc.get("DATELINE"), Field.Store.YES));
			nDoc.add(new StringField("DATE", doc.get("DATE"), Field.Store.YES));
			nDoc.add(new StringField("PathSgm", doc.get("PathSgm"), Field.Store.YES));
			nDoc.add(new IntPoint("SeqDocNumer", seqDocNumber++));
			nDoc.add(new StringField("Hostname", doc.get("Hostname"), Field.Store.YES));
			nDoc.add(new StringField("Thread", Thread.currentThread().getName(), Field.Store.YES));
			nDoc.add(new TextField("SimTitle1", title1, Field.Store.YES));
			nDoc.add(new TextField("SimBody1", body1, Field.Store.YES));
			nDoc.add(new StringField("SimPathSgm1", pathSgm1, Field.Store.YES));
			docList.add(nDoc);
		}
		int i = 0;
		for (Document docf : docList) {
			System.out.println("Adding doc nº" + i);
			writer.addDocument(docf);
			i++;
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