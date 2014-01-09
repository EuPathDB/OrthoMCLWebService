package org.orthomcl.wsfplugin;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;

import org.eupathdb.websvccommon.wsfplugin.EuPathServiceException;
import org.eupathdb.websvccommon.wsfplugin.blast.NcbiBlastResultFormatter;


public class OrthoMCLBlastResultFormatter extends NcbiBlastResultFormatter {

  @Override
  protected String getProject(String organism) throws SQLException {
    return "OrthoMCL";
  }

  @Override
  protected String getIdUrl(String recordClass, String projectId,
      String sourceId) throws EuPathServiceException {
    try {
    String url = "showRecord.do?name=" + recordClass + "&project_id="
        + URLEncoder.encode(projectId, "UTF-8") + "&full_id="
        + URLEncoder.encode(sourceId, "UTF-8");
    return url;
    } catch (UnsupportedEncodingException ex) {
      throw new EuPathServiceException(ex);
    }
  }

}
