
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
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetDetector;
import org.xml.sax.SAXException;

public class Java8ProjectFacetDetector extends ProjectFacetDetector {
  private static final Logger logger = Logger.getLogger(Java8ProjectFacetDetector.class.getName());

  @Override
  public void detect(IFacetedProjectWorkingCopy fpjwc, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 10);
    IFile appEngineWebXml =
        WebProjectUtil.findInWebInf(fpjwc.getProject(), new Path("appengine-web.xml"));
    if (!appEngineWebXml.exists()) {
      logger.fine("skipping " + fpjwc.getProjectName() + ": no appengine-web.xml found");
      return;
    }
    try (InputStream content = appEngineWebXml.getContents()) {
      AppEngineDescriptor descriptor = AppEngineDescriptor.parse(content);
      IProjectFacetVersion javaFacetVersion = fpjwc.getProjectFacetVersion(JavaFacet.FACET);
      // IProjectFacetVersion dynamicWebFacetVersion =
      // fpjwc.getProjectFacetVersion(WebFacetUtils.WEB_FACET);
      // Action javaInstallAction = fpjwc.getProjectFacetAction(JavaFacet.FACET);
      if (descriptor.isJava8()) {
        logger
            .fine(fpjwc.getProjectName() + ": appengine-web.xml is java8 so setting Java 8 facet");
        if (!JavaFacet.VERSION_1_8.equals(javaFacetVersion)) {
          fpjwc.addProjectFacet(JavaFacet.VERSION_1_8);
        }
        fpjwc.addProjectFacet(AppEngineStandardFacet.JAVA8);
      } else if (!JavaFacet.VERSION_1_7.equals(javaFacetVersion)) {
        logger.fine(
            fpjwc.getProjectName() + ": appengine-web.xml is not java8 so setting Java 7 facet");
        fpjwc.addProjectFacet(JavaFacet.VERSION_1_7);
        fpjwc.addProjectFacet(AppEngineStandardFacet.JAVA7);
      }
    } catch (SAXException | IOException ex) {
      throw new CoreException(StatusUtil.error(this, "Unable to retrieve appengine-web.xml", ex));
    }
    throw new CoreException(StatusUtil.error(this, "Requires Java 8 support"));

  }

}
