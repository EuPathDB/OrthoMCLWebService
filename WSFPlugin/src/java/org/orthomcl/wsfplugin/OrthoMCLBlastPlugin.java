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
    
    // replace data_type with database, since data_type is not needed here.
    boolean replaced = false;
    for (int i = 0; i < partParams.length;i++) {
      if (partParams[i].equals(AbstractBlastPlugin.PARAM_DATA_TYPE)) {
        partParams[i] = PARAM_DATABASE;
        replaced = true;
        break;
      }
    }
    if (replaced) return partParams;
    
    // data_type not found, append the database param to the list
    String[] params = new String[partParams.length + 1];
    System.arraycopy(partParams, 0, params, 0, partParams.length);
    params[params.length - 1] = PARAM_DATABASE;
    return params;
  }
}
