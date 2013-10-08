/**
 * 
 */
package org.orthomcl.wsfplugin;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wsf.plugin.AbstractPlugin;
import org.gusdb.wsf.plugin.PluginRequest;
import org.gusdb.wsf.plugin.PluginResponse;
import org.gusdb.wsf.plugin.WsfPluginException;

/**
 * @author Jerric, modified by Cristina 2010 to add DNA motif
 * @created Jan 31, 2006
 */

// geneID could be an ORF or a genomic sequence deending on who uses the plugin
public class MotifPlugin extends AbstractPlugin {

  private static class Match {

    public String sourceId;
    public String locations;
    public int matchCount = 0;
    public String sequence;

    @Override
    public int hashCode() {
      return sourceId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj != null && obj instanceof Match) {
        Match match = (Match) obj;
        return sourceId.equals(match.sourceId);
      } else return false;
    }
  }

  private static final String CTX_WDK_MODEL = "wdkModel";

  private static final String PARAM_ORGANISM = "organism";
  private static final String PARAM_EXPRESSION = "motif_expression";

  // column definitions for returnd results
  private static final String COLUMN_SOURCE_ID = "full_id";
  private static final String COLUMN_LOCATIONS = "Locations";
  private static final String COLUMN_MATCH_COUNT = "MatchCount";
  private static final String COLUMN_SEQUENCE = "Sequence";

  private static final String CSS_MATCH = "motif-match";
  private static final int CONTEXT_LENGTH = 20;

  private static final Map<Character, String> SYMBOLS = new HashMap<Character, String>();
  static {
    SYMBOLS.put('0', "DE");
    SYMBOLS.put('1', "ST");
    SYMBOLS.put('2', "ILV");
    SYMBOLS.put('3', "FHWY");
    SYMBOLS.put('4', "KRH");
    SYMBOLS.put('5', "DEHKR");
    SYMBOLS.put('6', "AVILMFYW");
    SYMBOLS.put('7', "KRHDENQ");
    SYMBOLS.put('8', "CDEHKNQRST");
    SYMBOLS.put('9', "ACDGNPSTV");
    SYMBOLS.put('B', "AGS");
    SYMBOLS.put('Z', "ACDEGHKNQRST");
  }

  private WdkModel wdkModel;

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wsf.WsfPlugin#getRequiredParameters()
   */
  @Override
  public String[] getRequiredParameterNames() {
    return new String[] { PARAM_EXPRESSION, PARAM_ORGANISM };
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wsf.plugin.AbstractPlugin#defineContextKeys()
   */
  @Override
  protected String[] defineContextKeys() {
    return new String[] { CTX_WDK_MODEL };
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wsf.WsfPlugin#getColumns()
   */
  @Override
  public String[] getColumns() {
    return new String[] { COLUMN_SOURCE_ID, COLUMN_LOCATIONS,
        COLUMN_MATCH_COUNT, COLUMN_SEQUENCE };
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wsf.plugin.WsfPlugin#validateParameters(java.util.Map)
   */
  @Override
  public void validateParameters(PluginRequest request)
      throws WsfPluginException {}

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wsf.plugin.AbstractPlugin#initialize(java.util.Map)
   */
  @Override
  public void initialize(Map<String, Object> context)
      throws WsfPluginException {
    super.initialize(context);

    WdkModelBean wdkModelBean = (WdkModelBean) context.get(CTX_WDK_MODEL);
    wdkModel = wdkModelBean.getModel();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wsf.WsfPlugin#execute(java.util.Map, java.lang.String[])
   */
  @Override
  public void execute(PluginRequest request, PluginResponse response)
      throws WsfPluginException {
    logger.info("Invoking " + getClass().getSimpleName() + "...");

    Map<String, String> params = request.getParams();
    // create a column order map
    String[] orderedColumns = request.getOrderedColumns();
    Map<String, Integer> orders = new HashMap<String, Integer>();
    for (int i = 0; i < orderedColumns.length; i++)
      orders.put(orderedColumns[i], i);

    // get required parameters
    String organisms = params.get(PARAM_ORGANISM);
    String expression = params.get(PARAM_EXPRESSION);

    // translate the expression
    Pattern searchPattern = translateExpression(expression);

    // open the database and get a resultSet
    String sql = "SELECT eas.secondary_identifier AS source_id, eas.sequence "
        + " FROM dots.ExternalAaSequence eas, apidb.OrthomclTaxon ot "
        + " WHERE ot.three_letter_abbrev IN (" + organisms + ")"
        + "   AND ot.taxon_id = eas.taxon_id";
    DataSource dataSource = wdkModel.getAppDb().getDataSource();
    ResultSet resultSet = null;
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql, "motif-search", 500);
      while (resultSet.next()) {
        String sourceId = resultSet.getString("source_id");
        String sequence = resultSet.getString("sequence");

        findMatches(response, searchPattern, sourceId, sequence, orders);
      }
    } catch (Exception ex) {
      throw new WsfPluginException(ex);
    } finally {
      SqlUtils.closeResultSetAndStatement(resultSet);
    }
  }

  private Pattern translateExpression(String expression) {
    boolean inSquareBraces = false, inCurlyBraces = false;
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < expression.length(); i++) {
      char ch = Character.toUpperCase(expression.charAt(i));
      boolean skipChar = false;
      if (ch == '{') inCurlyBraces = true;
      else if (ch == '}') inCurlyBraces = false;
      else if (ch == '[') inSquareBraces = true;
      else if (ch == ']') inSquareBraces = false;
      else if (!inCurlyBraces && SYMBOLS.containsKey(ch)) {
        // the char is not in any curly braces, and is a known code;
        // replace the char with the actual string.
        String replace = SYMBOLS.get(ch);
        if (!inSquareBraces) replace = "[" + replace + "]";
        builder.append(replace);
        skipChar = true;
      }
      if (!skipChar) builder.append(ch);
    }
    logger.debug("translated expression: " + builder);

    int option = Pattern.CASE_INSENSITIVE;
    return Pattern.compile(builder.toString(), option);
  }

  private void findMatches(PluginResponse response, Pattern searchPattern,
      String sourceId, String sequence, Map<String, Integer> orders)
      throws WsfPluginException {
    Match match = new Match();
    match.sourceId = sourceId;
    StringBuffer sbLoc = new StringBuffer();
    StringBuffer sbSeq = new StringBuffer();
    int prev = 0;

    Matcher matcher = searchPattern.matcher(sequence);
    while (matcher.find()) {
      int start = matcher.start();
      String location = getLocation(0, start, matcher.end() - 1, false);

      // add locations
      if (sbLoc.length() != 0) sbLoc.append(", ");
      sbLoc.append('(' + location + ')');

      // obtain the context sequence
      if ((matcher.start() - prev) <= (CONTEXT_LENGTH * 2)) {
        // no need to trim
        sbSeq.append(sequence.substring(prev, start));
      } else { // need to trim some
        if (prev != 0)
          sbSeq.append(sequence.substring(prev, prev + CONTEXT_LENGTH));
        sbSeq.append("... ");
        sbSeq.append(sequence.substring(start - CONTEXT_LENGTH, start));
      }
      sbSeq.append("<font class=\"" + CSS_MATCH + "\">");
      sbSeq.append(sequence.substring(start, matcher.end()));
      sbSeq.append("</font>");
      prev = matcher.end();
      match.matchCount++;
    }
    if (match.matchCount == 0) return;

    // grab the last context
    if ((prev + CONTEXT_LENGTH) < sequence.length()) {
      sbSeq.append(sequence.substring(prev, prev + CONTEXT_LENGTH));
      sbSeq.append("... ");
    } else {
      sbSeq.append(sequence.substring(prev));
    }
    match.locations = sbLoc.toString();
    match.sequence = sbSeq.toString();
    addMatch(response, match, orders);
  }

  private void addMatch(PluginResponse response, Match match,
      Map<String, Integer> orders) throws WsfPluginException {
    String[] row = new String[orders.size()];

    row[orders.get(COLUMN_SOURCE_ID)] = match.sourceId;
    row[orders.get(COLUMN_LOCATIONS)] = match.locations;
    row[orders.get(COLUMN_MATCH_COUNT)] = Integer.toString(match.matchCount);
    row[orders.get(COLUMN_SEQUENCE)] = match.sequence;
    // logger.debug("result " + resultToString(result) + "\n");
    response.addRow(row);
  }

  private String getLocation(int length, int start, int stop, boolean reversed) {
    if (reversed) {
      int newStart = length - stop;
      stop = length - start;
      start = newStart;
    }
    String location = Integer.toString(start);
    if (start != stop) location += "-" + stop;
    return location;
  }
}
