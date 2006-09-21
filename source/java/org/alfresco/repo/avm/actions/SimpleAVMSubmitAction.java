/*
 * Copyright (C) 2006 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */

package org.alfresco.repo.avm.actions;

import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avmsync.AVMDifference;
import org.alfresco.service.cmr.avmsync.AVMSyncService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

/**
 * This action submits all the newer changes in the passed in NodeRef
 * to its corresponding staging area. It ignores conflicts and older nodes.
 * @author britt
 */
public class SimpleAVMSubmitAction extends ActionExecuterAbstractBase
{
    private static Logger fgLogger = Logger.getLogger(SimpleAVMSubmitAction.class);
    
    public static String NAME = "simple-avm-submit";
    
    /**
     * The AVMService instance.
     */
    private AVMService fAVMService;
    
    /**
     * The AVMSyncService instance.
     */
    private AVMSyncService fAVMSyncService;
    
    /**
     * Default constructor.
     */
    public SimpleAVMSubmitAction()
    {
        super();
    }
    
    /**
     * Set the AVMService.
     * @param avmService The instance.
     */
    public void setAvmService(AVMService avmService)
    {
        fAVMService = avmService;
    }
    
    /**
     * Set the AVMSyncService.
     * @param avmSyncService The instance.
     */
    public void setAvmSyncService(AVMSyncService avmSyncService)
    {
        fAVMSyncService = avmSyncService;
    }
    
    /**
     * Perform the action. The NodeRef must be an AVM NodeRef.
     * @param action Don't actually need anything from this here.
     * @param actionedUponNodeRef The AVM NodeRef.
     */
    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
    {
        // Crack the NodeRef.
        Object [] avmVersionPath = AVMNodeConverter.ToAVMVersionPath(actionedUponNodeRef);
        int version = (Integer)avmVersionPath[0];
        String path = (String)avmVersionPath[1];
        // Get store name and path parts.
        String [] storePath = path.split(":");
        // Get the .website.name property.
        PropertyValue wsProp = 
            fAVMService.getStoreProperty(storePath[0], 
                    QName.createQName(null, ".website.name"));
        if (wsProp == null)
        {
            fgLogger.warn(".website.name property not found.");
            return;
        }
        // And the actual web-site name.
        String websiteName = wsProp.getStringValue();
        // Construct the submit destination path.
        String avmDest = websiteName + "-staging:" + storePath[1];
        // Get the difference between source and destination.
        List<AVMDifference> diffs = 
            fAVMSyncService.compare(version, path, -1, avmDest);
        // Do the update.
        fAVMSyncService.update(diffs, true, true, false, false);
        // Cleanup by flattening the source relative to the destination.
        fAVMSyncService.flatten(path, avmDest);
    }

    /**
     * This action takes no parameters.
     * @param paramList The List to add nothing to.
     */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList)
    {
        // No parameters for this action.
    }
}
