package org.orthomcl.wsfplugin;

import org.eupathdb.websvccommon.wsfplugin.blast.AbstractBlastPlugin;
import org.gusdb.wsf.plugin.WsfPluginException;

public class OrthoMCLBlastPlugin extends AbstractBlastPlugin {

  public static final String PARAM_DATABASE = "BlastDatabase";

  public OrthoMCLBlastPlugin() throws WsfPluginException {
    super(new OrthoMCLBlastCommandFormatter(), new OrthoMCLBlastResultFormatter());
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
