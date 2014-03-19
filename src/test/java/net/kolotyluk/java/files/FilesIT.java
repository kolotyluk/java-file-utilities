/*  Copyright © 2014 by Eric Kolotyluk <eric@kolotyluk.net>

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package net.kolotyluk.java.files;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

/**
 * Integration Test Cases
 * 
 * <p>While these are technically unit tests, they are long running,
 * on the order of 10 minutes or more. Consequently, they are best 
 * run as Integration Tests.</p>
 * 
 * <p>These tests are run in order because some tests rely on the
 * results from previous tests.</p>
 * @author Eric Kolotyluk
 *
 */
@FixMethodOrder(NAME_ASCENDING)
public class FilesIT {
	
	private static Path testFolder;
	private static Path referenceFile;
	private static Path identicalFile;
	private static Path differentFile;

	@SuppressWarnings(value="ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",justification="test code")
	private static long
		lastTime,
		timeToCreateTestFixtures,
		timeToCompareIdenticalFiles,
		timeToCompareIdenticalFilesInternal,
		timeToCompareIdenticalFilesApache,
		timeToCompareDifferentFiles,
		timeToCompareDifferentFilesInternal,
		timeToCompareDifferentFilesApache;

	/**
	 * Fixture Setup
	 * 
	 * <p>Create some temporary test files. These are quite large because
	 * the Unit Under Test is designed for media files. Also, the UUT is
	 * based on memory mapped files, and you can only map about 2 GiB at
	 * a time, so we want to test files larger than this. Finally, the
	 * reason for using memory mapped files is to increase performance, so
	 * we want to measure the performance and compare it against something
	 * like org.apache.commons.io.FileUtils.contentEquals().
	 * @throws IOException
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws IOException
	{
		testFolder = Files.createTempDirectory("testFolder-");
		
		System.out.println("testFolder = " + testFolder);
		
		referenceFile = Files.createTempFile(testFolder, "test-", ".txt");
		identicalFile = Files.createTempFile(testFolder, "test-", ".txt");
		differentFile = Files.createTempFile(testFolder, "test-", ".txt");
		
		System.out.println("referenceFile = " + referenceFile);
		System.out.println("identicalFile = " + identicalFile);
		System.out.println("differentFile = " + differentFile);

		lastTime = System.currentTimeMillis();

		Charset charset = Charset.forName("UTF-8");
		OpenOption[] openOptions = new OpenOption[] {StandardOpenOption.WRITE};

		BufferedWriter writer1 = Files.newBufferedWriter(referenceFile, charset, openOptions);
		BufferedWriter writer2 = Files.newBufferedWriter(identicalFile, charset, openOptions);
		BufferedWriter writer3 = Files.newBufferedWriter(differentFile, charset, openOptions);

		// file3 is different in the first byte, so comparison should fail early

		writer1.write('a');
		writer2.write('a');
		writer3.write('b');
		
		// TODO this takes a long time, so maybe we can speed it up?

		for (long i = 0; i < Integer.MAX_VALUE; i++)
		{
			writer1.write('a');
			writer2.write('a');
			writer3.write('a');
		}
		
		writer1.write('a');
		writer2.write('a');
		writer3.write('a');
		
		for (long i = 0; i < Integer.MAX_VALUE; i++)
		{
			writer1.write('a');
			writer2.write('a');
			writer3.write('a');
		}
		
		writer1.close();
		writer2.close();
		writer3.close();
		
		timeToCreateTestFixtures = System.currentTimeMillis() - lastTime;
		
		System.out.println("timeToCreateTestFixtures = " + timeToCreateTestFixtures + " ms");

		lastTime = System.currentTimeMillis();
	}

	/**
	 * Fixture Teardown
	 * 
	 * <p>We have created some really large temporary files, so it is important
	 * to delete them before we run out of space on our file system.</p>
	 * 
	 * <p>Note: sometimes if there is a problem with the tests, this code does not
	 * get executed, so we should find a way to make this part more robust.</p>
	 * @throws IOException
	 */
	@AfterClass
	public static void tearDownAfterClass() throws IOException
	{
		Files.delete(referenceFile); assertFalse(Files.exists(referenceFile)); System.out.println("referenceFile deleted");
		Files.delete(identicalFile); assertFalse(Files.exists(identicalFile)); System.out.println("identicalFile deleted");
		Files.delete(differentFile); assertFalse(Files.exists(differentFile)); System.out.println("differentFile deleted");

		Files.delete(testFolder); assertFalse(Files.exists(testFolder)); System.out.println("testFolder deleted");
	}

