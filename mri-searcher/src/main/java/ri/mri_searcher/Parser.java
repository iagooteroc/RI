package ri.mri_searcher;

import java.util.LinkedList;
import java.util.List;

public class Parser {
	public static List<List<String>> parseString(StringBuffer fileContent) {
		/* First the contents are converted to a string */
		String text = fileContent.toString();

		/*
		 * The method split of the String class splits the strings using the
		 * delimiter which was passed as argument Therefor lines is an array of
		 * strings, one string for each line
		 */
		String[] lines = text.split("\n");

		/*
		 * For each Reuters article the parser returns a list of strings where
		 * each element of the list is a field (TITLE, BODY, TOPICS, DATELINE).
		 * Each *.sgm file that is passed in fileContent can contain many
		 * Reuters articles, so finally the parser returns a list of list of
		 * strings, i.e, a list of reuters articles, that is what the object
		 * documents contains
		 */

		List<List<String>> documents = new LinkedList<List<String>>();
		System.out.println(lines.length);

		/* The tag REUTERS identifies the beginning and end of each article */
		int i = 0;
		while (i < lines.length) {
			if (!lines[i].startsWith(".I")) {
				continue;
			}
			StringBuilder sb = new StringBuilder();
			sb.append(lines[i++]);
			while ((i < lines.length) && !lines[i].startsWith(".I")) {
				sb.append(lines[i++]);
				sb.append("\n");
			}
			/*
			 * Here the sb object of the StringBuilder class contains the
			 * Reuters article which is converted to text and passed to the
			 * handle document method that will return the document in the form
			 * of a list of fields
			 */
			documents.add(handleDocument(sb.toString()));
		}
		return documents;
	}

	public static List<String> handleDocument(String text) {

		System.out.println(text);
		/*
		 * This method returns the Reuters article that is passed as text as a
		 * list of fields
		 */

		/* The fields TOPICS, TITLE, DATELINE and BODY are extracted */
		/* Each topic inside TOPICS is identified with a tag D */
		/* If the BODY ends with boiler plate text, this text is removed */

		String i = extract("I", "T", text, true);
		String t = extract("T", "A", text, true);
		String a = extract("A", "B", text, true);
		String b = extract("B", "W", text, true);
		String w = extract("W", "I", text, true);
		List<String> document = new LinkedList<String>();
		document.add(i);
		document.add(t);
		document.add(a);
		document.add(b);
		document.add(w);
		return document;
	}

	private static String extract(String elt, String elt2, String text, boolean allowEmpty) {

		/*
		 * This method find the tags for the field elt in the String text and
		 * extracts and returns the content
		 */
		/*
		 * If the tag does not exists and the allowEmpty argument is true, the
		 * method returns the null string, if allowEmpty is false it returns a
		 * IllegalArgumentException
		 */

		String startElt = "." + elt;
		String endElt = "." + elt2;
		int startEltIndex = text.indexOf(startElt);
		if (startEltIndex < 0) {
			if (allowEmpty)
				return "";
			throw new IllegalArgumentException("no start, elt=" + elt + " text=" + text);
		}
		int start = startEltIndex + startElt.length();
		int end = text.indexOf(endElt, start);
		if (end < 0)
			return text.substring(start);
		return text.substring(start, end);
	}
}
