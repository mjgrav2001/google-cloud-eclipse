/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.ProjectFacetDetector;
import org.xml.sax.SAXException;

public class AppEngineStandardJre7ProjectFacetDetector extends ProjectFacetDetector {
  private static final Logger logger =
      Logger.getLogger(AppEngineStandardJre7ProjectFacetDetector.class.getName());

  @Override
  public void detect(IFacetedProjectWorkingCopy workingCopy, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 10);
    IFile appEngineWebXml =
        WebProjectUtil.findInWebInf(workingCopy.getProject(), new Path("appengine-web.xml"));
    if (appEngineWebXml == null || !appEngineWebXml.exists()) {
      logger.fine("skipping " + workingCopy.getProjectName() + ": no appengine-web.xml found");
      return;
    }
    try (InputStream content = appEngineWebXml.getContents()) {
      AppEngineDescriptor descriptor = AppEngineDescriptor.parse(content);
      if (descriptor.getRuntime() == null || !"java7".equals(descriptor.getRuntime())) {
        logger.fine("skipping " + workingCopy.getProjectName() + ": appengine-web.xml is not java7");
        return;
      }
      logger.fine(workingCopy.getProjectName() + ": appengine-web.xml has java7 runtime");
      workingCopy.addProjectFacet(AppEngineStandardFacet.JRE7);
      if (!workingCopy.hasProjectFacet(JavaFacet.FACET)) {
        logger.fine(workingCopy.getProjectName() + ": setting Java 7 facet");
        workingCopy.addProjectFacet(JavaFacet.VERSION_1_7);
      }
      if (!workingCopy.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
        logger.fine(workingCopy.getProjectName() + ": setting Dynamic Web 2.5 facet");
        workingCopy.addProjectFacet(WebFacetUtils.WEB_25);
      }
    } catch (SAXException | IOException ex) {
      throw new CoreException(StatusUtil.error(this, "Unable to retrieve appengine-web.xml", ex));
    }
  }

}
