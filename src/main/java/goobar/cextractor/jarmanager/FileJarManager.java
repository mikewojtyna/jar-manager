/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package goobar.cextractor.jarmanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import aQute.lib.osgi.Analyzer;

/**
 * File implementation of {@link JarManager} interface. As name suggests, this
 * implementation works only with files.
 *
 * @author goobar
 */
public class FileJarManager implements JarManager
{

	private static Logger logger = LoggerFactory
		.getLogger(FileJarManager.class);

	private boolean generateManifest;

	private File jarFile;

	private JarOutputStream jarOS;

	private Boolean manifestAdded;

	private VirtualJarManager virtualJarManager;

	/**
	 * Creates file JarManager.
	 *
	 * @param jarFile
	 *                If file already exists its content (excluding
	 *                manifest, which is generated) is copied. Otherwise,
	 *                new archive is created. The archive is persisted when
	 *                {@link #save()} method is called.
	 *
	 * @throws IOException
	 * @throws ArchiveModificationException
	 * @throws EntryDuplicatedException
	 */
	@SuppressWarnings("javadoc")
	public FileJarManager(File jarFile) throws IOException,
		ArchiveModificationException, EntryDuplicatedException
	{
		init(jarFile);
	}

	@Override
	public void addClass(Class<?> clazz) throws ClassNotFoundException,
		ArchiveModificationException, EntryDuplicatedException
	{
		virtualJarManager.addClass(clazz);
	}

	@Override
	public void addContent(JarEntry entry, BytesWrapper bytes)
		throws ArchiveModificationException, EntryDuplicatedException
	{
		virtualJarManager.addContent(entry, bytes);
	}

	@Override
	public void addDirectory(Path dir)
		throws ArchiveModificationException, EntryDuplicatedException
	{
		virtualJarManager.addDirectory(dir);
	}

	@Override
	public void addEntry(JarEntry entry)
		throws ArchiveModificationException, EntryDuplicatedException
	{
		virtualJarManager.addEntry(entry);
	}

	@Override
	public void addFile(File file, JarEntry entry)
		throws FileNotFoundException, ArchiveModificationException,
		EntryDuplicatedException
	{
		try
		{
			virtualJarManager.addFile(file, entry);
		}
		catch (IOException ex)
		{
			throw new ArchiveModificationException(ex.getMessage(),
				ex);
		}
	}

	@Override
	public void addManifest(Manifest manifest)
		throws ArchiveModificationException
	{
		manifestAdded = true;
		virtualJarManager.addManifest(manifest);
	}

	@Override
	public void addPackage(Class<?> representative)
		throws ArchiveModificationException, ClassNotFoundException,
		EntryDuplicatedException
	{
		virtualJarManager.addPackage(representative);
	}

	@Override
	public void addPropertiesToManifest(Map<String, String> properties)
		throws ArchiveModificationException
	{
		virtualJarManager.addPropertiesToManifest(properties);
	}

	@Override
	public List<JarEntry> entries() throws ArchiveReadException
	{
		List<JarEntry> entries = virtualJarManager.entries();
		return entries;
	}

	@Override
	public List<Entry<JarEntry, BytesWrapper>> entriesWithContent()
	{
		return virtualJarManager.entriesWithContent();
	}

	@Override
	public void extract(Path dir) throws CannotExtractArchiveException
	{
		List<Entry<JarEntry, BytesWrapper>> entries = virtualJarManager
			.entriesWithContent();
		for (Entry<JarEntry, BytesWrapper> entry : entries)
		{
			// Path entryAsPath =
			// JarManagerUtils.convertEntryToPath(entry.getKey(),
			// dir);
			Path entryAsPath = JarManagerUtils
				.convertEntryToRelativePath(entry.getKey());
			try
			{
				if (entry.getKey().isDirectory())
				{
					Files.createDirectories(
						dir.resolve(entryAsPath));
				}
				else
				{
					int nameCount = entryAsPath
						.getNameCount();
					if (nameCount > 2)
					{
						Path subpath = entryAsPath
							.subpath(0,
								nameCount - 1);
						// if (entryAsPath.isAbsolute())
						// {
						// subpath =
						// subpath.toAbsolutePath();
						// }
						Files.createDirectories(
							dir.resolve(subpath));
					}
					Files.write(dir.resolve(entryAsPath),
						entry.getValue().getBytes());
				}
			}
			catch (Exception ex)
			{
				throw new CannotExtractArchiveException(String
					.format("Cannot extract archive. Reason: %s",
						ex.toString()),
					ex);
			}
		}
	}

	@Override
	public Entry<JarEntry, BytesWrapper> findEntryByName(String entryName)
		throws ArchiveReadException
	{
		return virtualJarManager.findEntryByName(entryName);
	}

