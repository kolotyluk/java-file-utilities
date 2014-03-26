Java File Utilities
===================

Some useful utilities and extras not found other places.

## Files.contentEquals

  Uses [Memory Mapped Files](http://en.wikipedia.org/wiki/Memory-mapped_file) for fast comparison of large files.
  This is about 4 times faster than calling
  
  > [org.apache.commons.io.FileUtils.contentEquals(file1, file2)](http://commons.apache.org/proper/commons-io/apidocs/org/apache/commons/io/FileUtils.html#contentEquals%28java.io.File,%20java.io.File%29)
   
   with identical 4 [GiB](http://en.wikipedia.org/wiki/Gibibyte) files. Performance may suffer slightly for smaller files.
   A future version may be optimized for smaller files too.

   Useful for checking if media files, such as music and video, have identical or duplicate content.
   Video files can easily be over 4 GiB in size. Technically this method will compare files as large as the
   file system supports.
   
   Performance is about 16 seconds for two 4 GiB files with identical content on an Intel Xeon 5580 at 3.2 GHz
   using a file system on a LSI MegaRAID SAS 9266-8i.</p>
