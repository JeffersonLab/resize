package org.jlab.resize.util;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IO Utilities.
 *
 * @author ryans
 */
public final class IOUtil {

  private static final Logger logger = Logger.getLogger(IOUtil.class.getName());

  private IOUtil() {
    // Can't instantiate publicly
  }

  /**
   * Closes a Closeable without generating any checked Exceptions. If an IOException does occur
   * while closing it is logged as a WARNING.
   *
   * @param c The Closeable
   */
  public static void closeQuietly(Closeable c) {
    if (c != null) {
      try {
        c.close();
      } catch (IOException e) {
        // Supressed, but logged
        logger.log(Level.WARNING, "Unable to close resource.", e);
      }
    }
  }

  /**
   * Copies all of the bytes from the InputStream into the OutputStream using a buffer of 4096
   * bytes.
   *
   * @param in The InputStream
   * @param out The OutputStream
   * @return The number of bytes copied
   * @throws IOException If unable to copy
   */
  public static long copy(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[4096];
    long count = 0;
    int n = 0;

    while (-1 != (n = in.read(buffer))) {
      out.write(buffer, 0, n);
      count += n;
    }

    return count;
  }

  private static Base64.Encoder enc = Base64.getEncoder();
  private static Base64.Decoder dec = Base64.getDecoder();

  /**
   * Encodes an array of bytes to base64.
   *
   * @param data The bytes
   * @return A base64 encoded String
   */
  public static String encodeBase64(byte[] data) {
    return enc.encodeToString(data);
  }

  /**
   * Decodes a base64 String to an array of bytes.
   *
   * @param data The base64 encoded String
   * @return The bytes
   */
  public static byte[] decodeBase64(String data) {
    return dec.decode(data);
  }

  /**
   * Fully reads in a file and returns an array of the bytes representing the file. Be careful
   * reading in large files because they may result in an OutOfMemoryError.
   *
   * <p>This method uses the File length to efficiently allocate memory.
   *
   * @param file The file to load into memory.
   * @return The bytes
   * @throws IOException If an error occurs reading in the file.
   */
  public static byte[] fileToBytes(final File file) throws IOException {
    final byte[] bytes = new byte[(int) file.length()];

    DataInputStream dis = null;

    try {
      dis = new DataInputStream(new FileInputStream(file));

      dis.readFully(bytes);
    } finally {
      closeQuietly(dis);
    }

    return bytes;
  }
}
