package org.orthomcl.wsfplugin;

import java.util.Map;

import org.eupathdb.websvccommon.wsfplugin.blast.NcbiBlastCommandFormatter;

public class OrthoMCLBlastCommandFormatter extends NcbiBlastCommandFormatter {

  @Override
  public String getBlastDatabase(Map<String, String> params) {
    String database = params.get(OrthoMCLBlastPlugin.PARAM_DATABASE);
    if (database.startsWith("'"))
      database = database.substring(1);
    if (database.endsWith("'"))
      database = database.substring(0, database.length() - 1);
    return database;
  }

}