	/**
	 * Test Identical File Comparison
	 * 
	 * <p> This test is designed to see not only if the comparison of two
	 * identical files is correct, but also if it is efficient for large files.
	 * @throws IOException 
	 * @see #tc07_contentEqualsIsQuick()
	 */
	@Test
	public void tc00_contentEquals() throws IOException
	{
		assumeNotNull(referenceFile);
		
		assumeNotNull(identicalFile);
		
		lastTime = System.currentTimeMillis();
		
		boolean equal = net.kolotyluk.java.files.Files.contentEquals(referenceFile, identicalFile);
		
		timeToCompareIdenticalFiles = System.currentTimeMillis() - lastTime;

		System.out.println("timeToCompareIdenticalFiles = " + timeToCompareIdenticalFiles + " ms");
		
		assertTrue("failure - referenceFile should be identical to identicalFile", equal);
	}

	/**
	 * Test Identical File Comparison Using Internal Test Method
	 * 
	 * <p>Double check that the files are equal using an internal test
	 * method to be sure they actually are equal.</p>
	 * @throws IOException
	 */
	@Test
	public void tc01_contentEqualsInternal() throws IOException
	{
		assumeNotNull(referenceFile);
		
		assumeNotNull(identicalFile);
		
		lastTime = System.currentTimeMillis();
		
		boolean equal = contentEquals(referenceFile, identicalFile);
		
		timeToCompareIdenticalFilesInternal = System.currentTimeMillis() - lastTime;

		System.out.println("timeToCompareIdenticalFilesInternal = " + timeToCompareIdenticalFilesInternal + " ms");		

		assertTrue("failure - referenceFile should be identical to identicalFile (internal)", equal);
	}

	/**
	 * Test Identical File Comparison Using Apache
	 * 
	 * <p>Many people use Apache's library for comparing files, but it is not
	 * as fast for large files, so we are doing more of a performance comparison
	 * here. It also helps to test with an external standard to triple-check
	 * that the files are equal.</p>
	 * @throws IOException
	 * @see #tc07_contentEqualsIsQuick()
	 */
	@Test
	public void tc02_contentEqualsApache() throws IOException
	{
		assumeNotNull(referenceFile);
		
		assumeNotNull(identicalFile);
		
		lastTime = System.currentTimeMillis();
		
		boolean equal = org.apache.commons.io.FileUtils.contentEquals(referenceFile.toFile(), identicalFile.toFile());
		
		timeToCompareIdenticalFilesApache = System.currentTimeMillis() - lastTime;

		System.out.println("timeToCompareIdenticalFilesApache = " + timeToCompareIdenticalFilesApache + " ms");		

		assertTrue("failure - referenceFile should be identical to identicalFile (Apache)", equal);
	}

	/**
	 * Test Different File Comparison
	 * 
	 * <p>This test is designed to see not only if the comparison of two
	 * different files is correct, but also if it is efficient for large files.</p>
	 * @throws IOException 
	 * @see #tc08_notContentEqualsIsQuick()
	 */
	@Test
	public void tc04_notContentEquals() throws IOException
	{
		assumeNotNull(referenceFile);
		
		assumeNotNull(differentFile);
		
		lastTime = System.currentTimeMillis();
		
		boolean equal =  net.kolotyluk.java.files.Files.contentEquals(referenceFile, differentFile);
		
		timeToCompareDifferentFiles = System.currentTimeMillis() - lastTime;

		System.out.println("timeToCompareDifferentFiles = " + timeToCompareDifferentFiles + " ms");

		assertFalse("failure - referenceFile should not be identical to different file", equal);

	}

