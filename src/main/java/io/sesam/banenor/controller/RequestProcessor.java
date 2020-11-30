package io.sesam.banenor.controller;

import agresso.wsdl.DocumentRevisionResponse;
import io.sesam.banenor.models.FacturaInfo;
import io.sesam.banenor.soap.AgressoSoapClient;
import io.sesam.banenor.soap.Credentials;
import io.sesam.banenor.sp.SimpleSharepointClient;
import java.util.List;
import java.util.Base64;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This controller processs requests from Sesam to fetch data from Agressom document archive 
 * @author Timur Samkharadze
 */
@RestController
public class RequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestProcessor.class);
    private static final int SUCCESS = 0;
    
    @Autowired
    Credentials creds;
    
    @Autowired
    AgressoSoapClient wsClient;
    
    @Autowired
    SimpleSharepointClient spClient;
    
    @RequestMapping(value = "/", method = {RequestMethod.POST})
    public List<FacturaInfo> processRequest(@RequestBody List<FacturaInfo> facturaList) {
        
        facturaList.forEach((FacturaInfo f) -> {
            final DocumentRevisionResponse doc;
            if (null == f.doc_guid || null == f.doc_type) {
                f.setStatus("One or both of required parameters (doc_guid, doc_type) are missing, couldn't process this entity.");
                f.setError(true);
                return;
            }
            
            try {
                //dirty hack if Sesam recoginze field as UUID instead of String and adds ~u prefix
                String doc_guid = f.doc_guid;
                if (null != doc_guid && doc_guid.startsWith("~u")) {
                    doc = this.wsClient.getDocumentByIdAndType(doc_guid.replace("~u", ""), f.doc_type, this.creds);
                } else {
                    doc = this.wsClient.getDocumentByIdAndType(doc_guid, f.doc_type, this.creds);
                }
                
                if (SUCCESS != doc.getResponse().getStatus()) {
                    LOG.warn("Couldn't get document " + doc.getResponse().getStatus() + " " + doc.getResponse().getText());
                    f.setStatus(doc.getResponse().getText() + ": " + doc.getResponse().getStatus());
                    f.setError(true);
                    return;
                }
                LOG.debug("Got document "+doc.getRevision().getFileName());
                String fileName = doc.getRevision().getFileName();
                byte[] fileContent = Base64.getDecoder().decode(doc.getRevision().getFileContent());
                spClient.uploadFileContent(fileName, fileContent, f);
                
                f.setError(false);
                f.setStatus(new StringBuilder("Uploaded '" + fileName + "' successfully").append(new Date().toString()).toString());
                
            } catch (Exception e) {
                LOG.error("Something went wrong:", e);
                f.setStatus(e.getMessage());
                f.setError(true);
            }
            
        });
        return facturaList;
    }
    
}
