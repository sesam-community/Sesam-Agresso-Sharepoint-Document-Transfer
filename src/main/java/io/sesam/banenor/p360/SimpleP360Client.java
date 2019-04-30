package io.sesam.banenor.p360;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Timur Samkharadze
 */
@Component
public class SimpleP360Client {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleP360Client.class);

    @Autowired
    P360ServiceConfiguration p360Config;

    public final byte[] downloadFileFromUrl(final String url) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustAllStrategy());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());

        CloseableHttpClient httpclient = HttpClients.custom()
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setSSLSocketFactory(sslsf)
                .build();

        HttpGet fileReq = new HttpGet(url);
        HttpClientContext context = HttpClientContext.create();

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        NTCredentials creds = new NTCredentials(p360Config.getP360Username(), p360Config.getP360Password(), "", p360Config.getDomain());
        credsProvider.setCredentials(AuthScope.ANY, creds);
        List<String> authtypes = new ArrayList<>();
        authtypes.add(AuthSchemes.NTLM);
        RequestConfig requestConfig = RequestConfig.custom().setTargetPreferredAuthSchemes(authtypes).build();
        context.setRequestConfig(requestConfig);
        context.setCredentialsProvider(credsProvider);

        LOG.info("Trying to download file from URL {}", url);
        byte[] fileData = httpclient.execute(fileReq, new FileDownloadResponseHandler(), context);

        if (p360Config.isDebug()) {
            try (FileOutputStream os = new FileOutputStream("tmp_file")) {
                os.write(fileData);
            }

            FileReader fileReader = new FileReader("tmp_file");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String s = bufferedReader.readLine();
            
            while (s != null) {
                LOG.debug(s);
                s = bufferedReader.readLine();
            }
        }

        return fileData;
    }

    /**
     * helper class to download file into byte array
     */
    static class FileDownloadResponseHandler implements ResponseHandler<byte[]> {

        @Override
        public byte[] handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            StatusLine res = response.getStatusLine();
            LOG.info("Request executed with code {} {}", res.getStatusCode(), res.getReasonPhrase());
            if (res.getStatusCode() >= 300) {
                throw new ClientProtocolException(res.getStatusCode() + " " + res.getReasonPhrase());
            }
            try (InputStream source = response.getEntity().getContent()) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int len;

                while ((len = source.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                return os.toByteArray();
            }
        }
    }
}
