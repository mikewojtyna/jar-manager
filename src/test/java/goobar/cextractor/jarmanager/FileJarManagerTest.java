/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package goobar.cextractor.jarmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import goobar.cextractor.jarmanager.testpackage.ClassA;

/**
 *
 * @author goobar
 */
@SuppressWarnings("javadoc")
public class FileJarManagerTest
{

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private File testFile;

	private File testJarFile;

	@Before
	public void setUp() throws Exception
	{
		testFile = tempFolder.newFile("test.txt");
		testJarFile = new File(tempFolder.getRoot(), "test.jar");
	}

	@Test
	public void should_AddClass() throws Exception
	{
		// given
		JarManager jarManager = new FileJarManager(testJarFile);

		// when
		jarManager.addClass(BytesWrapper.class);
		jarManager.save();

		// then
		FileJarManager newJarManagerAfterSave = new FileJarManager(
			testJarFile);
		Entry<JarEntry, BytesWrapper> testClassEntry = newJarManagerAfterSave
			.findEntryByName(
				"goobar/cextractor/jarmanager/BytesWrapper.class");
		assertNotNull(testClassEntry.getKey());
		assertNotNull(testClassEntry.getValue());
	}

	@Test
	public void should_AddContent() throws Exception
	{
		// given
		JarManager jarManager = new FileJarManager(testJarFile);
		String entry = "path0/path1/test.txt";
		BytesWrapper contentBytes = new BytesWrapper(
			"test.txt content".getBytes());

		// when
		jarManager.addContent(new JarEntry(entry), contentBytes);

		// then
		assertEquals(contentBytes,
			jarManager.findEntryByName(entry).getValue());
	}

	@Test
	public void should_AddDirectoryContent() throws Exception
	{
		// given
		Path testDir = tempFolder.newFolder("testDir").toPath();
		// prepare directory content
		Files.createFile(testDir.resolve("testFile0.txt"));
		Path nestedTestDir = Files
			.createDirectory(testDir.resolve("nestedTestDir"));
		Files.createFile(nestedTestDir.resolve("testFile1.txt"));
		JarManager jarManager = new FileJarManager(testJarFile);

		// when
		jarManager.addDirectory(testDir);
		jarManager.save();

		// then
		FileJarManager newJarManagerAfterSave = new FileJarManager(
			testJarFile);
		assertNotNull(newJarManagerAfterSave
			.findEntryByName("nestedTestDir/"));
		assertNotNull(newJarManagerAfterSave
			.findEntryByName("testFile0.txt").getKey());
		assertNotNull(newJarManagerAfterSave
			.findEntryByName("testFile0.txt").getValue());
		assertNotNull(newJarManagerAfterSave
			.findEntryByName("nestedTestDir/testFile1.txt")
			.getKey());
		assertNotNull(newJarManagerAfterSave
			.findEntryByName("nestedTestDir/testFile1.txt")
			.getValue());
	}

	@Test
	public void should_AddFile() throws IOException,
		ArchiveModificationException, FileNotFoundException,
		EntryDuplicatedException, ArchiveReadException
	{
		// given
		JarManager jarManager = new FileJarManager(testJarFile);
		// add two lines to test file
		Files.write(testFile.toPath(), Arrays.asList("line0", "line1"),
			Charset.defaultCharset());
		String testFileEntry = "path0/path1/test.txt";

		// when
		jarManager.addFile(testFile, new JarEntry(testFileEntry));
		jarManager.save();

		// when
		FileJarManager newJarManagerAfterSave = new FileJarManager(
			testJarFile);
		Entry<JarEntry, BytesWrapper> entry = newJarManagerAfterSave
			.findEntryByName(testFileEntry);
		assertEquals(testFileEntry, entry.getKey().getName());
		try (InputStream testFileContentByteStream = new ByteArrayInputStream(
			entry.getValue().getBytes()))
		{
			// test file entry content
			List<String> testFileEntryLines = IOUtils.readLines(
				testFileContentByteStream,
				Charset.defaultCharset());
			assertEquals(Arrays.asList("line0", "line1"),
				testFileEntryLines);
		}
	}

	@Test
	public void should_AddManifest() throws Exception
	{
		// given
		Manifest manifest = new Manifest();
		JarManager jarManager = new FileJarManager(testJarFile);

		// when
		jarManager.addManifest(manifest);
		jarManager.save();

		// then
		FileJarManager newFileJarManagerAfterSave = new FileJarManager(
			testJarFile);
		assertEquals(manifest, newFileJarManagerAfterSave.manifest());
	}

	@Test
	public void should_AddPackage() throws Exception
	{
		// given
		JarManager jarManager = new FileJarManager(testJarFile);

		// when
		jarManager.addPackage(ClassA.class);

		// then
		List<JarEntry> entries = jarManager.entries();
		assertEquals(4, entries.size());
		assertNotNull(jarManager.findEntryByName(
			"goobar/cextractor/jarmanager/testpackage/"));
		assertNotNull(jarManager.findEntryByName(
			"goobar/cextractor/jarmanager/testpackage/ClassA.class"));
		assertNotNull(jarManager.findEntryByName(
			"goobar/cextractor/jarmanager/testpackage/ClassB.class"));
		assertNotNull(jarManager.findEntryByName(
			"goobar/cextractor/jarmanager/testpackage/ClassC.class"));
	}

	@Test
	public void should_ExtractArchive() throws Exception
	{
		// given
		JarManager jarManager = new FileJarManager(testJarFile);
		JarEntry entry = new JarEntry("path0/path1/test.txt");
		jarManager.addFile(testFile, entry);
		Path extractDir = tempFolder.newFolder("extract-dir").toPath();

		// when
		jarManager.extract(extractDir);

		// then
		assertTrue(Files.exists(extractDir.resolve("path0")
			.resolve("path1").resolve("test.txt")));
	}

	@Test
	public void should_RemoveEntry() throws Exception
	{
		// given
		JarManager jarManager = new FileJarManager(testJarFile);
		JarEntry entry = new JarEntry("path/test.txt");
		jarManager.addFile(testFile, entry);
		assertEquals(entry,
			jarManager.findEntryByName(entry.getName()).getKey());

		// when
		jarManager.removeEntry(entry);

		// then
		assertNull(jarManager.findEntryByName(entry.getName()));
	}

	@Test(expected = EntryDuplicatedException.class)
	public void should_ThrowException_When_EntryIsDuplicated()
		throws Exception
	{
		// given
		FileJarManager jarManager = new FileJarManager(testJarFile);
		JarEntry entry = new JarEntry("entry/");

		// when
		jarManager.addEntry(entry);
		jarManager.addEntry(entry);
	}

}
