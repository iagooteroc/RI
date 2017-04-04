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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Index {

	private static String openMode = null;
	private static String indexPath = "index";
	private static String collPath = null;
	private static List<String> collsPath = null;
	private static String filename = null;
	private static String similarity = null;
	private static String lambdaormu = null;

	/*
	 * Collects the following args until the next option (starting with '-') or
	 * the end of the arguments
	 */
	private static List<String> collectArgs(String[] args, int i) {
		List<String> collectedArgs = new LinkedList<>();
		for (int j = i; j < args.length; j++) {
			if (args[j].startsWith("-"))
				break;
			collectedArgs.add(args[j]);
		}
		return collectedArgs;
	}

	private static List<Path> collListToPathList(List<String> collsPath) {
		List<Path> pathList = new LinkedList<>();
		for (String collPath : collsPath) {
			if (collPath != null) {
				Path docDir = Paths.get(collPath);
				if (!Files.isReadable(docDir)) {
					System.out.println("Warning: Document directory '" + docDir.toAbsolutePath()
							+ "' does not exist or is not readable and will be ignored");
					continue;
				}
				pathList.add(docDir);
			}
		}
		return pathList;
	}

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

	public static void main(String[] args) throws IOException {

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case ("-openmode"):
				openMode = args[i + 1];
				i++;
				break;
			case ("-index"):
				indexPath = args[i + 1];
				i++;
				break;
			case ("-coll"):
				collPath = args[i + 1];
				i++;
				break;
			case ("-colls"):
				collsPath = collectArgs(args, i + 1);
				i += collsPath.size();
				break;
			case ("-filename"):
				filename = args[i + 1];
				i++;
				break;
			case ("-indexingmodel"):
				similarity = args[i + 1];
				lambdaormu = args[i + 2];
				i += 2;
				break;
			default:
				break;
			}
		}

		// Now we have to see which option was provided
		// if we have collPath:
		Path docDir = null;
		List<Path> docDirList = null;
		if (collPath != null) {
			docDir = Paths.get(collPath);
			if (!Files.isReadable(docDir)) {
				System.out.println("Document directory '" + docDir.toAbsolutePath()
						+ "' does not exist or is not readable, please check the path");
				System.exit(1);
			}
		} else {
			// otherwise, we have a list of coll paths
			docDirList = collListToPathList(collsPath);
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

	private static void index(Path docDir, List<Path> docDirList, String filename, String similarity, String lambdaormu, String openMode, String indexPath) throws IOException {
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
				doc.add(new TextField("W", field, Field.Store.YES));
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
