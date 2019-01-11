package io.sesam.banenor.soap;

import agresso.wsdl.DocumentRequest;
import agresso.wsdl.DocumentRevisionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

import agresso.wsdl.GetDocumentRevision;
import agresso.wsdl.GetDocumentRevisionResponse;
import agresso.wsdl.WSCredentials;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;

/**
 *
 * @author Timur Samkharadze
 */
@Component
public class AgressoSoapClient extends WebServiceGatewaySupport {

    private static final Logger LOG = LoggerFactory.getLogger(AgressoSoapClient.class);
    @Autowired
    ServiceConfiguration config;

    public DocumentRevisionResponse getDocumentByIdAndType(final String docId, final String docType, Credentials creds) {
        LOG.info("Serving request for doc id {}", docId);

        DocumentRequest docRequest = new DocumentRequest();
        WSCredentials credentials = new WSCredentials();

        credentials.setClient(creds.getClient());
        credentials.setUsername(creds.getUsername());
        credentials.setPassword(creds.getPassword());

        docRequest.setDocId(docId);
        docRequest.setDocType(docType);

        GetDocumentRevision payload = new GetDocumentRevision();
        payload.setCredentials(credentials);
        payload.setRequest(docRequest);

        Jaxb2Marshaller m = new Jaxb2Marshaller();
        m.setContextPath("agresso.wsdl");

        NamespacePrefixMapper mapper = new NamespacePrefixMapper() {
            @Override
            public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
                return "doc";
            }
        };
        Map<String, Object> props = new HashMap();
        props.put("jaxb.formatted.output", Boolean.TRUE);
        props.put("com.sun.xml.bind.namespacePrefixMapper", mapper);
        m.setMarshallerProperties(props);

        WebServiceTemplate template = getWebServiceTemplate();
        template.setMarshaller(m);
        template.setUnmarshaller(m);

        GetDocumentRevisionResponse response = (GetDocumentRevisionResponse) template
                .marshalSendAndReceive(config.getUrl(), payload, new SoapActionCallback(config.getAction()));
        return response.getGetDocumentRevisionResult();
    }
}
