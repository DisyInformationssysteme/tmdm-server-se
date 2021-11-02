/*
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package org.talend.mdm.bulkload.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

/**
 * Bulkload amount items client
 *
 */
public class BulkloadClientUtil {

    public static final Integer MAX_HTTP_REQUESTS;
    public static final String CLIENT_CONNECTION_TIMEOUT = "ws_client_connection_timeout"; //$NON-NLS-1$
    public static final String CLIENT_SOCKET_TIMEOUT = "ws_client_receive_timeout"; //$NON-NLS-1$

    static {
        String httpRequests = System.getProperty("bulkload.concurrent.http.requests");//$NON-NLS-1$
        MAX_HTTP_REQUESTS = httpRequests == null? Integer.MAX_VALUE : Integer.parseInt(httpRequests);
    }

    public static void bulkload(String url, String cluster, String concept, String datamodel, boolean validate, boolean smartpk,
            boolean insertonly, boolean updateReport, String source, InputStream itemdata, String username, String password,
            String transactionId, List<String> cookies, String tokenKey, String tokenValue) throws Exception {

        HttpPut httpPut = new HttpPut(url);
        String auth = username + ":" + password;
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        httpPut.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        httpPut.setHeader("Content-Type", "text/xml; charset=utf8"); //$NON-NLS-1$ //$NON-NLS-2$
        if (transactionId != null) {
            httpPut.setHeader("transaction-id", transactionId); //$NON-NLS-1$
        }
        if (cookies != null) {
            for (String cookie : cookies) {
                httpPut.setHeader("Cookie", cookie); //$NON-NLS-1$
            }
        }
        if (tokenKey != null && tokenValue != null) {
            httpPut.setHeader(tokenKey, tokenValue);
        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(getClientTimeout(CLIENT_CONNECTION_TIMEOUT))
                .setConnectTimeout(getClientTimeout(CLIENT_CONNECTION_TIMEOUT))
                .setSocketTimeout(getClientTimeout(CLIENT_SOCKET_TIMEOUT))
                .setExpectContinueEnabled(true)
                .build();

        CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .disableCookieManagement()
                .build();

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("cluster", cluster)); //$NON-NLS-1$
        params.add(new BasicNameValuePair("concept", concept)); //$NON-NLS-1$
        params.add(new BasicNameValuePair("datamodel", datamodel)); //$NON-NLS-1$
        params.add(new BasicNameValuePair("validate", String.valueOf(validate))); //$NON-NLS-1$
        params.add(new BasicNameValuePair("action", "load")); //$NON-NLS-1$
        params.add(new BasicNameValuePair("smartpk", String.valueOf(smartpk))); //$NON-NLS-1$
        params.add(new BasicNameValuePair("insertonly", String.valueOf(insertonly))); //$NON-NLS-1$
        params.add(new BasicNameValuePair("updateReport", String.valueOf(updateReport))); //$NON-NLS-1$
        params.add(new BasicNameValuePair("source", String.valueOf(source))); //$NON-NLS-1$

        URI uri = new URIBuilder(httpPut.getURI()).addParameters(params).build();
        ((HttpRequestBase) httpPut).setURI(uri);

        InputStreamEntity content = new InputStreamEntity(itemdata);
        content.setChunked(true);
        httpPut.setEntity(content);

        CloseableHttpResponse response = client.execute(httpPut);
        if (itemdata instanceof InputStreamMerger) {
            ((InputStreamMerger) itemdata).setAlreadyProcessed(true);
        }
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 500) {
            HttpEntity entity = response.getEntity();
            String responseResult = StringUtils.EMPTY;
            if (entity != null) {
                BufferedReader bufferedReader = null;
                try {
                    InputStream inputStream = entity.getContent();
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    responseResult = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
                    bufferedReader.close();
                } catch (Exception e) {
                    throw e;
                } finally {
                    // releases system resources associated with this stream
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                }
            }
            throw new BulkloadException(responseResult);
        } else if (statusCode >= 400) {
            throw new BulkloadException("Could not send data to MDM (HTTP status code: " + statusCode + ")."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        try {
            response.close();
            client.close();
        } catch (IOException e) {
            throw e;
        }
    }

    private static int getClientTimeout(String property) throws Exception {
        int defaultTimeout = 60000;
        String inputTimeout = System.getProperty(property);
        if (inputTimeout != null) {
            try {
                int timeout = Integer.parseInt(inputTimeout);
                if (timeout > 0) {
                    return timeout;
                }
            } catch (Exception exception) {
                throw new RuntimeException(property + " property value '" + inputTimeout + "' is invalid", exception);  //$NON-NLS-1$//$NON-NLS-2$
            }
        }
        return defaultTimeout;
    }

    public static InputStreamMerger bulkload(String url, String cluster, String concept, String dataModel, boolean validate,
            boolean smartPK, boolean insertOnly, boolean updateReport, String source, String username, String password,
            String transactionId, List<String> cookies, String tokenKey, String tokenValue,
            AtomicInteger startedBulkloadCount) {
        InputStreamMerger merger = new InputStreamMerger();
        while (startedBulkloadCount.get() >= MAX_HTTP_REQUESTS) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new BulkloadException("Waitting to create bulkload thread meets exception."); //$NON-NLS-1$
            }
        }
        Runnable loadRunnable = new AsyncLoadRunnable(url, cluster, concept, dataModel, validate, smartPK, insertOnly,
                updateReport, source, merger, username, password, transactionId, cookies, tokenKey, tokenValue,
                startedBulkloadCount, merger.getReadyRead());
        Thread loadThread = new Thread(loadRunnable);
        loadThread.start();
        return merger;
    }

    private static class AsyncLoadRunnable implements Runnable {

        private final String url;

        private final String cluster;

        private final String concept;

        private final String dataModel;

        private final boolean validate;

        private final boolean smartPK;

        private final boolean insertOnly;

        private final boolean updateReport;

        private final String source;

        private final InputStreamMerger inputStream;

        private final String userName;

        private final String password;

        private final String transactionId;

        private final List<String> cookies;

        private final String tokenKey;

        private final String tokenValue;

        private final AtomicInteger startedBulkloadCount;

        private final CountDownLatch readyRead;

        public AsyncLoadRunnable(String url, String cluster, String concept, String dataModel, boolean validate, boolean smartPK,
                boolean insertOnly, boolean updateReport, String source, InputStreamMerger inputStream, String userName,
                String password, String transactionId, List<String> cookies, String tokenKey, String tokenValue,
                AtomicInteger startedBulkloadCount, CountDownLatch readyRead) {
            this.url = url;
            this.cluster = cluster;
            this.concept = concept;
            this.dataModel = dataModel;
            this.validate = validate;
            this.smartPK = smartPK;
            this.insertOnly = insertOnly;
            this.updateReport = updateReport;
            this.source = source;
            this.inputStream = inputStream;
            this.userName = userName;
            this.password = password;
            this.transactionId = transactionId;
            this.cookies = cookies;
            this.tokenKey = tokenKey;
            this.tokenValue = tokenValue;
            this.startedBulkloadCount = startedBulkloadCount;
            this.readyRead = readyRead;
        }

        @Override
        public void run() {
            try {
                readyRead.await();
                startedBulkloadCount.incrementAndGet();
                bulkload(url, cluster, concept, dataModel, validate, smartPK, insertOnly, updateReport, source, inputStream,
                        userName, password, transactionId, cookies, tokenKey, tokenValue);
            } catch (Throwable e) {
                inputStream.reportFailure(e);
            } finally {
                startedBulkloadCount.decrementAndGet();
                synchronized (startedBulkloadCount) {
                    startedBulkloadCount.notifyAll();
                }
            }
        }
    }
}