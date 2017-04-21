/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package goobar.cextractor.jarmanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

/**
 * Interface for managing jar archive.
 *
 * @author goobar
 */
@SuppressWarnings("javadoc")
public interface JarManager
{

	/**
	 * Adds a class file to archive. Most probably class file will be
	 * searched inside CLASSPATH.
	 *
	 * @param clazz
	 *                Class that will be added to archive.
	 * @throws ClassNotFoundException
	 *                 Thrown when class file can't be found.
	 * @throws ArchiveModificationException
	 * @throws EntryDuplicatedException
	 */
	public void addClass(Class<?> clazz) throws ClassNotFoundException,
		ArchiveModificationException, EntryDuplicatedException;

	/**
	 * Adds an entry with bytes content.
	 *
	 * @param entry
	 *                Content will be added to this entry.
	 * @param bytes
	 *                Content in bytes {@link BytesWrapper}.
	 *
	 * @throws ArchiveModificationException
	 * @throws EntryDuplicatedException
	 * @see BytesWrapper
	 */
	public void addContent(JarEntry entry, BytesWrapper bytes)
		throws ArchiveModificationException, EntryDuplicatedException;

	/**
	 * Adds directory content (with subdirectories) to the archive.
	 *
	 * @param dir
	 *                the directory to add
	 * @throws ArchiveModificationException
	 *                 thrown when archive cannot be modified
	 * @throws EntryDuplicatedException
	 *                 thrown when entry is duplicated
	 */
	public void addDirectory(Path dir)
		throws ArchiveModificationException, EntryDuplicatedException;

	/**
	 * Adds entry without content.
	 *
	 * @param entry
	 *                Jar entry without content
	 * @throws ArchiveModificationException
	 * @throws EntryDuplicatedException
	 */
	public void addEntry(JarEntry entry)
		throws ArchiveModificationException, EntryDuplicatedException;

	/**
	 * Adds a file.
	 *
	 * @param file
	 *                File to add.
	 * @param entry
	 *                File will be added to this entry.
	 * @throws ArchiveModificationException
	 * @throws FileNotFoundException
	 * @throws EntryDuplicatedException
	 */
	public void addFile(File file, JarEntry entry)
		throws ArchiveModificationException, FileNotFoundException,
		EntryDuplicatedException;

	/**
	 * Adds the manifest file to archive.
	 *
	 * @param manifest
	 *                Manifest file that will be added to archive. Previous
	 *                manifest will be replaced.
	 * @throws ArchiveModificationException
	 */
	public void addManifest(Manifest manifest)
		throws ArchiveModificationException;

	/**
	 * Adds a package to archive.
	 *
	 * @param representative
	 *                Package representative. Representative is any class
	 *                that is inside the package. All other classes inside
	 *                the package will be added as well.
	 * @throws ArchiveModificationException
	 * @throws ClassNotFoundException
	 *                 Thrown when class can't be found.
	 * @throws EntryDuplicatedException
	 */
	public void addPackage(Class<?> representative)
		throws ArchiveModificationException, ClassNotFoundException,
		EntryDuplicatedException;

	/**
	 * Adds extra properties to manifest file.
	 *
	 * @param properties
	 *                key - value properties pairs.
	 *
	 * @throws ArchiveModificationException
	 */
	public void addPropertiesToManifest(Map<String, String> properties)
		throws ArchiveModificationException;

	/**
	 * Returns all entries in archive.
	 *
	 * @return List of all entries in archive.
	 * @throws ArchiveReadException
	 */
	public List<JarEntry> entries() throws ArchiveReadException;

	/**
	 * Returns list of entries with content.
	 *
	 * @return List of entries. Each entry has also a content assigned to
	 *         it. When entry is empty content assigned to it is null.
	 */
	public List<Entry<JarEntry, BytesWrapper>> entriesWithContent();

	/**
	 * Extracts archive.
	 *
	 * @param dir
	 *                the target directory
	 * @throws CannotExtractArchiveException
	 *                 thrown when archive cannot be extracted
	 */
	public void extract(Path dir) throws CannotExtractArchiveException;

	/**
	 * Returns entry by given name.
	 *
	 * @param entryName
	 *                Entry with this name (or null if it doesn't exist)
	 *                will be returned.
	 * @return Pair of jar entry and its content (null if entry doesn't have
	 *         content).
	 * @throws ArchiveReadException
	 */
	public Entry<JarEntry, BytesWrapper> findEntryByName(String entryName)
		throws ArchiveReadException;

	/**
	 * Sets manifest generation flag.
	 *
	 * @param generateManifest
	 *                if true Manifest file will be generated
	 */
	public void generateManifestFlag(boolean generateManifest);

	/**
	 * Returns manifest.
	 *
	 * @return Manifest file.
	 */
	public Manifest manifest();

	/**
	 * Removes given entry.
	 *
	 * @param entry
	 *                Entry to be removed from archive.
	 * @throws ArchiveModificationException
	 */
	public void removeEntry(JarEntry entry)
		throws ArchiveModificationException;

	/**
	 * Removes entry with the same name as entryName parameter.
	 *
	 * @param entryName
	 *                Entry with this name will be removed from archive.
	 * @throws ArchiveModificationException
	 */
	public void removeEntry(String entryName)
		throws ArchiveModificationException;

	/**
	 * Saves archive. Behaviour depends on implementation. The archive can
	 * be saved to disk, persisted to database etc.
	 *
	 * @throws ArchiveModificationException
	 */
	public void save() throws ArchiveModificationException;
}
