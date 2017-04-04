package ri.mri_searcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;

public class Search {

	private static String similarity = null;
	private static String indexPath = null;
	private static String cut = null;
	private static String top = null;
	private static String queries = null;
	
	private static String queriesPath = "C:\\Users\\iago_\\Desktop\\Universidad\\3.- Recuperación de Información\\P2\\Cranfield\\cran.qry";
	
	private static String searchQueryRaw(String id) throws IOException {
		InputStream stream = Files.newInputStream(Paths.get(queriesPath));
		String str = IOUtils.toString(stream, "UTF-8");
		StringBuffer strBuffer = new StringBuffer(str);
		String text = strBuffer.toString();
		String idStr = String.format("%03d", Integer.parseInt(id));
		int index = text.indexOf(".I " + idStr);
		System.out.println(".I " + idStr);
		int next = Integer.parseInt(id) + 1;
		int start = index + id.length();
		String endStr = String.format("%03d", next); 
		int end = text.indexOf(".I " + endStr, start);
		if (end < 0)
			return text.substring(start);
		return text.substring(start, end);
	}
	
	private static String formatQuery(String queryRaw) {
		String tag = ".W";
		int index = queryRaw.indexOf(tag);
		return queryRaw.substring(index + tag.length());
	}
	
	public static void main(final String[] args) {
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case ("-search"):
				similarity = args[i + 1];
				i++;
				break;
			case ("-indexin"):
				indexPath = args[i + 1];
				i++;
				break;
			case ("-cut"):
				cut = args[i + 1];
				i++;
				break;
			case ("-top"):
				top = args[i + 1];
				break;
			case ("-queries"):
				queries = args[i + 1];
				i++;
				break;
			case ("-fieldsproc"):
				//wat
				break;
			case ("-fieldsvisual"):
				//wat
				break;
			default:
				break;
			}
		}
		
		String queryRaw = null;
		String formatedQuery = null;
		
		try {
			queryRaw = searchQueryRaw("3");
			formatedQuery = formatQuery(queryRaw);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("RAW: \n============================\n" + queryRaw);
		System.out.println("FORMATED: \n============================\n" + formatedQuery);
		
	}
}
