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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test changing the App Engine Standard Facet results in appropriate {@code <runtime>} changes in
 * the {@code appengine-web.xml}.
 */
public class AppEngineStandardFacetVersionChangeTests {
  @Rule
  public TestProjectCreator jre7Project = new TestProjectCreator()
      .withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, AppEngineStandardFacet.JRE7);

  @Rule
  public TestProjectCreator jre8Project = new TestProjectCreator()
      .withFacetVersions(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25,
          AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);

  /** Should be able to change a App Engine Standard JRE7 project to JRE8 with no other changes. */
  @Test
  public void testChange_AESJ7_AESJ8() throws CoreException, IOException, SAXException {
    IFacetedProject project = jre7Project.getFacetedProject();
    assertDescriptorRuntimeIsJre7(project);

    Set<Action> actions = new HashSet<>();
    actions.add(new Action(Action.Type.VERSION_CHANGE,
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8, null));
    project.modify(actions, null);

    assertDescriptorRuntimeIsJre8(project);
  }

  /** Fail changing AppEngine Standard JRE8 to JRE7 with no other changes. */
  @Test
  public void testChange_AESJ8_AESJ7() throws CoreException, IOException, SAXException {
    IFacetedProject project = jre8Project.getFacetedProject();
    assertDescriptorRuntimeIsJre8(project);

    try {
      Set<Action> actions = new HashSet<>();
      actions.add(new Action(Action.Type.VERSION_CHANGE, AppEngineStandardFacet.JRE7, null));
      project.modify(actions, null);
      fail("should fail as still has Java 8 facet");
    } catch (CoreException ex) {
      // expected
      assertDescriptorRuntimeIsJre8(project);
    }
  }

  /** Changing AppEngine Standard JRE8 to JRE7+Java7 facet should succeed. */
  @Test
  public void testChange_AESJ8_AESJ7andJava7() throws CoreException, IOException, SAXException {
    IFacetedProject project = jre8Project.getFacetedProject();
    assertDescriptorRuntimeIsJre8(project);

    Set<Action> actions = new HashSet<>();
    actions.add(new Action(Action.Type.VERSION_CHANGE, AppEngineStandardFacet.JRE7, null));
    actions.add(new Action(Action.Type.VERSION_CHANGE, JavaFacet.VERSION_1_7, null));
    project.modify(actions, null);

    assertDescriptorRuntimeIsJre7(project);
  }

  private static AppEngineDescriptor parseDescriptor(IFacetedProject project)
      throws IOException, CoreException, SAXException {
    IFile descriptorFile =
        WebProjectUtil.findInWebInf(project.getProject(), new Path("appengine-web.xml"));
    assertNotNull("appengine-web.xml not found", descriptorFile);
    assertTrue("appengine-web.xml does not exist", descriptorFile.exists());

    try (InputStream is = descriptorFile.getContents()) {
      return AppEngineDescriptor.parse(is);
    }
  }

  private static void assertDescriptorRuntimeIsJre7(IFacetedProject project)
      throws IOException, CoreException, SAXException {
    AppEngineDescriptor descriptor = parseDescriptor(project);
    assertNotNull(descriptor);
    assertNull("should have no <runtime> element", descriptor.getRuntime());
  }

  private static void assertDescriptorRuntimeIsJre8(IFacetedProject project)
      throws IOException, CoreException, SAXException {
    AppEngineDescriptor descriptor = parseDescriptor(project);
    assertNotNull(descriptor);
    assertTrue("missing <runtime>java8</runtime>", descriptor.isJava8());
  }

}
