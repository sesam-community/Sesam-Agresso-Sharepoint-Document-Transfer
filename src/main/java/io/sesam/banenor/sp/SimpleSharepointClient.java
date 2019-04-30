package io.sesam.banenor.sp;

import io.sesam.banenor.models.FacturaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * Sharepoint on Premis 1. use NTLM auth 2. Obtain FormDigest and use them (whith auth context) in all subsequent
 * requests
 *
 * @author 100tsa
 */
@Component
public class SimpleSharepointClient {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleSharepointClient.class);

    @Autowired
    private SpServiceConfiguration spConfig;

    public void uploadFileContent(final String fileName, final byte[] fileContent, FacturaInfo factura) throws IOException, Exception {
//        String url = "_api/web/GetFolderByServerRelativeUrl('/" + this.libName + "')/Files/add(url='" + fileName + "',overwrite=true)";
//uncomment for SSL
//        SSLContextBuilder builder = new SSLContextBuilder();
//        builder.loadTrustMaterial(null, (X509Certificate[] certificate, String authType) -> {
//            return true;
//        });
//        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
        CloseableHttpClient httpclient = HttpClients.custom()
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                //.setSSLSocketFactory(sslsf).disableRedirectHandling()
                .build();

        HttpHost target = new HttpHost(spConfig.getBaseUrl(), 80, "http");
        HttpClientContext context = HttpClientContext.create();

        String formDigest;

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, new NTCredentials(spConfig.getSpUsername(), spConfig.getSpPassword(), "", ""));
        List<String> authtypes = new ArrayList<>();
        authtypes.add(AuthSchemes.NTLM);
        RequestConfig requestConfig = RequestConfig.custom().setTargetPreferredAuthSchemes(authtypes).build();
        context.setRequestConfig(requestConfig);
        context.setCredentialsProvider(credsProvider);

        HttpPost request1 = new HttpPost("/_api/contextinfo");
        request1.addHeader("Accept", "application/json;odata=verbose");
        LOG.info("Send request to obtain FormDigest to {}{}", target.toURI(), request1.getURI());

        Arrays.asList(request1.getAllHeaders()).forEach((header) -> {
            LOG.info("Header: {}", header);
        });
        //obtain auth context
        try (CloseableHttpResponse response1 = httpclient.execute(target, request1, context)) {
            HttpEntity res = response1.getEntity();
            LOG.info("Got response with status code: {} {}", response1.getStatusLine().getStatusCode(), response1.getStatusLine().getReasonPhrase());
            LOG.info("Got Form digest entity {} with content-length {}", res, res.getContentLength());
            Arrays.asList(response1.getAllHeaders()).forEach((header) -> {
                LOG.info("Header: {}", header);
            });

            String json = EntityUtils.toString(res, StandardCharsets.UTF_8);
            LOG.info("Form Digest response content: {}", json);

            JSONObject resObj = new JSONObject(json);
            formDigest = resObj.getJSONObject("d")
                    .getJSONObject("GetContextWebInformation").getString("FormDigestValue");

            LOG.info("got form digest value {}...masked...", formDigest.substring(0, 10));

            EntityUtils.consume(res);
            if (response1.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                LOG.warn("{} {}", response1.getStatusLine().getStatusCode(), response1.getStatusLine().getReasonPhrase());
                throw new Exception(response1.getStatusLine().getReasonPhrase());
            }
        } finally {
            request1.reset();
        }

        // File upload - will rewrite if exists with this name
        HttpPost request2 = new HttpPost(String.format("/_api/web/GetFolderByServerRelativeUrl('/%s')/Files/add(url='%s',overwrite=true)", spConfig.getLibrary(), fileName));
        request2.setEntity(new ByteArrayEntity(fileContent));// source
        request2.addHeader("Content-type", "application/json;odata=verbose");
        request2.addHeader("X-RequestDigest", formDigest);
        request2.addHeader("Accept", "application/json;odata=verbose");

        String itemRelativeUrl;

        LOG.info("Send request to store document");
        try (CloseableHttpResponse response2 = httpclient.execute(target, request2, context)) {
            HttpEntity res = response2.getEntity();
            String json = EntityUtils.toString(res, StandardCharsets.UTF_8);
            JSONObject resObj = new JSONObject(json);
            itemRelativeUrl = resObj.getJSONObject("d").getString("ServerRelativeUrl");
            LOG.info("response content: {}", json);
            LOG.info("Got response {} with content-length {}", response2.getEntity(), response2.getEntity().getContentLength());
            Arrays.asList(response2.getAllHeaders()).forEach((header) -> {
                LOG.debug("Response header: {}", header);
            });
            EntityUtils.consume(response2.getEntity());
            int rc = response2.getStatusLine().getStatusCode();
            String reason = response2.getStatusLine().getReasonPhrase();

            switch (rc) {
                case HttpStatus.SC_CREATED:
                    LOG.info("{} is copied (new file created)", fileName);
                    break;
                case HttpStatus.SC_OK:
                    LOG.info("{} is copied (original overwritten)", fileName);
                    break;
                default:
                    throw new Exception("Problem while copying " + fileName + " reason" + reason + " httpcode: " + rc);
            }
        } finally {
            request2.reset();
        }

        String listItemEntityFullName;
        //now we need to obtain ListItemEntityTypeFullName for this list
        HttpGet getListItemEntityRequest = new HttpGet(String.format("/_api/web/lists/GetByTitle('%s')", spConfig.getLibrary()));
        getListItemEntityRequest.addHeader("X-RequestDigest", formDigest);
        getListItemEntityRequest.addHeader("Accept", "application/json;odata=verbose");
        LOG.info("Send request to obtain list item full name");
        try (CloseableHttpResponse res = httpclient.execute(target, getListItemEntityRequest, context)) {
            HttpEntity responseEntity = res.getEntity();
            String json = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
            JSONObject resObj = new JSONObject(json);
            listItemEntityFullName = resObj.getJSONObject("d").getString("ListItemEntityTypeFullName");
            LOG.info("Response: {}", json);
        } finally {
            getListItemEntityRequest.reset();
        }
        LOG.info("ListItemEntityTypeFullName: {}", listItemEntityFullName);
        //upload metadata for file

        HttpPost updateFileMetadataRequest = new HttpPost(String.format("/_api/web/GetFileByServerRelativeUrl('%s')/ListItemAllFields", itemRelativeUrl));
        updateFileMetadataRequest.addHeader("X-RequestDigest", formDigest);
        updateFileMetadataRequest.addHeader("X-HTTP-Method", "MERGE");
        updateFileMetadataRequest.addHeader("IF-MATCH", "*");
        updateFileMetadataRequest.addHeader("Accept", "application/json;odata=verbose");

        JSONObject payload = new JSONObject();
        JSONObject __metadata = new JSONObject();
        __metadata.put("type", listItemEntityFullName);
        payload.put("__metadata", __metadata);

        if (null != factura.cur_amount) {
            payload.put("Sum", factura.cur_amount.replace("~f", ""));
        }
        if (null != factura.doc_guid) {
            payload.put("DokumentId", factura.doc_guid.replace("~u", ""));
        }
        payload.put("FakturaDokumentType", factura.doc_type);
        if (null != factura.voucher_date) {
            payload.put("RegDato", factura.voucher_date.replace("~t", ""));
        }
        if (null != factura.due_date) {
            payload.put("FakturaForfallsdato", factura.due_date.replace("~t", ""));
        }
        payload.put("Kundeidentifikator", factura.comp_reg_no);
        if (null != factura.last_update) {
            payload.put("Tidsstempel", factura.last_update.replace("~t", ""));
        }
        payload.put("Organisasjonsnummer", factura.comp_reg_no);

        LOG.info("Payload to send: {}", payload);

        updateFileMetadataRequest.addHeader("Content-Type", "application/json; odata=verbose");

        updateFileMetadataRequest.setEntity(new ByteArrayEntity(payload.toString().getBytes(StandardCharsets.UTF_8)));
        LOG.info("Send request to update file metadata");
        try (CloseableHttpResponse res = httpclient.execute(target, updateFileMetadataRequest, context)) {
            HttpEntity responseEntity = res.getEntity();
            if (null != responseEntity) {
                String json = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                LOG.info("Response: {}", json);
            }
        } finally {
            updateFileMetadataRequest.reset();
        }
    }

    public void deleteFileFromLibrary(String fileName, String libraryPath) throws IOException, JSONException, Exception {
        CloseableHttpClient httpclient = HttpClients.custom()
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                //.setSSLSocketFactory(sslsf).disableRedirectHandling()
                .build();

        HttpHost target = new HttpHost(spConfig.getBaseUrl(), 80, "http");
        HttpClientContext context = HttpClientContext.create();

        String formDigest;

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, new NTCredentials(spConfig.getSpUsername(), spConfig.getSpPassword(), "", ""));
        List<String> authtypes = new ArrayList<>();
        authtypes.add(AuthSchemes.NTLM);
        RequestConfig requestConfig = RequestConfig.custom().setTargetPreferredAuthSchemes(authtypes).build();
        context.setRequestConfig(requestConfig);
        context.setCredentialsProvider(credsProvider);

        HttpPost request1 = new HttpPost("/_api/contextinfo");
        request1.addHeader("Accept", "application/json;odata=verbose");
        LOG.info("Send request to obtain FormDigest to {}{}", target.toURI(), request1.getURI());

        Arrays.asList(request1.getAllHeaders()).forEach((header) -> {
            LOG.info("Header: {}", header);
        });

        //obtain auth context
        try (CloseableHttpResponse response1 = httpclient.execute(target, request1, context)) {
            HttpEntity res = response1.getEntity();
            LOG.info("Got Form digest entity {} with content-length {}", res, res.getContentLength());
            Arrays.asList(response1.getAllHeaders()).forEach((header) -> {
                LOG.info("Header: {}", header);
            });

            String json = EntityUtils.toString(res, StandardCharsets.UTF_8);
            LOG.info("Form Digest response content: {}", json);

            JSONObject resObj = new JSONObject(json);
            formDigest = resObj.getJSONObject("d")
                    .getJSONObject("GetContextWebInformation").getString("FormDigestValue");

            LOG.info("got form digest value {}...masked...", formDigest.substring(0, 10));

            EntityUtils.consume(res);
            if (response1.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                LOG.warn("{} {}", response1.getStatusLine().getStatusCode(), response1.getStatusLine().getReasonPhrase());
                throw new Exception(response1.getStatusLine().getReasonPhrase());
            }
        } finally {
            request1.reset();
        }

        //request to delete file
        HttpPost deleteFileReq = new HttpPost(String.format("/_api/web/GetFileByServerRelativeUrl('%s')/%s", libraryPath, fileName));
        deleteFileReq.addHeader("X-RequestDigest", formDigest);
        deleteFileReq.addHeader("X-HTTP-Method", "DELETE");

        LOG.info("Send request to delete file {} from library {}", fileName, libraryPath);
        try (CloseableHttpResponse response2 = httpclient.execute(target, deleteFileReq, context)) {
            EntityUtils.consume(response2.getEntity());
            int rc = response2.getStatusLine().getStatusCode();
            String reason = response2.getStatusLine().getReasonPhrase();

            switch (rc) {
                case HttpStatus.SC_CREATED:
                    LOG.info("{} is copied (new file created)", fileName);
                    break;
                case HttpStatus.SC_OK:
                    LOG.info("{} is copied (original overwritten)", fileName);
                    break;
                default:
                    throw new Exception("Problem while copying " + fileName + " reason" + reason + " httpcode: " + rc);
            }
        } finally {
            deleteFileReq.reset();
        }

    }

    public void uploadFileContentWithMetadata(final String spLibName, final String fileName, final byte[] fileContent, final List<Map<String, String>> metadata) throws IOException, JSONException, Exception {
//        String url = "_api/web/GetFolderByServerRelativeUrl('/" + this.libName + "')/Files/add(url='" + fileName + "',overwrite=true)";
//uncomment for SSL
//        SSLContextBuilder builder = new SSLContextBuilder();
//        builder.loadTrustMaterial(null, (X509Certificate[] certificate, String authType) -> {
//            return true;
//        });
//        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
        CloseableHttpClient httpclient = HttpClients.custom()
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                //.setSSLSocketFactory(sslsf).disableRedirectHandling()
                .build();

        HttpHost target = new HttpHost(spConfig.getBaseUrl(), 80, "http");
        HttpClientContext context = HttpClientContext.create();

        String formDigest;

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, new NTCredentials(spConfig.getSpUsername(), spConfig.getSpPassword(), "", ""));
        List<String> authtypes = new ArrayList<>();
        authtypes.add(AuthSchemes.NTLM);
        RequestConfig requestConfig = RequestConfig.custom().setTargetPreferredAuthSchemes(authtypes).build();
        context.setRequestConfig(requestConfig);
        context.setCredentialsProvider(credsProvider);

        HttpPost request1 = new HttpPost("/_api/contextinfo");
        request1.addHeader("Accept", "application/json;odata=verbose");
        LOG.info("Send request to obtain FormDigest to {}{}", target.toURI(), request1.getURI());

        Arrays.asList(request1.getAllHeaders()).forEach((header) -> {
            LOG.info("Header: {}", header);
        });
        //obtain auth context
        try (CloseableHttpResponse response1 = httpclient.execute(target, request1, context)) {
            HttpEntity res = response1.getEntity();
            
            LOG.info("Got response with status code: {} {}", response1.getStatusLine().getStatusCode(), response1.getStatusLine().getReasonPhrase());
            LOG.info("Got Form digest entity {} with content-length {}", res, res.getContentLength());
            Arrays.asList(response1.getAllHeaders()).forEach((header) -> {
                LOG.info("Header: {}", header);
            });

            String json = EntityUtils.toString(res, StandardCharsets.UTF_8);
            LOG.info("Form Digest response content: {}", json);

            JSONObject resObj = new JSONObject(json);
            formDigest = resObj.getJSONObject("d")
                    .getJSONObject("GetContextWebInformation").getString("FormDigestValue");

            LOG.info("got form digest value {}...masked...", formDigest.substring(0, 10));

            EntityUtils.consume(res);
            if (response1.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                LOG.warn("{} {}", response1.getStatusLine().getStatusCode(), response1.getStatusLine().getReasonPhrase());
                throw new Exception(response1.getStatusLine().getReasonPhrase());
            }
        } finally {
            request1.reset();
        }

        // File upload - will rewrite if exists with this name
        HttpPost request2 = new HttpPost(String.format("/_api/web/GetFolderByServerRelativeUrl('/%s')/Files/add(url='%s',overwrite=true)", spLibName, fileName));
        request2.setEntity(new ByteArrayEntity(fileContent));// source
        request2.addHeader("Content-type", "application/json;odata=verbose");
        request2.addHeader("X-RequestDigest", formDigest);
        request2.addHeader("Accept", "application/json;odata=verbose");

        String itemRelativeUrl;

        LOG.info("Send request to store document");
        try (CloseableHttpResponse response2 = httpclient.execute(target, request2, context)) {
            HttpEntity res = response2.getEntity();
            String json = EntityUtils.toString(res, StandardCharsets.UTF_8);
            JSONObject resObj = new JSONObject(json);
            itemRelativeUrl = resObj.getJSONObject("d").getString("ServerRelativeUrl");
            LOG.info("response content: {}", json);
            LOG.info("Got response {} with content-length {}", response2.getEntity(), response2.getEntity().getContentLength());
            Arrays.asList(response2.getAllHeaders()).forEach((header) -> {
                LOG.debug("Response header: {}", header);
            });
            EntityUtils.consume(response2.getEntity());
            int rc = response2.getStatusLine().getStatusCode();
            String reason = response2.getStatusLine().getReasonPhrase();

            switch (rc) {
                case HttpStatus.SC_CREATED:
                    LOG.info("{} is copied (new file created)", fileName);
                    break;
                case HttpStatus.SC_OK:
                    LOG.info("{} is copied (original overwritten)", fileName);
                    break;
                default:
                    throw new Exception("Problem while copying " + fileName + " reason" + reason + " httpcode: " + rc);
            }
        } finally {
            request2.reset();
        }

        String listItemEntityFullName;
        //now we need to obtain ListItemEntityTypeFullName for this list
        HttpGet getListItemEntityRequest = new HttpGet(String.format("/_api/web/lists/GetByTitle('%s')", spLibName));
        getListItemEntityRequest.addHeader("X-RequestDigest", formDigest);
        getListItemEntityRequest.addHeader("Accept", "application/json;odata=verbose");
        LOG.info("Send request to obtain list item full name");
        try (CloseableHttpResponse res = httpclient.execute(target, getListItemEntityRequest, context)) {
            HttpEntity responseEntity = res.getEntity();
            String json = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
            JSONObject resObj = new JSONObject(json);
            listItemEntityFullName = resObj.getJSONObject("d").getString("ListItemEntityTypeFullName");
            LOG.info("Response: {}", json);
        } finally {
            getListItemEntityRequest.reset();
        }
        LOG.info("ListItemEntityTypeFullName: {}", listItemEntityFullName);
        //upload metadata for file

        HttpPost updateFileMetadataRequest = new HttpPost(String.format("/_api/web/GetFileByServerRelativeUrl('%s')/ListItemAllFields", itemRelativeUrl));
        updateFileMetadataRequest.addHeader("X-RequestDigest", formDigest);
        updateFileMetadataRequest.addHeader("X-HTTP-Method", "MERGE");
        updateFileMetadataRequest.addHeader("IF-MATCH", "*");
        updateFileMetadataRequest.addHeader("Accept", "application/json;odata=verbose");

        JSONObject payload = new JSONObject();
        JSONObject __metadata = new JSONObject();
        __metadata.put("type", listItemEntityFullName);
        payload.put("__metadata", __metadata);

        metadata.forEach((Map<String, String> map) -> {
            map.forEach((String k, String v) -> {
                try {
                    payload.put(k, v);
                } catch (JSONException ex) {
                    LOG.error(ex.getMessage());
                }
            });
        });

        LOG.info("Payload to send: {}", payload);

        updateFileMetadataRequest.addHeader("Content-Type", "application/json; odata=verbose");

        updateFileMetadataRequest.setEntity(new ByteArrayEntity(payload.toString().getBytes(StandardCharsets.UTF_8)));
        LOG.info("Send request to update file metadata");
        try (CloseableHttpResponse res = httpclient.execute(target, updateFileMetadataRequest, context)) {
            HttpEntity responseEntity = res.getEntity();
            if (null != responseEntity) {
                String json = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                LOG.info("Response: {}", json);
            }
        } finally {
            updateFileMetadataRequest.reset();
        }
    }

}
