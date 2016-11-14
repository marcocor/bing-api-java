package it.unipi.di.acube.searchapi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

public class StringCompress {

    /**
     * Compress a string with GZip.
     * 
     * @param str
     *            the string.
     * @return the compressed string.
     * @throws IOException
     *             if something went wrong during compression.
     */
    public static byte[] compress(String str) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes("utf-8"));
        gzip.close();
        return out.toByteArray();
    }

    /**
     * Decompress a GZipped string.
     * 
     * @param compressed
     *            the sequence of bytes
     * @return the decompressed string.
     * @throws IOException
     *             if something went wrong during decompression.
     */
    public static String decompress(byte[] compressed) throws IOException {
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
        return new String(IOUtils.toByteArray(gis), "utf-8");
    }
}
