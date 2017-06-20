/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.standard.java8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.cloud.tools.eclipse.appengine.facets.convert.AppEngineStandardProjectConvertJob;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test that our <em>Convert to App Engine Standard Project</em> doesn't downgrade Java or jst.web
 * facet versions.
 */
public class ConversionTests {
  @Rule
  public TestProjectCreator projectCreator = new TestProjectCreator();

  @Test
  public void testConvertBare_Java7_Web25()
      throws CoreException, IOException, InterruptedException, SAXException {
    IFacetedProject project = projectCreator
        .withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25).getFacetedProject();
    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();

    // ensure facet versions haven't been downgraded
    assertEquals(JavaFacet.VERSION_1_7, project.getProjectFacetVersion(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_25, project.getProjectFacetVersion(WebFacetUtils.WEB_FACET));
    assertEquals(AppEngineStandardFacet.JRE7,
        project.getProjectFacetVersion(AppEngineStandardFacet.FACET));

    // ensure appengine-web.xml has <runtime>java8</runtime>
    IFile appengineWebXml =
        WebProjectUtil.findInWebInf(project.getProject(), new Path("appengine-web.xml"));
    assertNotNull("appengine-web.xml is missing", appengineWebXml);
    assertTrue("appengine-web.xml does not exist", appengineWebXml.exists());
    try (InputStream input = appengineWebXml.getContents()) {
      AppEngineDescriptor descriptor = AppEngineDescriptor.parse(input);
      assertNull(descriptor.getRuntime());
      assertFalse(descriptor.isJava8());
    }
  }

  @Test
  public void testConvertAES7()
      throws CoreException, IOException, InterruptedException, SAXException {
    IFacetedProject project = projectCreator.getFacetedProject();
    IFolder webInf = project.getProject().getFolder("WebContent/WEB-INF");
    ResourceUtils.createFolders(webInf, null);
    IFile appEngineWeb = webInf.getFile("appengine-web.xml");
    appEngineWeb.create(
        new ByteArrayInputStream(
            "<appengine-web-app></appengine-web-app>"
                .getBytes(StandardCharsets.UTF_8)),
        true, null);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();

    assertEquals(JavaFacet.VERSION_1_7, project.getProjectFacetVersion(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_25, project.getProjectFacetVersion(WebFacetUtils.WEB_FACET));
    assertEquals(AppEngineStandardFacet.JRE7,
        project.getProjectFacetVersion(AppEngineStandardFacet.FACET));
  }

  @Test
  public void testConvertAES8()
      throws CoreException, IOException, InterruptedException, SAXException {
    IFacetedProject project = projectCreator.getFacetedProject();
    IFolder webInf = project.getProject().getFolder("WebContent/WEB-INF");
    ResourceUtils.createFolders(webInf, null);
    IFile appEngineWeb = webInf.getFile("appengine-web.xml");
    appEngineWeb.create(
        new ByteArrayInputStream("<appengine-web-app><runtime>java8</runtime></appengine-web-app>"
            .getBytes(StandardCharsets.UTF_8)),
        true, null);

    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();

    assertEquals(JavaFacet.VERSION_1_8, project.getProjectFacetVersion(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_31, project.getProjectFacetVersion(WebFacetUtils.WEB_FACET));
    assertEquals(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        project.getProjectFacetVersion(AppEngineStandardFacet.FACET));
  }

  @Test
  public void testConvertBare_Java8_Web31()
      throws CoreException, IOException, InterruptedException, SAXException {
    IFacetedProject project = projectCreator
        .withFacetVersions(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31).getFacetedProject();
    Job conversionJob = new AppEngineStandardProjectConvertJob(project);
    conversionJob.schedule();
    conversionJob.join();

    // ensure facet versions haven't been downgraded
    assertEquals(JavaFacet.VERSION_1_8, project.getProjectFacetVersion(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_31, project.getProjectFacetVersion(WebFacetUtils.WEB_FACET));
    assertEquals(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        project.getProjectFacetVersion(AppEngineStandardFacet.FACET));

    // ensure appengine-web.xml has <runtime>java8</runtime>
    IFile appengineWebXml =
        WebProjectUtil.findInWebInf(project.getProject(), new Path("appengine-web.xml"));
    assertNotNull("appengine-web.xml is missing", appengineWebXml);
    assertTrue("appengine-web.xml does not exist", appengineWebXml.exists());
    try (InputStream input = appengineWebXml.getContents()) {
      assertTrue(AppEngineDescriptor.parse(input).isJava8());
    }
  }
}
