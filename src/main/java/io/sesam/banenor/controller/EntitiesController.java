package io.sesam.banenor.controller;

import io.sesam.banenor.models.FileUrlWithMetadataEntity;
import io.sesam.banenor.p360.P360ServiceConfiguration;
import io.sesam.banenor.p360.SimpleP360Client;
import io.sesam.banenor.sp.SimpleSharepointClient;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller processing p360 related requests from Sesam
 *
 * @author Timur Samkharadze <timur.samkharadze@sysco.no>
 *
 *
 */
@RestController
public class EntitiesController {

    private static final Logger LOG = LoggerFactory.getLogger(RequestProcessor.class);

    @Autowired
    SimpleP360Client p360Client;

    @Autowired
    SimpleSharepointClient spClient;

    @Autowired
    P360ServiceConfiguration p360Config;

    /**
     * Structure of json to post to service: { "source": [NAME OF DATASOURCE], "filename": [NAME OF FILE], "url": [URL
     * TO FILE], "metadata": [ { [METADATA KEY 1]: [VALUE OF METADATA KEY 1] }, { [METADATA KEY 2]: [VALUE OF METADATA
     * KEY 2] }, ..] }
     *
     * Example using curl: curl --data '[{"source": "p360", "filename": "loremipsum.docx", "url":
     * "http://www.loremipsum.com/file.docx", "metadata": [{"Author":"Alan Smithee"}, {"Documenttype":"Word document
     * (docx)"}]}]' http://localhost:8000/api/entities
     *
     * To delete a file send a json only containing the filename or with _deleted property equal to true. 
     * Example: curl --data '[{"filename": "loremipsum.docx"}]' http://localhost:8000/api/entities
     *
     * @param p360Files json array
     * @return json array
     * @throws org.springframework.boot.configurationprocessor.json.JSONException
     */
    @RequestMapping(value = "/api/p360/entities", method = {RequestMethod.POST})
    public final List<FileUrlWithMetadataEntity> processRequest(@RequestBody List<FileUrlWithMetadataEntity> p360Files)
            throws JSONException, Exception {
        for (FileUrlWithMetadataEntity item : p360Files) {
            if (item.deleted || item.url == null) {
                LOG.info("Deleting {} from SharePoint", item.filename);
                spClient.deleteFileFromLibrary(item.filename, p360Config.getP360SpLibraryName());
                continue;
            }
            LOG.info("Syncing {} to Sharepoint", item.filename);
            LOG.info("Trying to fetch file from url {}",item.url);
            byte[] fileArray = p360Client.downloadFileFromUrl(item.url);
            LOG.info("Downloaded file of size {}", fileArray.length);
            spClient.uploadFileContentWithMetadata( p360Config.getP360SpLibraryName(),item.filename, fileArray, item.metadata);

        }
        return p360Files;
    }
}
