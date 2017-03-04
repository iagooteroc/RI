package indexTest.indexTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;

/**
 * The Class ThreadPoolExample. It print the name of each subfolder using a
 * different thread.
 */
public class ThreadPoolExample {

	/**
	 * This Runnable takes a folder and prints its path.
	 */
	public static class WorkerThread implements Runnable {

		private final IndexWriter writer;
		private final Path file;
		private final String hostname;

		public WorkerThread(IndexWriter writer, Path file, String hostname) {
			this.writer = writer;
			this.file = file;
			this.hostname = hostname;
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
			/** Indexes a single document */

			try (InputStream stream = Files.newInputStream(file)) {
				// make a new, empty document
				Document doc = new Document();

				String str = IOUtils.toString(stream, "UTF-8");
				StringBuffer strBuffer = new StringBuffer(str);
				List<List<String>> documents = Reuters21578Parser.parseString(strBuffer);
				String field;
				List<Document> docList = new LinkedList<>();
				int seqDocNumber = 1;
				for (List<String> document : documents) {
					int i = 0;
					field = document.get(i++);
					doc.add(new TextField("TITLE", field, Field.Store.NO));
					field = document.get(i++);
					doc.add(new TextField("BODY", field, Field.Store.NO));
					field = document.get(i++);
					doc.add(new TextField("TOPICS", field, Field.Store.NO));
					field = document.get(i++);
					doc.add(new TextField("DATELINE", field, Field.Store.NO));
					field = document.get(i++);
					doc.add(new TextField("DATE", field, Field.Store.NO));
					// Add the path of the file as a field named "path". Use a
					// field that is indexed (i.e. searchable), but don't
					// tokenize
					// the field into separate words and don't index term
					// frequency
					// or positional information:
					doc.add(new StringField("PathSgm", file.toString(), Field.Store.YES));
					doc.add(new StoredField("SeqDocNumer", seqDocNumber));
					doc.add(new StringField("Hostname", hostname, Field.Store.YES));
					// doc.add(new StoredField("Thread", x));
					System.out.println("Adding doc...");
					docList.add(doc);
					doc = new Document();
				}

				// Add the last modified date of the file a field named
				// "modified".
				// Use a LongPoint that is indexed (i.e. efficiently filterable
				// with
				// PointRangeQuery). This indexes to milli-second resolution,
				// which
				// is often too fine. You could instead create a number based on
				// year/month/day/hour/minutes/seconds, down the resolution you
				// require.
				// For example the long value 2011021714 would mean
				// February 17, 2011, 2-3 PM.
				// doc.add(new LongPoint("modified", lastModified));

				// Add the contents of the file to a field named "contents".
				// Specify
				// a Reader,
				// so that the text of the file is tokenized and indexed, but
				// not
				// stored.
				// Note that FileReader expects the file to be in UTF-8
				// encoding.
				// If that's not the case searching for special characters will
				// fail.
				// doc.add(new TextField("contents",
				// new BufferedReader(new InputStreamReader(stream,
				// StandardCharsets.UTF_8))));

				
				if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
					// New index, so we just add the document (no old document
					// can
					// be there):
					for (Document docf : docList) {
						System.out.println("adding " + file);
						writer.addDocument(docf);
					}
				} else {
					// Existing index (an old copy of this document may have
					// been
					// indexed) so
					// we use updateDocument instead to replace the old one
					// matching
					// the exact
					// path, if present:
					for (Document docf : docList) {
						System.out.println("updating " + file);
						// TODO: cambiar esto
						writer.updateDocument(new Term("PathSgm", file.toString()), docf);
					}
				}
			} catch (IOException e) {
				System.out.println("Fuck");
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