	/**
	 * Test Different File Comparison Using Internal Test Method
	 * 
	 * <p>Double check that the files are not equal using an internal test
	 * method to be sure they actually are different.</p>
	 * @throws IOException
	 */
	@Test
	public void tc05_notContentEqualsInternal() throws IOException
	{
		assumeNotNull(referenceFile);
		
		assumeNotNull(identicalFile);
		
		lastTime = System.currentTimeMillis();
		
		boolean equal = contentEquals(referenceFile, differentFile);
		
		timeToCompareDifferentFilesInternal = System.currentTimeMillis() - lastTime;

		System.out.println("timeToCompareDifferentFilesInternal = " + timeToCompareDifferentFilesInternal + " ms");		

		assertFalse("failure - referenceFile should not be identical to different file (internal)", equal);
	}

	/**
	 * Test Different File Comparison Using Apache
	 * 
	 * <p>Just triple-checking that the files really are different.</p>
	 * @throws IOException
	 */
	@Test
	public void tc06_notContentEqualsApache() throws IOException
	{
		assumeNotNull(referenceFile);
		
		assumeNotNull(identicalFile);
		
		lastTime = System.currentTimeMillis();
		
		boolean equal = org.apache.commons.io.FileUtils.contentEquals(referenceFile.toFile(), differentFile.toFile());
		
		timeToCompareDifferentFilesApache = System.currentTimeMillis() - lastTime;

		System.out.println("timeToCompareDifferentFilesApache = " + timeToCompareDifferentFilesApache + " ms");		

		assertFalse("failure - referenceFile should not be identical to different file (Apache)", equal);
	}

	/**
	 * Is the Unit Under Test faster than Apache?
	 * <p>
	 * net.kolotyluk.java.files.Files.identical(referenceFile, identicalFile)
	 * should execute faster than org.apache.commons.io.FileUtils.contentEquals(referenceFile.toFile(), identicalFile.toFile());
	 * for large files because it is using memory mapped files.
	 */
	@Test
	public void tc07_contentEqualsIsQuick()
	{
		assertTrue("failure - too long to determine referenceFile is identical to differentFile", timeToCompareIdenticalFiles < timeToCompareIdenticalFilesApache);
	}

	/**
	 * Is comparing different files faster than comparing identical files?
	 * <p>
	 * Comparing files that are different in the first byte should be significantly faster
	 * than than comparing identical files. One problem with an earlier version of the code
	 * was it was using <pre><code>if (!buffer1.equals(buffer2)) return false;</code></pre> which was slow, probably
	 * because it was loading the entire map into memory before the comparison.
	 */
	@Test
	public void tc08_notContentEqualsIsQuick()
	{
		assertTrue("failure - too long to determine referenceFile is not identical to differentFile", timeToCompareDifferentFiles < timeToCompareIdenticalFiles / 100);
	}
	
	/**
	 * Internal contentEquals similar to Apache.
	 * @param file1
	 * @param file2
	 * @return true if the contents are identical
	 * @throws IOException
	 */
	boolean contentEquals(Path file1, Path file2) throws IOException
	{
		InputStream stream1 = null;
		InputStream stream2 = null;
		
		try
		{
			int EOF = -1;

			long length1 = Files.size(file1);
			long length2 = Files.size(file2);
			
			if (length1 != length2) return false;
			
			stream1 = new BufferedInputStream(Files.newInputStream(file1), 65536);
			stream2 = new BufferedInputStream(Files.newInputStream(file2), 65536);
			
			for (int i = 0; i <= length1; i++)
			{
				int input1 = stream1.read();
				int input2 = stream2.read();
				if (input1 != input2) return false;
				if (input1 == EOF)
				{
					if (input2 == EOF) return true; else return false;
				}
				else if (input2 == EOF)
				{
					if (input1 == EOF) return true; else return false;
				}
			}
		}
		finally
		{
			try
			{
				if (stream1 != null) stream1.close();
			}
			catch (IOException e)
			{
				if (stream2 != null) stream2.close();
				throw e;
			}
			if (stream2 != null) stream2.close();
		}

		return false;
	}

//TODO test this method too	

//	@Test
//	public void testCleanDirectByteBuffer() {
//		fail("Not yet implemented");
//	}

}
