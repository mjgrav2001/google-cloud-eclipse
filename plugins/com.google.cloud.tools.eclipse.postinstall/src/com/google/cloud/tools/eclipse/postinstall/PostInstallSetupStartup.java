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

package com.google.cloud.tools.eclipse.postinstall;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

public class PostInstallSetupStartup implements IStartup {

  private static final String PLUGIN_ID = "com.google.cloud.tools.eclipse.postinstall";

  @Override
  public void earlyStartup() {
    IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(PLUGIN_ID);
    boolean setupDone = preferences.getBoolean("POST_INSTALL_SETUP_DONE", false);
    if (!setupDone) {
      showPostInstallSetupDialog();
    }
  }

  private void showPostInstallSetupDialog() {
    final IWorkbench workbench = PlatformUI.getWorkbench();
    workbench.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        Shell shell = workbench.getActiveWorkbenchWindow().getShell();
      }
    });
  }
}
