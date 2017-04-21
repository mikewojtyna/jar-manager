/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package goobar.cextractor.jarmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Memory implementation of {@link JarManager} interface. Any modifications to
 * archive are never persisted. This implementation is based on {@link Map}
 * interface. This class is useful when building or updating archive without
 * need to immediately persisting it. Method {@link #save()} is not implemented
 * (it throws {@link UnsupportedOperationException}).
 *
 * @author goobar
 */
public class VirtualJarManager implements JarManager
{

	private static Logger logger = LoggerFactory
		.getLogger(VirtualJarManager.class);

	private Manifest manifest;

	private final Map<JarEntry, BytesWrapper> virtualArchiveContent;

	@SuppressWarnings("javadoc")
	public VirtualJarManager()
	{
		virtualArchiveContent = new HashMap<JarEntry, BytesWrapper>();
		manifest = new Manifest();
	}

	@Override
	public void addClass(Class<?> clazz) throws ClassNotFoundException,
		ArchiveModificationException, EntryDuplicatedException
	{
		try
		{
			File classFile = findClassFile(clazz);
			try
			{
				addEntry(new JarEntry(
					convertCanonicalNameToEntryName(
						clazz.getPackage().getName())
						+ JarManagerUtils.ENTRY_DIRECTORY_SUFFIX));
			}
			catch (EntryDuplicatedException ex)
			{
				logDebug(ex.getMessage(), ex);
			}
			addFile(classFile,
				new JarEntry(convertCanonicalNameToEntryName(
					clazz.getCanonicalName())
					+ JarManagerUtils.JAVA_CLASS_FILE_EXTENSION));
		}
		catch (FileNotFoundException ex)
		{
			throw new ClassNotFoundException(ex.getMessage(), ex);
		}
		catch (URISyntaxException ex)
		{
			throw new ArchiveModificationException(ex.getMessage(),
				ex);
		}
	}

	@Override
	public void addContent(JarEntry entry, BytesWrapper bytes)
		throws ArchiveModificationException, EntryDuplicatedException
	{
		checkIsEntryDuplicated(entry);
		checkIfContent(entry);
		virtualArchiveContent.put(entry, bytes);
	}

	@Override
	public void addDirectory(Path dir)
		throws ArchiveModificationException, EntryDuplicatedException
	{
		try
		{
			SimpleFileVisitorImpl visitor = new SimpleFileVisitorImpl(
				dir);
			Files.walkFileTree(dir, visitor);

			ArchiveModificationException archiveModificationException = visitor
				.getArchiveModificationException();
			EntryDuplicatedException entryDuplicatedException = visitor
				.getEntryDuplicatedException();
			FileNotFoundException fileNotFoundException = visitor
				.getFileNotFoundException();

			if (archiveModificationException != null)
			{
				throw archiveModificationException;
			}
			if (entryDuplicatedException != null)
			{
				throw entryDuplicatedException;
			}
			if (fileNotFoundException != null)
			{
				throw new ArchiveModificationException(
					fileNotFoundException.getMessage());
			}
		}
		catch (IOException ex)
		{
			throw new ArchiveModificationException(String.format(
				"Cannot add directory %s because of I/O errors. Reason: %s",
				ex.toString()), ex);
		}
	}

	@Override
	public void addEntry(JarEntry entry)
		throws ArchiveModificationException, EntryDuplicatedException
	{
		if (checkIsEntryDuplicated(entry))
		{
			throwEntryDuplicatedException(entry);
		}
		checkIfDirectory(entry);
		virtualArchiveContent.put(entry, null);
	}

	@Override
	public void addFile(File file, JarEntry entry)
		throws ArchiveModificationException, FileNotFoundException,
		EntryDuplicatedException
	{
		FileInputStream fis = null;
		try
		{
			checkIsEntryDuplicated(entry);
			checkIfContent(entry);
			fis = new FileInputStream(file);
			byte[] content = IOUtils.toByteArray(fis);
			BytesWrapper bytesWrapper = new BytesWrapper(content);
			virtualArchiveContent.put(entry, bytesWrapper);
		}
		catch (FileNotFoundException ex)
		{
			throw new FileNotFoundException(ex.getMessage());
		}
		catch (IOException ex)
		{
			throw new ArchiveModificationException(ex.getMessage());
		}
		finally
		{
			try
			{
				if (fis != null)
				{
					fis.close();
				}
			}
			catch (IOException ex)
			{
				logDebug(ex.getMessage(), ex);
			}
		}
	}

	@Override
	public void addManifest(Manifest manifest)
	{
		this.manifest = manifest;
	}

	@Override
	public void addPackage(Class<?> representative)
		throws ArchiveModificationException, ClassNotFoundException,
		EntryDuplicatedException
	{
		try
		{
			File file = findClassFile(representative);
			String path = file.getAbsolutePath();
			Integer separatorIndex = path
				.lastIndexOf(File.separator);
			String packageString = path.substring(0,
				separatorIndex);
			File packageDir = new File(packageString);
			File[] classFiles = packageDir
				.listFiles(new FilenameFilter()
				{
					@Override
					public boolean accept(File dir,
						String name)
					{
						if (name.endsWith(
							JarManagerUtils.JAVA_CLASS_FILE_EXTENSION))
						{
							return true;
						}
						else
						{
							return false;
						}
					}
				});
			for (File classFile : classFiles)
			{
				String packageEntryName = convertCanonicalNameToEntryName(
					representative.getPackage().getName())
					+ JarManagerUtils.ENTRY_DIRECTORY_SUFFIX;
				JarEntry jarEntry = new JarEntry(
					packageEntryName);
				if (!checkIsEntryDuplicated(jarEntry))
				{
					addEntry(jarEntry);
				}
				addFile(classFile, new JarEntry(packageEntryName
					+ classFile.getName()));
			}
		}
		catch (URISyntaxException ex)
		{
			throw new ArchiveModificationException(ex.getMessage(),
				ex);
		}
		catch (FileNotFoundException ex)
		{
			throw new ClassNotFoundException(ex.getMessage(), ex);
		}
	}

	@Override
	public void addPropertiesToManifest(Map<String, String> properties)
		throws ArchiveModificationException
	{
		for (Entry<String, String> property : properties.entrySet())
		{
			manifest.getMainAttributes().putValue(property.getKey(),
				property.getValue());
		}
	}

	@Override
	public List<JarEntry> entries()
	{
		List<JarEntry> entries = new ArrayList<JarEntry>(
			virtualArchiveContent.keySet());

		return entries;
	}

	@Override
	public List<Map.Entry<JarEntry, BytesWrapper>> entriesWithContent()
	{
		List<Map.Entry<JarEntry, BytesWrapper>> result = new ArrayList<Map.Entry<JarEntry, BytesWrapper>>(
			virtualArchiveContent.entrySet());
		return result;
	}

	@Override
	public void extract(Path dir) throws CannotExtractArchiveException
	{
		throw new UnsupportedOperationException(
			"Method extract not implemented");
	}

	@Override
	public Entry<JarEntry, BytesWrapper> findEntryByName(String entryName)
		throws ArchiveReadException
	{
		Entry<JarEntry, BytesWrapper> result = null;
		for (Entry<JarEntry, BytesWrapper> entry : virtualArchiveContent
			.entrySet())
		{
			if (entry.getKey().getName().equals(entryName))
			{
				result = entry;
			}
		}

		return result;
	}

	@Override
	public void generateManifestFlag(boolean generateManifest)
	{
	}

	@Override
	public Manifest manifest()
	{
		return manifest;
	}

	@Override
	public void removeEntry(JarEntry entry)
	{
		virtualArchiveContent.remove(entry);
	}

	@Override
	public void removeEntry(String entryName)
		throws ArchiveModificationException
	{
		Iterator<JarEntry> it = virtualArchiveContent.keySet()
			.iterator();
		while (it.hasNext())
		{
			if (it.next().getName().equals(entryName))
			{
				it.remove();
			}
		}
	}

	@Override
	public void save() throws ArchiveModificationException
	{
		throw new UnsupportedOperationException(
			"Method save not implemented");
	}

	private void checkIfContent(JarEntry entry)
		throws ArchiveModificationException
	{
		if (entry.isDirectory())
		{
			throw new ArchiveModificationException(String.format(
				"Entry %s is a directory, not content",
				entry.getName()));
		}
	}

	private void checkIfDirectory(JarEntry entry)
		throws ArchiveModificationException
	{
		if (!entry.isDirectory())
		{
			throw new ArchiveModificationException(String.format(
				"Entry %s is not a directory but content",
				entry.getName()));
		}
	}

	private boolean checkIsEntryDuplicated(JarEntry entry)
		throws EntryDuplicatedException
	{
		for (Entry<JarEntry, BytesWrapper> entryIter : virtualArchiveContent
			.entrySet())
		{
			if (entryIter.getKey().getName()
				.equals(entry.getName()))
			{
				return true;
			}
		}
		return false;
	}

	private String convertCanonicalNameToEntryName(String canonicalName)
	{
		/*
		 String entryName = canonicalName.replace(".", "/");
		 return entryName;
		 */
		return JarManagerUtils
			.convertCanonicalNameToEntryName(canonicalName);
	}

	private File findClassFile(Class<?> clazz) throws URISyntaxException
	{
		String entryName = convertCanonicalNameToEntryName(
			clazz.getCanonicalName());
		URL url = clazz.getClassLoader()
			.getResource(entryName + ".class");
		File file = new File(url.toURI());
		return file;
	}

	/**
	 * @param message
	 * @param ex
	 */
	private void logDebug(String message, Exception ex)
	{
		logger.debug(message, ex);
	}

	private void throwEntryDuplicatedException(JarEntry entry)
		throws EntryDuplicatedException
	{
		throw new EntryDuplicatedException(
			"Entry " + entry + " already exists in archive");
	}

	private class SimpleFileVisitorImpl extends SimpleFileVisitor<Path>
	{

		private ArchiveModificationException archiveModificationException;

		private EntryDuplicatedException entryDuplicatedException;

		private FileNotFoundException fileNotFoundException;

		private final Path root;

		public SimpleFileVisitorImpl(Path root)
		{
			this.root = root;
		}

		public ArchiveModificationException getArchiveModificationException()
		{
			return archiveModificationException;
		}

		public EntryDuplicatedException getEntryDuplicatedException()
		{
			return entryDuplicatedException;
		}

		public FileNotFoundException getFileNotFoundException()
		{
			return fileNotFoundException;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir,
			BasicFileAttributes attrs) throws IOException
		{
			try
			{
				if (!dir.equals(root))
				{
					Path relativeDir = root.relativize(dir);
					addEntry(new JarEntry(relativeDir
						.toString()
						+ JarManagerUtils.ENTRY_DIRECTORY_SUFFIX));
				}
				return FileVisitResult.CONTINUE;
			}
			catch (ArchiveModificationException ex)
			{
				String msg = String.format(
					"Cannot add directory %s. Archive modification exception. Reason: %s",
					dir.toString(), ex.toString());
				logError(msg, ex);
				archiveModificationException = new ArchiveModificationException(
					msg);
				return FileVisitResult.TERMINATE;
			}
			catch (EntryDuplicatedException ex)
			{
				String msg = String.format(
					"Cannot add directory %s. Entry is duplicated. Reason: %s",
					dir.toString(), ex.toString());
				logError(msg, ex);
				entryDuplicatedException = new EntryDuplicatedException(
					msg);
				return FileVisitResult.TERMINATE;
			}
		}

		@Override
		public FileVisitResult visitFile(Path file,
			BasicFileAttributes attrs) throws IOException
		{
			try
			{
				Path relativeFile = root.relativize(file);
				addFile(file.toFile(), JarManagerUtils
					.convertPathToEntry(relativeFile));
				return FileVisitResult.CONTINUE;
			}
			catch (ArchiveModificationException ex)
			{
				String msg = String.format(
					"Cannot add file %s. Archive modification exception. Reason: %s",
					file.toString(), ex.toString());
				logError(msg, ex);
				archiveModificationException = new ArchiveModificationException(
					msg);
				return FileVisitResult.TERMINATE;
			}
			catch (FileNotFoundException ex)
			{
				String msg = String.format(
					"Cannot add file %s. File not found. Reason: %s",
					file.toString(), ex.toString());
				logError(msg, ex);
				fileNotFoundException = new FileNotFoundException(
					msg);
				return FileVisitResult.TERMINATE;
			}
			catch (EntryDuplicatedException ex)
			{
				String msg = String.format(
					"Cannot add file %s. Entry is duplicated. Reason: %s",
					file.toString(), ex.toString());
				logError(msg, ex);
				entryDuplicatedException = new EntryDuplicatedException(
					msg);
				return FileVisitResult.TERMINATE;
			}
		}

		/**
		 * @param msg
		 * @param ex
		 */
		private void logError(String msg, Exception ex)
		{
			logger.error(msg, ex);
		}
	}
}