	@Override
	public void generateManifestFlag(boolean generateManifest)
	{
		this.generateManifest = generateManifest;
	}

	/**
	 * @return the jarFile
	 */
	public File getJarFile()
	{
		return jarFile;
	}

	@Override
	public Manifest manifest()
	{
		return virtualJarManager.manifest();
	}

	@Override
	public void removeEntry(JarEntry entry)
		throws ArchiveModificationException
	{
		virtualJarManager.removeEntry(entry);
	}

	@Override
	public void removeEntry(String entryName)
		throws ArchiveModificationException
	{
		virtualJarManager.removeEntry(entryName);
	}

	@Override
	public void save() throws ArchiveModificationException
	{
		try
		{
			saveVirtualArchiveToDisk(null);
			closeArchive();
			Analyzer analyzer = new Analyzer();
			analyzer.setJar(jarFile);
			if (!manifestAdded)
			{
				addPropertiesToAnalyzer(analyzer);
				Manifest manifest = analyzer.calcManifest();
				// addMissingManifestProperties(manifest);
				virtualJarManager.addManifest(manifest);
				manifestAdded = false;
			}
			analyzer.close();
			if (generateManifest)
			{
				saveVirtualArchiveToDisk(
					virtualJarManager.manifest());
			}
			else
			{
				saveVirtualArchiveToDisk(null);
			}
		}
		catch (Exception ex)
		{
			throw new ArchiveModificationException(ex.getMessage(),
				ex);
		}
		finally
		{
			closeArchiveFinally();
		}
	}

	/**
	 * @param jarFile
	 *                the jarFile to set
	 */
	public void setJarFile(File jarFile)
	{
		this.jarFile = jarFile;
	}

	private void addPropertiesToAnalyzer(Analyzer analyzer)
	{
		Manifest virtualManifest = virtualJarManager.manifest();
		if (virtualManifest != null)
		{
			Attributes attributes = virtualManifest
				.getMainAttributes();
			for (Entry<Object, Object> attr : attributes.entrySet())
			{
				analyzer.setProperty(attr.getKey().toString(),
					attr.getValue().toString());
			}
		}
	}

	private void closeArchive() throws IOException
	{
		if (jarOS != null)
		{
			jarOS.close();
		}
	}

	private void closeArchiveFinally()
	{
		try
		{
			closeArchive();
		}
		catch (IOException ex)
		{
			logWarn(ex.getMessage(), ex);
		}
	}

	private void copyArchiveContent() throws IOException,
		ArchiveModificationException, EntryDuplicatedException
	{
		if (!jarFile.exists())
		{
			return;
		}
		JarFile jarArchive = null;
		try
		{
			jarArchive = new JarFile(jarFile);
			List<JarEntry> entries = Collections
				.list(jarArchive.entries());
			for (JarEntry entry : entries)
			{
				if (entry.isDirectory())
				{
					addEntry(entry);
				}
				else
				{
					InputStream is = jarArchive
						.getInputStream(entry);
					BytesWrapper content = new BytesWrapper(
						IOUtils.toByteArray(is));
					addContent(entry, content);
				}
			}
		}
		finally
		{
			if (jarArchive != null)
			{
				try
				{
					jarArchive.close();
				}
				catch (IOException ex)
				{
					logWarn(ex.getMessage(), ex);
				}
			}
		}
	}

	private void init(File jarFile) throws IOException,
		ArchiveModificationException, EntryDuplicatedException
	{
		this.jarFile = jarFile;
		virtualJarManager = new VirtualJarManager();
		manifestAdded = false;
		generateManifest = true;
		copyArchiveContent();
	}

	/**
	 * @param message
	 * @param ex
	 */
	private void logWarn(String message, IOException ex)
	{
		logger.warn(message, ex);
	}

	private void openArchive(Manifest manifest) throws IOException
	{
		closeArchive();
		if (manifest != null)
		{
			jarOS = new JarOutputStream(
				new FileOutputStream(jarFile), manifest);
		}
		else
		{
			jarOS = new JarOutputStream(
				new FileOutputStream(jarFile));
		}
	}

	private void saveVirtualArchiveToDisk(Manifest manifest)
		throws IOException
	{
		openArchive(manifest);
		for (Entry<JarEntry, BytesWrapper> entryWithContent : virtualJarManager
			.entriesWithContent())
		{
			jarOS.putNextEntry(entryWithContent.getKey());
			if (entryWithContent.getValue() != null)
			{
				jarOS.write(
					entryWithContent.getValue().getBytes());
			}
			jarOS.closeEntry();
		}
	}
}