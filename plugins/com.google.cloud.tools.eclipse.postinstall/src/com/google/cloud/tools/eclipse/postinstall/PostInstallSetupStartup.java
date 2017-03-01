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

import com.google.common.annotations.VisibleForTesting;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

public class PostInstallSetupStartup implements IStartup {

  private static final Logger logger = Logger.getLogger(PostInstallSetupStartup.class.getName());

  private static final String PLUGIN_ID = "com.google.cloud.tools.eclipse.postinstall";
  private static final String PREFERENCE_KEY_SETUP_DONE =
      "com.google.cloud.tools.eclipse.postinstall.setupDone";

  @Override
  public void earlyStartup() {
    IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(PLUGIN_ID);
    showSetupDialogOnce(preferences, new Runnable() {
      @Override
      public void run() {
        openDialogInUiThread();
      }
    });
  }

  @VisibleForTesting
  void showSetupDialogOnce(IEclipsePreferences preferences, Runnable dialogOpener) {
    try {
      boolean setupDone = preferences.getBoolean(PREFERENCE_KEY_SETUP_DONE, false);
      if (!setupDone) {
        dialogOpener.run();

        preferences.putBoolean(PREFERENCE_KEY_SETUP_DONE, false);
        preferences.flush();
      }
    } catch (BackingStoreException ex) {
      logger.log(Level.WARNING, "Failed to save preferecens.", ex);
    }
  }

  private void openDialogInUiThread() {
    final IWorkbench workbench = PlatformUI.getWorkbench();
    workbench.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        Shell shell = workbench.getActiveWorkbenchWindow().getShell();
        new PostInstallSetupDialog(shell).open();
      }
    });
  }
}
