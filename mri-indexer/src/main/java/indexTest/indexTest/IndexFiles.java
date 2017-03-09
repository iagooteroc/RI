package indexTest.indexTest;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import indexTest.indexTest.ThreadPoolExample;

/**
 * Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing. Run
 * it with no command-line arguments for usage information.
 */
public class IndexFiles {
	private static String openMode = null;
	private static String indexPath = "index";
	private static String collPath = null;
	private static List<String> collsPath = null;
	private static List<String> indexes1Path = null;
	private static String indexes2Path = null;

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

	private static void validateArgs() {
		// Check if openMode is valid
		if (openMode == null
				|| (!openMode.equals("create") && !openMode.equals("append") && !openMode.equals("create_or_append"))) {
			System.err.println("openmode " + openMode + " is invalid: " + usage);
			System.exit(1);
		}

		// Check if coll or colls are valid
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

	private static void indexes1Option(List<String> indexes2Path, List<Path> docDirList) throws IOException {
		// create n-threads, with n being the number of docDirs
		final ExecutorService executor = Executors.newFixedThreadPool(docDirList.size());
		
		List<IndexWriter> writerList = new LinkedList<>();
		Directory[] dirList = new Directory[docDirList.size()];
		
		for (int i = 1; i < indexes2Path.size(); i++) {
			Directory dir = FSDirectory.open(Paths.get(indexes2Path.get(i)));
			dirList[i-1] = dir;
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

			setOpenMode(iwc, "create"); // Debe ser create porque los pathname1..N pueden no existir

			String hostname = getHostname();

			IndexWriter writer = new IndexWriter(dir, iwc);
			writerList.add(writer);
			
			final Runnable worker = new ThreadPoolExample.WorkerThread(writer, docDirList.get(i-1), hostname);
			executor.execute(worker);
		}
		/*
		 * Close the ThreadPool; no more jobs will be accepted, but all the
		 * previously submitted jobs will be processed.
		 */
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

		System.out.println("Finished all threads");

		for (IndexWriter writer: writerList)
			writer.close();
		
		Directory dir = FSDirectory.open(Paths.get(indexes2Path.get(0)));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		setOpenMode(iwc, openMode);

		IndexWriter writer = new IndexWriter(dir, iwc);
		writer.addIndexes(dirList);
		writer.close();
		
		System.out.println("-> " + indexes2Path.get(0) + " created");
		
	}

	private static void indexes2Option(List<Path> docDirList) throws IOException {
		Directory dir = FSDirectory.open(Paths.get(indexes2Path));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		setOpenMode(iwc, openMode);

		String hostname = getHostname();

		IndexWriter writer = new IndexWriter(dir, iwc);

		// create n-threads, with n being the number of docDirs
		final ExecutorService executor = Executors.newFixedThreadPool(docDirList.size());
		for (Path docPath : docDirList) {
			final Runnable worker = new ThreadPoolExample.WorkerThread(writer, docPath, hostname);
			executor.execute(worker);
		}

		/*
		 * Close the ThreadPool; no more jobs will be accepted, but all the
		 * previously submitted jobs will be processed.
		 */
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

		System.out.println("Finished all threads");

		writer.close();
	}

	private static void indexOption(Path docDir, List<Path> docDirList) throws IOException {
		Directory dir = FSDirectory.open(Paths.get(indexPath));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		setOpenMode(iwc, openMode);

		String hostname = getHostname();

		IndexWriter writer = new IndexWriter(dir, iwc);
		// if we have only one docDir, we index it
		if (docDir != null) {
			System.out.println("Indexing only " + docDir);
			// TODO: move indexDocs and indexDoc to a new class
			ThreadPoolExample.indexDocs(writer, docDir, hostname);
		} else {
			// else, we index all of them

			for (Path docPath : docDirList) {
				ThreadPoolExample.indexDocs(writer, docPath, hostname);
			}
		}

		writer.close();
	}

	private IndexFiles() {
	}

	private static String usage = "java org.apache.lucene.demo.IndexFiles"
			+ " [-openmode OPENMODE] [-index PATH] [-coll PATH] [-colls PATH1 PATH2 ... PATHN] \n"
			+ " [-indexes1 PATH0 PATH1 ... PATHN] [-indexes2 PATH0]\n\n";

	public static void main(String[] args) {

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
			case ("-indexes1"):
				indexes1Path = collectArgs(args, i + 1);
				i += indexes1Path.size();
				break;
			case ("-indexes2"):
				indexes2Path = args[i + 1];
				i++;
				break;
			default:
				break;
			}
		}

		validateArgs();

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
		try {
			// System.out.println("Indexing to directory '" + indexPath +
			// "'...");

			if (indexes1Path != null) {
				System.out.println("*Multiple threads multiple indexes option*");
				// TODO
				indexes1Option(indexes1Path, docDirList);
			} else if (indexes2Path != null) {
				System.out.println("*Multiple threads one index option*");
				indexes2Option(docDirList);
			} else {
				System.out.println("*No concurrency option*");
				indexOption(docDir, docDirList);
			}

			Date end = new Date();
			System.out.println(end.getTime() - start.getTime() + " total milliseconds");

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}
	}

}
