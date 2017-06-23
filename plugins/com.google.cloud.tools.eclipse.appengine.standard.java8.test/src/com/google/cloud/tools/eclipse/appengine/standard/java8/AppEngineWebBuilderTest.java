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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.Collections;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineWebBuilderTest {

  @Rule
  public TestProjectCreator testProject = new TestProjectCreator();

  /** Project without App Engine Standard facet should never have builder. */
  @Test
  public void testNoBuilder() throws CoreException {
    assertProjectMissingBuilder();
  }

  /** Project adding App Engine Standard facet should have builder. */
  @Test
  public void testAddedBuilder() throws CoreException {
    testProject.withFacetVersions(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25).getFacetedProject();

    assertProjectHasBuilder();
  }

  /** Project adding App Engine Standard facet should have builder. */
  @Test
  public void testRemovedBuilder() throws CoreException {
    testProject.withFacetVersions(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25).getFacetedProject();
    assertProjectHasBuilder();

    Action uninstallAction =
        new Action(Action.Type.UNINSTALL,
            AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8, null);
    testProject.getFacetedProject().modify(Collections.singleton(uninstallAction), null);
    assertProjectMissingBuilder();
  }

  /**
   * Project adding App Engine Standard facet should have builder, and should upgrade and downgrade
   * the Java facet appropriately.
   */
  @Test
  public void testAddingJava8Runtime_javaFacet() throws CoreException {
    testProject
        .withFacetVersions(AppEngineStandardFacet.JRE7, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25)
        .getFacetedProject();
    assertProjectHasBuilder();

    IFile appEngineWebDescriptor = WebProjectUtil.findInWebInf(testProject.getProject(),
        new Path("appengine-web.xml"));
    assertTrue("should have appengine-web.xml",
        appEngineWebDescriptor != null && appEngineWebDescriptor.exists());

    assertTrue(testProject.getFacetedProject().hasProjectFacet(JavaFacet.VERSION_1_7));
    assertTrue(testProject.getFacetedProject().hasProjectFacet(WebFacetUtils.WEB_25));

    AppEngineDescriptorTransform.addJava8Runtime(appEngineWebDescriptor);
    ProjectUtils.waitForProjects(testProject.getProject());

    assertEquals(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        testProject.getFacetedProject().getProjectFacetVersion(AppEngineStandardFacet.FACET));
    assertEquals("adding <runtime>java8</runtime> should change java to 1.8", JavaFacet.VERSION_1_8,
        testProject.getFacetedProject().getProjectFacetVersion(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_25,
        testProject.getFacetedProject().getProjectFacetVersion(WebFacetUtils.WEB_FACET));

    AppEngineDescriptorTransform.removeJava8Runtime(appEngineWebDescriptor);
    ProjectUtils.waitForProjects(testProject.getProject());
    assertEquals(AppEngineStandardFacet.JRE7,
        testProject.getFacetedProject().getProjectFacetVersion(AppEngineStandardFacet.FACET));
    assertEquals("removing <runtime>java8</runtime> should change java to 1.7",
        JavaFacet.VERSION_1_7,
        testProject.getFacetedProject().getProjectFacetVersion(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_25,
        testProject.getFacetedProject().getProjectFacetVersion(WebFacetUtils.WEB_FACET));
  }

  /**
   * Project adding App Engine Standard facet should have builder, and should downgrade the Dynamic
   * Web Project facet appropriately.
   */
  @Test
  public void testRemovingJava8Runtime() throws CoreException {
    testProject.withFacetVersions(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25).getFacetedProject();
    assertProjectHasBuilder();

    IFile appEngineWebDescriptor =
        WebProjectUtil.findInWebInf(testProject.getProject(), new Path("appengine-web.xml"));
    assertTrue("should have appengine-web.xml",
        appEngineWebDescriptor != null && appEngineWebDescriptor.exists());

    AppEngineDescriptorTransform.removeJava8Runtime(appEngineWebDescriptor);
    ProjectUtils.waitForProjects(testProject.getProject());
    assertEquals(AppEngineStandardFacet.JRE7,
        testProject.getFacetedProject().getProjectFacetVersion(AppEngineStandardFacet.FACET));
    assertEquals(JavaFacet.VERSION_1_7,
        testProject.getFacetedProject().getProjectFacetVersion(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_25,
        testProject.getFacetedProject().getProjectFacetVersion(WebFacetUtils.WEB_FACET));
  }

  /**
   * Project adding App Engine Standard facet should have builder, and should downgrade the Dynamic
   * Web Project facet appropriately.
   */
  @Test
  public void testRemovingJava8Runtime_javaFacet() throws CoreException {
    testProject.withFacetVersions(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25).getFacetedProject();
    assertProjectHasBuilder();

    IFile appEngineWebDescriptor =
        WebProjectUtil.findInWebInf(testProject.getProject(), new Path("appengine-web.xml"));
    assertTrue("should have appengine-web.xml",
        appEngineWebDescriptor != null && appEngineWebDescriptor.exists());

    AppEngineDescriptorTransform.removeJava8Runtime(appEngineWebDescriptor);
    ProjectUtils.waitForProjects(testProject.getProject());
    assertEquals(AppEngineStandardFacet.JRE7,
        testProject.getFacetedProject().getProjectFacetVersion(AppEngineStandardFacet.FACET));
    assertEquals("removing <runtime>java8</runtime> should change java to 1.7",
        JavaFacet.VERSION_1_7,
        testProject.getFacetedProject().getProjectFacetVersion(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_25,
        testProject.getFacetedProject().getProjectFacetVersion(WebFacetUtils.WEB_FACET));
  }

  /**
   * Project adding App Engine Standard facet should have builder, and should downgrade the Dynamic
   * Web Project facet appropriately.
   */
  @Ignore("Removing <runtime>java8</runtime> cannot currently downgrade DWP from 3.1 to 2.5")
  @Test
  public void testRemovingJava8Runtime_webFacet() throws CoreException {
    testProject.withFacetVersions(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        JavaFacet.VERSION_1_7, WebFacetUtils.WEB_31).getFacetedProject();
    assertProjectHasBuilder();

    IFile appEngineWebDescriptor =
        WebProjectUtil.findInWebInf(testProject.getProject(), new Path("appengine-web.xml"));
    assertTrue("should have appengine-web.xml",
        appEngineWebDescriptor != null && appEngineWebDescriptor.exists());

    AppEngineDescriptorTransform.removeJava8Runtime(appEngineWebDescriptor);
    ProjectUtils.waitForProjects(testProject.getProject());
    assertEquals(AppEngineStandardFacet.JRE7,
        testProject.getFacetedProject().getProjectFacetVersion(AppEngineStandardFacet.FACET));
    assertEquals("removing <runtime>java8</runtime> should change java to 1.7",
        JavaFacet.VERSION_1_7,
        testProject.getFacetedProject().getProjectFacetVersion(JavaFacet.FACET));
    assertEquals("removing <runtime>java8</runtime> should change jst.web to 2.5",
        WebFacetUtils.WEB_25,
        testProject.getFacetedProject().getProjectFacetVersion(WebFacetUtils.WEB_FACET));
  }

  private void assertProjectMissingBuilder() throws CoreException {
    ProjectUtils.waitForProjects(testProject.getProject());
    IProjectDescription description = testProject.getProject().getDescription();
    for (ICommand buildSpec : description.getBuildSpec()) {
      assertNotEquals(AppEngineWebBuilder.BUILDER_ID, buildSpec.getBuilderName());
    }
  }

  private void assertProjectHasBuilder() throws CoreException {
    ProjectUtils.waitForProjects(testProject.getProject());
    IProjectDescription description = testProject.getProject().getDescription();
    for (ICommand buildSpec : description.getBuildSpec()) {
      if (AppEngineWebBuilder.BUILDER_ID.equals(buildSpec.getBuilderName())) {
        return;
      }
    }
    fail("missing AppEngineWebBuilder");
  }
}
