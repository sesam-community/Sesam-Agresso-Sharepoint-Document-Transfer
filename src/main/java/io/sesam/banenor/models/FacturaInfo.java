package io.sesam.banenor.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Timur Samkharadze
 */
public class FacturaInfo {

    @JsonProperty("_id")
    public final String _id;

    @JsonProperty("doc_guid")
    public final String doc_guid;

    @JsonProperty("doc_type")
    public final String doc_type;

    @JsonProperty("apar_id")
    public final String apar_id;

    @JsonProperty("client")
    public final String client;

    @JsonProperty("cur_amount")
    public final String cur_amount;

    @JsonProperty("currency")
    public final String currency;

    @JsonProperty("due_date")
    public final String due_date;

    @JsonProperty("last_update")
    public final String last_update;

    @JsonProperty("rest_curr")
    public final String rest_curr;

    @JsonProperty("sequence_no")
    public final int sequence_no;

    @JsonProperty("status")
    public final String status;

    @JsonProperty("voucher_date")
    public final String voucher_date;

    @JsonProperty("voucher_no")
    public final long voucher_no;
    
    @JsonProperty("comp_reg_no")
    public final String comp_reg_no;

    /**
     * operation status returned back with entity
     */
    private String _status;

    private boolean error;

    public boolean isError() {
        return error;
    }

    public void setError(boolean isError) {
        this.error = isError;
    }

    public String getStatus() {
        return _status;
    }

    public void setStatus(String status) {
        this._status = status;
    }

    public FacturaInfo(String _id, String doc_guid, String doc_type, String apar_id, String client, String cur_amount, String currency, String due_date, String last_update, String rest_curr, int sequence_no, String status, String voucher_date, long voucher_no, String comp_reg_no) {
        this._id = _id;
        this.doc_guid = doc_guid;
        this.doc_type = doc_type;
        this.apar_id = apar_id;
        this.client = client;
        this.cur_amount = cur_amount;
        this.currency = currency;
        this.due_date = due_date;
        this.last_update = last_update;
        this.rest_curr = rest_curr;
        this.sequence_no = sequence_no;
        this.status = status;
        this.voucher_date = voucher_date;
        this.voucher_no = voucher_no;
        this.comp_reg_no = comp_reg_no;
    }

    @Override
    public String toString() {
        return "FacturaInfo{" + "_id=" + _id + ", doc_guid=" + doc_guid + ", doc_type=" + doc_type + ", apar_id=" + apar_id + ", client=" + client + ", cur_amount=" + cur_amount + ", currency=" + currency + ", due_date=" + due_date + ", last_update=" + last_update + ", rest_curr=" + rest_curr + ", sequence_no=" + sequence_no + ", status=" + status + ", voucher_date=" + voucher_date + ", voucher_no=" + voucher_no + ", _status=" + _status + ", error=" + error + '}';
    }

}
