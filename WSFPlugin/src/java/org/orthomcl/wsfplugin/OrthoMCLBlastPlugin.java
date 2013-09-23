package org.orthomcl.wsfplugin;

import org.eupathdb.websvccommon.wsfplugin.blast.AbstractBlastPlugin;
import org.eupathdb.websvccommon.wsfplugin.blast.BlastConfig;
import org.eupathdb.websvccommon.wsfplugin.blast.NcbiBlastResultFormatter;

public class OrthoMCLBlastPlugin extends AbstractBlastPlugin {
  
  public static final String PARAM_DATABASE = "BlastDatabase";

  public OrthoMCLBlastPlugin(BlastConfig config) {
    super(config, new OrthoMCLBlastCommandFormatter(),
        new NcbiBlastResultFormatter());
  }

  @Override
  public String[] getRequiredParameterNames() {
    String[] partParams = super.getRequiredParameterNames();
    String[] params = new String[partParams.length + 1];
    System.arraycopy(partParams, 0, params, 0, partParams.length);
    params[params.length - 1] = PARAM_DATABASE;
    return params;
  }
}
