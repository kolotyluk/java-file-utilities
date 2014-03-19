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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Some Useful File System Utilities and Extras
 * @author Eric Kolotyluk
 *
 */
public class Files {

  /**
   * Identical File Contents
   *  
   * <p>Uses 
   * <a href="http://en.wikipedia.org/wiki/Memory-mapped_file">Memory Mapped Files</a>
   * for fast comparison of large files. This is about 4 times faster than calling</p><pre>
   * <a href="http://commons.apache.org/proper/commons-io/apidocs/org/apache/commons/io/FileUtils.html#contentEquals%28java.io.File,%20java.io.File%29">org.apache.commons.io.FileUtils.contentEquals(File file1,File file2)</a></pre>
   * <p>with identical 4
   * <a href="http://en.wikipedia.org/wiki/Gibibyte">GiB</a>
   * files. Performance may suffer slightly for smaller files.
   * A future version may be optimized for smaller files too.</p>
   * 
   * <p>Useful for checking if media files, such as music and video,
   * have identical or duplicate content. Video files can easily
   * be over 4 GiB in size. Technically this method will compare
   * files as large as the file system supports.</p>
   * 
   * <p>Performance is about 16 seconds for two 4 GiB files with
   * identical content on an Intel Xeon 5580 at 3.2 GHz using a
   * file system on a LSI MegaRAID SAS 9266-8i.</p>
   * 
   * @param file1 1st file to be compared
   * @param file2 2nd file to be compared
   * @return true when both files have identical contents
   * @throws IllegalArgumentException if the arguments are not files.
   * @throws IOException if there is a problem with File I/O
   * @throws RuntimeException if there was a problem unmapping the
   * buffers or closing the channels, which may leave one or more
   * files locked for read.
   */
  public static Boolean contentEquals(Path file1, Path file2) throws IOException
  {
      if (!java.nio.file.Files.isRegularFile(file1))
	      throw new IllegalArgumentException(file1 + "is not a regular file");

	  if (!java.nio.file.Files.isRegularFile(file2))
		  throw new IllegalArgumentException(file2 + "is not a regular file");
    
	  FileChannel channel1 = null;
	  FileChannel channel2 = null;
    
	  MappedByteBuffer buffer1 = null;
	  MappedByteBuffer buffer2 = null;
    
	  try
	  {
		  long size1 = java.nio.file.Files.size(file1);
		  long size2 = java.nio.file.Files.size(file2);
        
		  if (size1 != size2) return false;
        
		  long position = 0;
		  long length = Math.min(Integer.MAX_VALUE, size1 - position);

		  channel1 = FileChannel.open(file1);
		  channel2 = FileChannel.open(file2);

		  // Cannot map files larger than Integer.MAX_VALUE,
		  // so we have to do it in pieces.

		  while (length > 0)
		  {
			  buffer1 = channel1.map(MapMode.READ_ONLY, position, length);
			  buffer2 = channel2.map(MapMode.READ_ONLY, position, length);
			  // if (!buffer1.equals(buffer2)) return false;
			  // The line above is much slower than the line below.
			  // It should not be, but it is, possibly because it is
			  // loading the entire buffer into memory before comparing
			  // the contents. See the corresponding unit test. EK
			  for (int i = 0; i < length; i++) if (buffer1.get() != buffer2.get()) return false;
			  position += length;
			  length = Math.min(Integer.MAX_VALUE, size1 - position);
			  cleanDirectByteBuffer(buffer1); buffer1 = null;
			  cleanDirectByteBuffer(buffer2); buffer2 = null;
		  }
	  }
	  finally
	  {
		  // Is is important to clean up so we do not hold any
		  // file locks, in case the caller wants to do something
		  // else with the files.
		  
		  // In terms of functional programming, holding a lock after
		  // returning to the caller would be an unwelcome side-effect.

		  cleanDirectByteBuffer(buffer1);
		  cleanDirectByteBuffer(buffer2);
    	
		  if (channel1 != null) try
		  {
			  channel1.close();
		  }
		  catch (IOException e)
		  {
			  if (channel2 != null) channel2.close();
			  throw e;
		  }

		  if (channel2 != null) channel2.close();
	  }
    
	  return true;
  }
  
 /**
  * Clean or unmap a direct ByteBuffer
  * 
  * <p>DirectByteBuffers are garbage collected by using a phantom reference and a
  * reference queue. Every once a while, the JVM checks the reference queue and
  * cleans the DirectByteBuffers. However, as this doesn't happen immediately
  * after discarding all references to a DirectByteBuffer, it's easy get
  * {@link OutOfMemoryError} problems using direct ByteBuffers.</p>
  * 
  * <p>Also, if a file is still mapped, via {@link MappedByteBuffer}, then it is
  * locked and cannot be destroyed or possibly written to if it was previously
  * mapped read. Trying to destroy or write to a locked file will result in an
  * {@link IOException}</p>
  * 
  * <p>This function explicitly calls the cleaner method of a {@link ByteBuffer} using
  * reflection because it is not publicly accessible.</p>
  * 
  * @param byteBuffer The DirectByteBuffer that will be "cleaned". Returns immediately if
  * the argument is null.
  * @throws IllegalArgumentException if byteBuffer isn't direct
  * @throws RuntimeException if cleaning may have failed
  */
  public static void cleanDirectByteBuffer(final ByteBuffer byteBuffer)
  {
	  
	if (byteBuffer == null) return;

    if (!byteBuffer.isDirect())
    	throw new IllegalArgumentException("byteBuffer isn't direct!");
    
    AccessController.doPrivileged(new PrivilegedAction<Void>() {
        public Void run() {
        	try
        	{
        		Method cleanerMethod = byteBuffer.getClass().getMethod("cleaner");
        	    cleanerMethod.setAccessible(true);
        	    Object cleaner = cleanerMethod.invoke(byteBuffer);
        	    Method cleanMethod = cleaner.getClass().getMethod("clean");
        	    cleanMethod.setAccessible(true);
        	    cleanMethod.invoke(cleaner);
        	}
        	catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        	{
        		throw new RuntimeException("Could not clean MappedByteBuffer -- File may still be locked!");
        	}
           return null; // nothing to return
        }
    });


  }

}