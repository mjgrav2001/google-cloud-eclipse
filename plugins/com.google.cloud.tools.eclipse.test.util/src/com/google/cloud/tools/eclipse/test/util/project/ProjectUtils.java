/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.test.util.project;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.reflection.ReflectionUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.internal.jobs.InternalJob;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.common.project.facet.core.util.internal.ZipUtil;
import org.eclipse.wst.validation.internal.operations.ValidationBuilder;
import org.eclipse.wst.validation.internal.operations.ValidatorManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * A set of utility methods for dealing with projects.
 */
public class ProjectUtils {
  private static boolean DEBUG = false;

  /**
   * Import the Eclipse projects found within the bundle containing {@code clazz} at the
   * {@code relativeLocation}. Return the list of projects imported.
   *
   * @throws IOException if the zip cannot be accessed
   * @throws CoreException if a project cannot be imported
   */
  public static List<IProject> importProjects(Class<?> clazz, String relativeLocation,
      boolean checkBuildErrors, IProgressMonitor monitor)
      throws IOException, CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 100);

    // Resolve the zip from within this bundle
    Bundle bundle = FrameworkUtil.getBundle(clazz);
    URL bundleLocation = bundle.getResource(relativeLocation);
    assertNotNull(bundleLocation);
    URL zipLocation = FileLocator.toFileURL(bundleLocation);
    if (!zipLocation.getProtocol().equals("file")) {
      throw new IOException("could not resolve location to a file");
    }
    File zippedFile = new File(zipLocation.getPath());
    assertTrue(zippedFile.exists());
    progress.worked(5);

    IWorkspaceRoot root = getWorkspace().getRoot();
    // extract projects into our workspace using WTP internal utility class
    // assumes projects are contained in subdirectories within the zip
    ZipUtil.unzip(zippedFile, root.getLocation().toFile(), progress.newChild(10));

    List<IPath> projectFiles = new ArrayList<>();
    try (ZipFile zip = new ZipFile(zippedFile)) {
      for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
        ZipEntry entry = it.nextElement();
        if (entry.getName().endsWith("/.project")) {
          IPath projectFileLocation = root.getLocation().append(new Path(entry.getName()));
          projectFiles.add(projectFileLocation);
        }
      }
      progress.worked(5);
    }

    // import the projects
    progress.setWorkRemaining(10 * projectFiles.size() + 10);
    List<IProject> projects = new ArrayList<>(projectFiles.size());
    for (IPath projectFile : projectFiles) {
      IProjectDescription descriptor =
          root.getWorkspace().loadProjectDescription(projectFile);
      IProject project = root.getProject(descriptor.getName());
      // bring in the project to the workspace
      project.create(descriptor, progress.newChild(2));
      project.open(progress.newChild(8));
      projects.add(project);
    }

    // wait for any post-import operations too
    waitForProjects(projects);
    if (checkBuildErrors) {
      failIfBuildErrors("Imported projects have errors", projects);
    }

    return projects;
  }

  /** Fail if there are any build errors on any project in the workspace. */
  public static void failIfBuildErrors() throws CoreException {
    failIfBuildErrors("Projects have build errors", getWorkspace().getRoot().getProjects());
  }

  /** Fail if there are any build errors on the specified projects. */
  public static void failIfBuildErrors(String message, Collection<IProject> projects)
      throws CoreException {
    failIfBuildErrors(message, projects.toArray(new IProject[projects.size()]));
  }

  /** Fail if there are any build errors on the specified projects. */
  public static void failIfBuildErrors(String message, IProject... projects) throws CoreException {
    Set<String> errors = getAllBuildErrors(projects);
    assertTrue(message + "\n" + Joiner.on("\n").join(errors), errors.isEmpty());
  }

  /** Return a list of all build errors on the specified projects. */
  public static Set<String> getAllBuildErrors(IProject... projects) throws CoreException {
    Set<String> errors = new LinkedHashSet<>();
    for (IProject project : projects) {
      IMarker[] problems = project.findMarkers(IMarker.PROBLEM, true /* includeSubtypes */,
          IResource.DEPTH_INFINITE);
      for (IMarker problem : problems) {
        int severity = problem.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
        if (severity >= IMarker.SEVERITY_ERROR) {
          errors.add(formatProblem(problem));
        }
      }
    }
    return errors;
  }

  private static String formatProblem(IMarker problem) {
    StringBuilder sb = new StringBuilder();
    sb.append(problem.getResource().getFullPath());
    sb.append(':');
    sb.append(problem.getAttribute(IMarker.LINE_NUMBER, -1));
    sb.append(": ");
    sb.append(problem.getAttribute(IMarker.MESSAGE, ""));
    return sb.toString();
  }

  public static void waitForProjects(Collection<IProject> projects) {
    waitForProjects(projects.toArray(new IProject[0]));
  }

  /** Wait for any spawned jobs and builds to complete (e.g., validation jobs). */
  public static void waitForProjects(IProject... projects) {
    Runnable delayTactic = new Runnable() {
      @Override
      public void run() {
        Display display = Display.getCurrent();
        if (display != null) {
          while (display.readAndDispatch()) {
            /* spin */
          }
        }
        Thread.yield();
      }
    };
    waitForProjects(delayTactic, projects);
  }

  /** Wait for any spawned jobs and builds to complete (e.g., validation jobs). */
  public static void waitForProjects(Runnable delayTactic, IProject... projects) {
    if (projects.length == 0) {
      projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    }
    Stopwatch timer = Stopwatch.createStarted();
    try {
      Collection<Job> jobs = Collections.emptyList();
      Set<String> previousBuildErrors = Collections.emptySet();
      boolean buildErrorsChanging;
      do {
        // wait a little bit to give the builders a chance
        delayTactic.run();

        // NB: don't join jobs: see #1529
        // for details: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/pull/1529

        // identify any pending build-related jobs
        jobs = findPendingBuildJobs(projects);

        // track whether we've reached a fixpoint in the build errors
        Set<String> currentBuildErrors = getAllBuildErrors(projects);
        buildErrorsChanging = !previousBuildErrors.equals(currentBuildErrors);
        previousBuildErrors = currentBuildErrors;

        if (DEBUG || timer.elapsed(TimeUnit.SECONDS) > 10) {
          if (!jobs.isEmpty()) {
            System.err.printf("ProjectUtils#waitForProjects[%s]: waiting for %d jobs: %s\n", timer,
                jobs.size(), jobs);
          }
          if (buildErrorsChanging) {
            System.err.printf("ProjectUtils#waitForProjects[%s]: waiting for %d build errors\n",
                timer, currentBuildErrors.size());
          }
          // Uncomment if tests are failing to identify any other build-related jobs.
          dumpJobsState();
        }
      } while (!jobs.isEmpty() || buildErrorsChanging);
    } catch (CoreException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static void dumpJobsState() {
    Job[] otherJobs = Job.getJobManager().find(null);
    if (otherJobs.length == 0) {
      System.err.printf("ProjectUtils#dumpJobsState: no jobs\n");
      return;
    }

    System.err.printf("ProjectUtils#dumpJobsState: %d jobs:\n", otherJobs.length);
    for (Job job : otherJobs) {
      String status;
      switch (job.getState()) {
        case Job.RUNNING:
          status = "RUNNING";
          break;
        case Job.WAITING:
          status = "WAITING";
          break;
        case Job.SLEEPING:
          status = "SLEEPING";
          break;
        case Job.NONE:
          status = "NONE";
          break;
        default:
          status = "UNKNOWN(" + job.getState() + ")";
          break;
      }
      Object blockingJob = null;
      try {
        blockingJob = ReflectionUtil.invoke(Job.getJobManager(), "findBlockingJob",
            new Class<?>[] {InternalJob.class}, InternalJob.class, job);
      } catch (Exception ex) {
        System.err.println("Unable to fetch blocking-job: " + ex);
      }
      System.err.printf(" %s[%d,%s%s%s%s]: %s%s\n", job.getClass().getName(), job.getPriority(),
          status, (job.isUser() ? ",user" : ""), (job.isSystem() ? ",system" : ""),
          (job.isBlocking() ? ",blocking" : ""), job,
          (blockingJob != null ? "; blocked by: " + blockingJob : ""));
    }
  }

  /** Identify all jobs that we know of that are related to building. */
  private static Collection<Job> findPendingBuildJobs(IProject... projects) {
    Set<Job> jobs = new HashSet<>();
    IJobManager jobManager = Job.getJobManager();
    Collections.addAll(jobs, jobManager.find(ResourcesPlugin.FAMILY_MANUAL_BUILD));
    Collections.addAll(jobs, jobManager.find(ResourcesPlugin.FAMILY_AUTO_BUILD));
    // J2EEElementChangedListener.PROJECT_COMPONENT_UPDATE_JOB_FAMILY
    Collections.addAll(jobs, jobManager.find("org.eclipse.jst.j2ee.refactor.component"));
    // ServerPlugin.SHUTDOWN_JOB_FAMILY
    Collections.addAll(jobs, jobManager.find("org.eclipse.wst.server.core.family"));
    Collections.addAll(jobs, jobManager.find("org.eclipse.wst.server.ui.family"));
    Collections.addAll(jobs, jobManager.find(ValidationBuilder.FAMILY_VALIDATION_JOB));
    for (IProject project : projects) {
      Collections.addAll(jobs, jobManager.find(
          project.getName() + ValidatorManager.VALIDATOR_JOB_FAMILY));
    }
    // remove sleeping jobs
    for (Iterator<Job> iter = jobs.iterator(); iter.hasNext();) {
      Job job = iter.next();
      if (job.getState() == Job.SLEEPING) {
        iter.remove();
      }
    }
    return jobs;
  }

  private static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }

  private ProjectUtils() {}
}
