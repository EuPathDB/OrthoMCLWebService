package org.orthomcl.wsfplugin;

import static org.gusdb.fgputil.FormatUtil.urlEncodeUtf8;

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
      String sourceId, String defline) throws EuPathServiceException {
    return "showRecord.do?name=" + recordClass + "&project_id="
        + urlEncodeUtf8(projectId) + "&full_id="
        + urlEncodeUtf8(sourceId);
  }
}
