
package com.google.cloud.tools.eclipse.appengine.standard.java8;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
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

public class AppEngineStandardJre8ProjectFacetDetector extends ProjectFacetDetector {
  private static final Logger logger = Logger.getLogger(AppEngineStandardJre8ProjectFacetDetector.class.getName());

  @Override
  public void detect(IFacetedProjectWorkingCopy fpjwc, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 10);
    IFile appEngineWebXml =
        WebProjectUtil.findInWebInf(fpjwc.getProject(), new Path("appengine-web.xml"));
    if (appEngineWebXml == null || !appEngineWebXml.exists()) {
      logger.fine("skipping " + fpjwc.getProjectName() + ": no appengine-web.xml found");
      return;
    }
    try (InputStream content = appEngineWebXml.getContents()) {
      AppEngineDescriptor descriptor = AppEngineDescriptor.parse(content);
      if (!descriptor.isJava8()) {
        logger.fine(fpjwc.getProjectName() + ": appengine-web.xml is not java8 so skipping");
      }
      logger.fine(fpjwc.getProjectName() + ": appengine-web.xml has runtime=java8");
      fpjwc.addProjectFacet(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);
      if (!fpjwc.hasProjectFacet(JavaFacet.FACET)) {
        logger.fine(fpjwc.getProjectName() + ": setting Java 8 facet");
        fpjwc.addProjectFacet(JavaFacet.VERSION_1_8);
      }
      if (!fpjwc.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
        logger.fine(fpjwc.getProjectName() + ": setting Dynamic Web 3.1 facet");
        fpjwc.addProjectFacet(WebFacetUtils.WEB_31);
      }
    } catch (SAXException | IOException ex) {
      throw new CoreException(StatusUtil.error(this, "Unable to retrieve appengine-web.xml", ex));
    }
  }
}
