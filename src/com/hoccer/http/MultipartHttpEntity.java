package com.hoccer.http;

import java.io.*;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.message.*;

import com.hoccer.data.*;
import com.hoccer.thread.*;

public class MultipartHttpEntity extends AbstractHttpEntity {

    public static final String BORDER          = "ycKtoN8VURwvDC4sUzYC9Mo7l0IVUyDDVf";

    private byte[]             mPreample       = null;
    private final byte[]       mEnd            = ("\r\n--" + BORDER + "--\r\n").getBytes();
    private StreamableContent  mStreamable;

    private StatusHandler      mStatusCallback = null;
    private long               mDataSize;

    public MultipartHttpEntity() {
    }

    public void registerStatusHandler(StatusHandler pStatusCallback) {
        mStatusCallback = pStatusCallback;
    }

    public void addPart(String name, StreamableContent pStreamable) throws IOException {

        if (mPreample != null) {
            throw new RuntimeException(
                    "this multipart implementation can only handle a single part --- sorry");
        }

        mPreample = createPreamble(name, pStreamable.getFilename(), pStreamable.getContentType());
        mStreamable = pStreamable;
        mDataSize = mStreamable.getStreamLength();
    }

    private byte[] createPreamble(String name, String filename, String mime) {
        StringBuilder preamble = new StringBuilder();
        preamble.append("--" + BORDER + "\r\n");
        preamble.append("Content-Disposition: form-data; name=\"" + name + "\"; ");
        preamble.append("filename=\"" + filename + "\"\r\n");
        preamble.append("Content-Type: " + mime + "\r\n");
        preamble.append("\r\n");

        return preamble.toString().getBytes();
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
        throw new RuntimeException(
                "Not Implemented: reading the content of such entities was not needed until now");
    }

    @Override
    public long getContentLength() {
        return mPreample.length + mDataSize + mEnd.length;
    }

    @Override
    public Header getContentType() {
        return new BasicHeader("Content-Type", "multipart/form-data; boundary=" + BORDER);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public boolean isStreaming() {
        return true;
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        outstream.write(mPreample);

        long uploaded = 0;

        byte[] buffer = new byte[0xFFFF];
        int len;
        InputStream stream = mStreamable.openInputStream();
        while ((len = stream.read(buffer)) != -1) {
            outstream.write(buffer, 0, len);
            uploaded += len;
            if (mStatusCallback != null) {
                double ratio = uploaded / (double) mStreamable.getStreamLength();
                mStatusCallback.onProgress((int) (ratio * 100));
            }
        }

        outstream.write(mEnd);
        if (mStatusCallback != null) {
            mStatusCallback.onSuccess();
        }
    }

    @Override
    public String toString() {
        return new String(mPreample);
    }
}