package sopra.grenoble.jiraLoader.excel.dto;

import org.apache.poi.ss.usermodel.Row;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sopra.grenoble.jiraLoader.excel.loaders.ExcelRowUtils;
import sopra.grenoble.jiraLoader.excel.loaders.XslsFileReaderAndWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cmouilleron
 */
public abstract class GenericModel {

    /**
     * Default logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(GenericModel.class);

    public String key;
    public String typeDemande;
    public String epicName;
    public String versionName;
    public String resume;
    public String clientReference;
    public String descriptif;
    public String priority;
    public String composantName;
    public String estimation;
    public String versionCorrected;
    public String linkTargetName;

    /**
     * @param key
     * @param typeDemande
     * @param epicName
     * @param versionName
     * @param resume
     * @param clientReference
     * @param descriptif
     * @param priority
     * @param composantName
     * @param estimation
     */
    public GenericModel(String key, String typeDemande, String epicName, String versionName, String clientReference, String resume,
                        String descriptif, String priority, String composantName, String estimation, String versionCorrected, String linkTargetName) {
        super();
        this.key = key;
        this.typeDemande = typeDemande;
        this.epicName = epicName;
        this.versionName = versionName;
        this.clientReference = clientReference;
        this.resume = resume;
        this.descriptif = descriptif;
        this.priority = priority;
        this.composantName = composantName;
        this.estimation = estimation;
        this.versionCorrected = versionCorrected;
    }

    /**
     * Default construtor
     */
    public GenericModel() {
        super();
    }

    /***
     * Constructor based on {@link Row}
     * @param row
     */
    public void loadRow(Row row, XslsFileReaderAndWriter excelLoader) {
        this.key = ExcelRowUtils.getStringValueFromRow(row, XslsFileReaderAndWriter.findColumnNumber(excelLoader, "ID")).orElse(null);
        this.typeDemande = ExcelRowUtils.getStringValueFromRow(row, XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Type de demande")).orElse(null);
        this.epicName = ExcelRowUtils.getStringValueFromRow(row, XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Epics")).orElse(null);
        this.versionName = ExcelRowUtils.getStringValueFromRow(row, XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Version affectée")).orElse(null);

        if (XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Version corrigée") != -1) {
            this.versionCorrected = ExcelRowUtils.getStringValueFromRow(row, XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Version corrigée")).orElse(null);
        }
        // when findColumnNumber return -1, that means there is not column with "ColumnName" (Référence Client here).
        if (XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Référence Client") != -1) {
            this.clientReference = ExcelRowUtils.getStringValueFromRow(row, XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Référence Client")).orElse(null);
        }
        this.resume = ExcelRowUtils.getStringValueFromRow(row, XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Résumé")).orElse(null);
        this.descriptif = ExcelRowUtils.getStringValueFromRow(row, XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Descriptif")).orElse(null);
        this.priority = ExcelRowUtils.getStringValueFromRow(row, XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Priorité")).orElse(null);
        this.composantName = ExcelRowUtils.getStringValueFromRow(row, XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Composant")).orElse(null);
        this.estimation = ExcelRowUtils.getStringValueFromRow(row, XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Estimation Originale")).orElse(null);

        if (XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Lien") != -1) {
            this.linkTargetName = ExcelRowUtils.getStringValueFromRow(row, XslsFileReaderAndWriter.findColumnNumber(excelLoader, "Lien")).orElse(null);
        }

        if ("Story".equals(this.typeDemande) && this.clientReference != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.clientReference + " | " + this.resume);
            this.resume = stringBuilder.toString();
        }
    }

    @Override
    public String toString() {
        ObjectMapper objMap = new ObjectMapper();
        try {
            return objMap.writeValueAsString(this);
        } catch (IOException e) {
            LOG.warn("Unable to parse dto object in JSON", e);
        }
        return "";
    }

    /**
     * Validate the file
     *
     * @return
     */
    public abstract boolean validate();

}
