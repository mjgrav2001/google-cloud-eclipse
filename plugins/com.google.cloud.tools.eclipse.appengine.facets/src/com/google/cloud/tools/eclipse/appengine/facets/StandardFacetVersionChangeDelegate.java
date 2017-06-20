
package com.google.cloud.tools.eclipse.appengine.facets;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

public class StandardFacetVersionChangeDelegate implements IDelegate {

  @Override
  public void execute(IProject project, IProjectFacetVersion fv, Object config,
      IProgressMonitor monitor) throws CoreException {
    /* required to allow changing between JRE7 <--> JRE8 */
  }

}
