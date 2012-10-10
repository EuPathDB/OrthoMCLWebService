/**
 * 
 */
package org.orthomcl.model.wsfplugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gusdb.wsf.plugin.AbstractPlugin;
import org.gusdb.wsf.plugin.WsfRequest;
import org.gusdb.wsf.plugin.WsfResponse;
import org.gusdb.wsf.plugin.WsfServiceException;
import org.gusdb.wsf.util.Formatter;

/**
 * @author jerric
 * 
 */
public class BlastPlugin extends AbstractPlugin {

    private static final String FILE_PROPERTY = "blast-config.xml";
    private static final String FILE_PREFIX = BlastPlugin.class.getSimpleName();

    private static final String PARAM_ALGORITHM = "BlastAlgorithm";
    private static final String PARAM_QUERY_SEQUENCE = "BlastQuerySequence";

    private static final String COLUMN_ID = "full_id";
    private static final String COLUMN_EVALUE_MANT = "evalue_mant";
    private static final String COLUMN_EVALUE_EXP = "evalue_exp";
    private static final String COLUMN_SCORE = "score";

    private static final String FIELD_APP_PATH = "AppPath";
    private static final String FIELD_DATABASE_PATH = "DatabasePath";
    private static final String FIELD_TEMP_PATH = "TempPath";
    private static final String FIELD_TIMEOUT = "Timeout";

    private static final String URL_SEQUENCE = "showRecord.do?name=SequenceRecordClasses.SequenceRecordClass&source_id=";
    private static final String URL_GROUP = "showRecord.do?name=GroupRecordClasses.GroupRecordClass&group_name=";
    private static final String FLAG_NO_GROUP = "no_group";

    private static final DecimalFormat formatter = new DecimalFormat("0.###E0");
 
    private String appPath;
    private String databasePath;
    private File tempDir;
    private long timeout;

    /**
     * @param propertyFile
     */
    public BlastPlugin() {
        super(FILE_PROPERTY);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wsf.plugin.Plugin#getRequiredParameterNames()
     */
    public String[] getRequiredParameterNames() {
        return new String[] { PARAM_ALGORITHM, PARAM_QUERY_SEQUENCE };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wsf.plugin.Plugin#getColumns()
     */
    public String[] getColumns() {
        return new String[] { COLUMN_ID, COLUMN_EVALUE_MANT, COLUMN_EVALUE_EXP, COLUMN_SCORE };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wsf.plugin.Plugin#validateParameters(org.gusdb.wsf.plugin.
     * WsfRequest)
     */
    public void validateParameters(WsfRequest request)
            throws WsfServiceException {}

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wsf.plugin.AbstractPlugin#defineContextKeys()
     */
    @Override
    protected String[] defineContextKeys() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wsf.plugin.AbstractPlugin#initialize(java.util.Map)
     */
    @Override
    public void initialize(Map<String, Object> context)
            throws WsfServiceException {
        super.initialize(context);

        appPath = getProperty(FIELD_APP_PATH);
        databasePath = getProperty(FIELD_DATABASE_PATH);
        String tempPath = getProperty(FIELD_TEMP_PATH);
        if (appPath == null || databasePath == null || tempPath == null)
            throw new WsfServiceException("The required fields in property "
                    + "file are missing: " + FIELD_APP_PATH + ", "
                    + FIELD_DATABASE_PATH + ", " + FIELD_TEMP_PATH);
        tempDir = new File(tempPath);

        String max = getProperty(FIELD_TIMEOUT);
        if (max == null) timeout = 60; // by default, set timeout as 60 seconds
        else timeout = Integer.valueOf(max);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wsf.plugin.Plugin#execute(org.gusdb.wsf.plugin.WsfRequest)
     */
    public WsfResponse execute(WsfRequest request) throws WsfServiceException {
        logger.info("Invoking " + getClass().getSimpleName() + "...");

        // create temporary files for input sequence and output report
        try {
            // get command string
            Map<String, String> params = request.getParams();
            File outFile = File.createTempFile(FILE_PREFIX, ".out", tempDir);
            String[] command = prepareParameters(params, outFile);
            logger.info("Command prepared: " + Formatter.printArray(command));

            // invoke the command
            StringBuffer output = new StringBuffer();
            int signal = invokeCommand(command, output, timeout);

            logger.debug("BLAST output: \n------------------\n"
                    + output.toString() + "\n-----------------\n");

            // if the invocation succeeds, prepare the result; otherwise,
            // prepare results for failure scenario
            logger.info("\nPreparing the result");
            StringBuilder message = new StringBuilder();
            String[] orderedColumns = request.getOrderedColumns();
            String[][] result = prepareResult(orderedColumns, outFile, message);
            logger.info("\nResult prepared");

            message.append(newline).append(output);

            WsfResponse wsfResult = new WsfResponse();
            wsfResult.setMessage(message.toString());
            wsfResult.setSignal(signal);
            wsfResult.setResult(result);
            return wsfResult;
        }
        catch (IOException ex) {
            logger.error(ex);
            throw new WsfServiceException(ex);
        }
        finally {
            cleanup();
        }
    }

    private String[] prepareParameters(Map<String, String> params, File outFile)
            throws IOException, WsfServiceException {
        // get sequence and save it into the sequence file
        String sequence = params.get(PARAM_QUERY_SEQUENCE);
        File seqFile = File.createTempFile(FILE_PREFIX + "_", ".in", tempDir);
        PrintWriter out = new PrintWriter(new FileWriter(seqFile));
        if (!sequence.startsWith(">")) out.println(">MySeq1");
        out.println(sequence);
        out.flush();
        out.close();

        // now prepare the commandline
        List<String> commands = new ArrayList<String>();
        commands.add(appPath + "/blastall");
        commands.add("-p");
        commands.add(params.get(PARAM_ALGORITHM));
        commands.add("-d");
        commands.add(databasePath);
        commands.add("-i");
        commands.add(seqFile.getAbsolutePath());
        commands.add("-o");
        commands.add(outFile.getAbsolutePath());

        for (String param : params.keySet()) {
            if (param.equals(PARAM_ALGORITHM)
                    || param.equals(PARAM_QUERY_SEQUENCE)) continue;
            commands.add(param);
            commands.add(params.get(param));
        }
        return commands.toArray(new String[0]);
    }

    private String[][] prepareResult(String[] orderedColumns, File outFile,
            StringBuilder message) throws IOException, WsfServiceException {
        // create a map of <column/position>
        Map<String, Integer> columnIndexes = new HashMap<String, Integer>(
                orderedColumns.length);
        for (int i = 0; i < orderedColumns.length; i++) {
            columnIndexes.put(orderedColumns[i], i);
        }

        // read and parse the output
        Map<String, String[]> sequences = new LinkedHashMap<String, String[]>();
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(outFile));
        boolean inSummary = false;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0) {
                message.append(newline);
            } else if (line.startsWith("Sequences producing significant alignments")) {
                inSummary = true;
                message.append(line).append(newline);
            } else if (line.startsWith(">")) {
                // find the defline of detail section
                inSummary = false;
                StringBuilder buffer = new StringBuilder();
                processDefline(line, buffer);
                message.append(buffer).append(newline);
            } else if (inSummary) { // find a line in the table summary
                StringBuilder buffer = new StringBuilder();
                String sourceId = processDefline(line, buffer);
                message.append(buffer).append(newline);

                // parse the columns and put them into the array
                String[] parts = line.split("\\s+");
                String[] columns = new String[orderedColumns.length];
                columns[columnIndexes.get(COLUMN_ID)] = sourceId;
                String score = parts[parts.length - 2];
                columns[columnIndexes.get(COLUMN_SCORE)] = score;
                String[] evalue = parseEValue(parts[parts.length - 1]);
                columns[columnIndexes.get(COLUMN_EVALUE_MANT)] = evalue[0];
                columns[columnIndexes.get(COLUMN_EVALUE_EXP)] = evalue[1];
                sequences.put(sourceId, columns);
            } else {
                message.append(line).append(newline);
            }
        }

        String[][] results = new String[sequences.size()][orderedColumns.length];
        int i = 0;
        for (String[] columns : sequences.values()) {
            System.arraycopy(columns, 0, results[i], 0, columns.length);
            i++;
        }
        return results;
    }

