package io.sesam.banenor.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Entity Shape for p360 file integrations
 *
 * @author Timur Samkharadze <timur.samkharadze@sysco.no>
 */
@JsonInclude(Include.ALWAYS)
public class FileUrlWithMetadataEntity {

    @JsonProperty("source")
    public final String source;

    @JsonProperty("filename")
    public final String filename;

    @JsonProperty("url")
    public final String url;

    @JsonProperty("metadata")
    public final List<Map<String, String>> metadata;

    @JsonProperty("_deleted")
    public final boolean deleted;

    public FileUrlWithMetadataEntity(String source, String filename, String url, List<Map<String, String>> metadata, boolean deleted) {
        this.source = source;
        this.filename = filename;
        this.url = url;
        this.metadata = metadata;
        this.deleted = deleted;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.source);
        hash = 97 * hash + Objects.hashCode(this.filename);
        hash = 97 * hash + Objects.hashCode(this.url);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileUrlWithMetadataEntity other = (FileUrlWithMetadataEntity) obj;
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        if (!Objects.equals(this.filename, other.filename)) {
            return false;
        }
        return Objects.equals(this.url, other.url);
    }

    @Override
    public String toString() {
        return "FileUrlWithMetadataEntity{" + "source=" + source + ", filename=" + filename + ", url=" + url + ", metadata=" + metadata + ", deleted=" + deleted + '}';
    }

}
