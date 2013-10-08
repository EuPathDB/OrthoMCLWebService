package org.orthomcl.wsfplugin;

import java.sql.SQLException;

import org.eupathdb.websvccommon.wsfplugin.blast.NcbiBlastResultFormatter;


public class OrthoMCLBlastResultFormatter extends NcbiBlastResultFormatter {

  @Override
  protected String getProject(String organism) throws SQLException {
    return "OrthoMCL";
  }

}