    private String[] parseEValue(String evalue) {
        evalue = evalue.toUpperCase();
        if (evalue.indexOf("E") < 0) {
            evalue = formatter.format(Double.valueOf(evalue));
        }
        String[] parts = evalue.split("E");
        if (parts[0].length() == 0) parts[0] = "1";
        return parts;
    }

    /**
     * Process the defline, and add linkes to the source_id and group_id, and
     * return the source_id found in the defline.
     * 
     * @param defline
     * @param buffer
     * @return
     * @throws WsfServiceException
     * @throws UnsupportedEncodingException
     */
    private String processDefline(String defline, StringBuilder buffer)
            throws WsfServiceException, UnsupportedEncodingException {
        String[] parts = defline.split("\\|", 4);
        if (parts.length != 4)
            throw new WsfServiceException("Invalid defline syntax: " + defline);

        String sourceId = parts[0].trim() + "|" + parts[1].trim();
        String groupId = parts[2].trim();

        if (sourceId.startsWith(">")) {
            sourceId = sourceId.substring(1);
            buffer.append(">");
        }
        buffer.append("<a href=\"").append(URL_SEQUENCE);
        buffer.append(URLEncoder.encode(sourceId, "UTF-8"));
        buffer.append("\">").append(sourceId).append("</a> | ");
        if (groupId.equals(FLAG_NO_GROUP)) buffer.append(groupId);
        else {
            buffer.append("<a href=\"").append(URL_GROUP);
            buffer.append(URLEncoder.encode(groupId, "UTF-8"));
            buffer.append("\">").append(groupId).append("</a> | ");
        }
        buffer.append(parts[3].trim());
        return sourceId;
    }

    private void cleanup() {
        long todayLong = new Date().getTime();
        // remove files older than a week (500000000)
        for (File tempFile : tempDir.listFiles()) {
            if (tempFile.isFile() && tempFile.canWrite()
                    && (todayLong - (tempFile.lastModified())) > 500000000) {
                logger.info("Temp file to be deleted: "
                        + tempFile.getAbsolutePath() + "\n");
                tempFile.delete();
            }
        }
    }
}
