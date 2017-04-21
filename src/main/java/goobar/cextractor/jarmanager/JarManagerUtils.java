/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package goobar.cextractor.jarmanager;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.jar.JarEntry;

/**
 *
 * @author goobar
 */
@SuppressWarnings("javadoc")
public class JarManagerUtils
{

	public static final String ENTRY_DIRECTORY_SUFFIX = "/";

	public static final String JAR_EXTENSION = ".jar";

	public static final String JAVA_CLASS_FILE_EXTENSION = ".class";

	public static String convertCanonicalNameToEntryName(
		String canonicalName)
	{
		String entryName = canonicalName.replace(".", "/");
		return entryName;
	}

	public static String convertEntryToCanonicalName(JarEntry entry)
	{
		String entryName = entry.getName();
		String result = entryName.replace("/", ".");
		return result;
	}

	public static Path convertEntryToPath(JarEntry entry, Path root)
	{
		Path result = root;
		String[] subentries = entry.getName()
			.split(ENTRY_DIRECTORY_SUFFIX);
		for (String subentrty : subentries)
		{
			result = result.resolve(subentrty);
		}
		return result;
	}

	public static JarEntry convertPathToEntry(Path path)
	{
		JarEntry entry = new JarEntry(path.toString().replace(
			FileSystems.getDefault().getSeparator(),
			ENTRY_DIRECTORY_SUFFIX));
		return entry;
	}

	static Path convertEntryToRelativePath(JarEntry entry)
	{
		Path result = FileSystems.getDefault().getPath("");
		String[] subentries = entry.getName()
			.split(ENTRY_DIRECTORY_SUFFIX);
		for (String subentrty : subentries)
		{
			result = result.resolve(subentrty);
		}
		return result;
	}
}
