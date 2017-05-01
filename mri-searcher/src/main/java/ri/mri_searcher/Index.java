package ri.mri_searcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Index {

	private static void setOpenMode(IndexWriterConfig iwc, String openMode) {
		switch (openMode) {
		case "append":
			iwc.setOpenMode(OpenMode.APPEND);
			break;
		case "create":
			// Create a new index in the directory, removing any
			// previously indexed documents:
			iwc.setOpenMode(OpenMode.CREATE);
			break;
		case "create_or_append":
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			break;
		}
	}

	private static void setSimilarity(IndexWriterConfig iwc, String similarity, String lambdaormu) {
		switch (similarity) {
		case "default":
			iwc.setSimilarity(new BM25Similarity());
			break;
		case "jm":
			iwc.setSimilarity(new LMJelinekMercerSimilarity(Float.parseFloat(lambdaormu)));
			break;
		case "dir":
			iwc.setSimilarity(new LMDirichletSimilarity(Float.parseFloat(lambdaormu)));
			break;
		}

	}

	public static void indexation(String collPath, String indexPath, String filename, String openMode, String similarity, String lambdaormu) throws IOException {
		Path docDir = null;
		List<Path> docDirList = null;
		if (collPath != null) {
			docDir = Paths.get(collPath);
			if (!Files.isReadable(docDir)) {
				System.out.println("Document directory '" + docDir.toAbsolutePath()
						+ "' does not exist or is not readable, please check the path");
				System.exit(1);
			}
		}
		Date start = new Date();
		index(docDir, docDirList, filename, similarity, lambdaormu, openMode, indexPath);
		Date end = new Date();
		System.out.println(end.getTime() - start.getTime() + " total milliseconds");
	}

	private static String getHostname() {
		String hostname = "Unknown";
		try {
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			hostname = addr.getHostName();
		} catch (UnknownHostException ex) {
			System.out.println("Hostname can not be resolved");
		}
		return hostname;
	}

	private static void index(Path docDir, List<Path> docDirList, String filename, String similarity, String lambdaormu,
			String openMode, String indexPath) throws IOException {
		Directory dir = FSDirectory.open(Paths.get(indexPath));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		setSimilarity(iwc, similarity, lambdaormu);

		setOpenMode(iwc, openMode);

		String hostname = getHostname();

		IndexWriter writer = new IndexWriter(dir, iwc);
		// if we have only one docDir, we index it
		if (docDir != null) {
			System.out.println("Indexing only " + docDir);
			indexDocs(writer, docDir, hostname, filename);
		} else {
			// else, we index all of them
			for (Path docPath : docDirList) {
				indexDocs(writer, docPath, hostname, filename);
			}
		}

		writer.close();
	}

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

	private static void indexDoc(IndexWriter writer, Path file, String hostname) {
		System.out.println(Thread.currentThread().getId() + " - Indexing " + file);
		try (InputStream stream = Files.newInputStream(file)) {
			Document doc = new Document();

			String str = IOUtils.toString(stream, "UTF-8");
			StringBuffer strBuffer = new StringBuffer(str);
			List<List<String>> documents = Parser.parseString(strBuffer);
			String field;
			for (List<String> document : documents) {
				int i = 0;
				field = document.get(i++);
				doc.add(new TextField("I", field, Field.Store.YES));
				field = document.get(i++);
				doc.add(new TextField("T", field, Field.Store.YES));
				field = document.get(i++);
				doc.add(new TextField("A", field, Field.Store.YES));
				field = document.get(i++);
				doc.add(new TextField("B", field, Field.Store.YES));
				field = document.get(i++);
				doc.add(new Field("W", field, TYPE_BODY));
				writer.addDocument(doc);
				doc = new Document();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected static void indexDocs(final IndexWriter writer, Path path, String hostname, String filename)
			throws IOException {
		System.out.println("indexDocs in " + path);
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					System.out.println("Checking filename: " + file);
					if (file.getFileName().toString().equals(filename)) {
						indexDoc(writer, file, hostname);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			if (path.getFileName().toString().equals(filename)) {
				indexDoc(writer, path, hostname);
			}
		}
	}
}
