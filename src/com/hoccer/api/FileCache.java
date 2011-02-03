/*******************************************************************************
 * Copyright (C) 2009, 2010, Hoccer GmbH Berlin, Germany <www.hoccer.com>
 * 
 * These coded instructions, statements, and computer programs contain
 * proprietary information of Hoccer GmbH Berlin, and are copy protected
 * by law. They may be used, modified and redistributed under the terms
 * of GNU General Public License referenced below. 
 *    
 * Alternative licensing without the obligations of the GPL is
 * available upon request.
 * 
 * GPL v3 Licensing:
 * 
 * This file is part of the "Linccer Java-API".
 * 
 * Linccer Java-API is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Linccer Java-API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Linccer Java-API. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.hoccer.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.UUID;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;

import com.hoccer.data.GenericStreamableContent;
import com.hoccer.data.StreamableContent;
import com.hoccer.http.AsyncHttpGet;
import com.hoccer.http.AsyncHttpPut;
import com.hoccer.http.AsyncHttpRequest;
import com.hoccer.http.HttpResponseHandler;

public class FileCache extends CloudService {

    private final HashMap<String, AsyncHttpRequest> mOngoingRequests = new HashMap<String, AsyncHttpRequest>();

    public FileCache(ClientConfig config) {
        super(config);
    }

    public HashMap<String, AsyncHttpRequest> getOngoingRequests() {
        for (String uri : mOngoingRequests.keySet()) {
            if (mOngoingRequests.get(uri).isTaskCompleted()) {
                mOngoingRequests.remove(uri);
            }
        }

        return mOngoingRequests;
    }

    public String store(StreamableContent data, int secondsUntilExipred)
            throws ClientProtocolException, IOException {

        String url = ClientConfig.getFileCacheBaseUri() + "/" + UUID.randomUUID() + "/?expires_in="
                + secondsUntilExipred;
        HttpPut request = new HttpPut(sign(url));

        InputStreamEntity entity = new InputStreamEntity(data.openInputStream(), data
                .getStreamLength());
        request.addHeader("Content-Disposition", " attachment; filename=\"" + data.getFilename()
                + "\"");
        entity.setContentType(data.getContentType());
        request.setEntity(entity);
        request.addHeader("Content-Type", data.getContentType());

        HttpResponse response = getHttpClient().execute(request);

        return convertResponseToString(response);
    }

    public void fetch(String locationUri, StreamableContent data) throws ClientProtocolException,
            IOException {
        HttpGet request = new HttpGet(locationUri);
        HttpResponse response = getHttpClient().execute(request);

        int statuscode = response.getStatusLine().getStatusCode();
        if (statuscode != 200) {
            throw new HttpResponseException(statuscode, "Unexpected status code " + statuscode
                    + " while requesting " + locationUri);
        }

        InputStream is = response.getEntity().getContent();
        OutputStream storageStream = data.openOutputStream();
        byte[] buffer = new byte[0xFFFF];
        int len;
        while ((len = is.read(buffer)) != -1) {
            storageStream.write(buffer, 0, len);
        }

        if (data instanceof GenericStreamableContent) {
            Header[] headers = response.getAllHeaders();
            HashMap<String, String> headermap = new HashMap<String, String>();
            for (int i = 0; i < headers.length; i++) {
                headermap.put(headers[i].getName(), headers[i].getValue());
            }
            AsyncHttpRequest.setTypeAndFilename((GenericStreamableContent) data, headermap,
                    locationUri);
        }
    }

    public String asyncStore(StreamableContent data, int secondsUntilExipred,
            HttpResponseHandler responseHandler) throws IOException {

        String uri = ClientConfig.getFileCacheBaseUri() + "/" + UUID.randomUUID();

        AsyncHttpPut storeRequest = new AsyncHttpPut(sign(uri + "?expires_in="
                + secondsUntilExipred));
        storeRequest.registerResponseHandler(responseHandler);
        storeRequest.setBody(data);
        storeRequest.start();
        mOngoingRequests.put(uri, storeRequest);

        return uri;
    }

    public void asyncFetch(String uri, StreamableContent sink, HttpResponseHandler responseHandler) {
        AsyncHttpGet fetchRequest = new AsyncHttpGet(uri);
        fetchRequest.registerResponseHandler(responseHandler);
        fetchRequest.setStreamableContent(sink);
        fetchRequest.start();
        mOngoingRequests.put(uri, fetchRequest);
    }

    public void cancel(String uri) {
        AsyncHttpRequest request = mOngoingRequests.remove(uri);
        if (request != null) {
            request.interrupt();
        }
    }
}
