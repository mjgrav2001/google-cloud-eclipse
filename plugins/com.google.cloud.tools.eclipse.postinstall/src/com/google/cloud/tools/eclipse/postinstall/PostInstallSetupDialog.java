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

import com.google.cloud.tools.eclipse.preferences.AnalyticsOptInArea;
import com.google.cloud.tools.eclipse.preferences.AnalyticsPreferences;
import com.google.cloud.tools.eclipse.preferences.areas.PreferenceArea;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class PostInstallSetupDialog extends Dialog {

  private static final Logger logger = Logger.getLogger(PostInstallSetupDialog.class.getName());

  private final ScopedPreferenceStore analyticsPreferenceStore;
  private final PreferenceArea analyticsArea;

  public PostInstallSetupDialog(Shell parent) {
    super(parent);

    analyticsArea = new AnalyticsOptInArea();
    analyticsPreferenceStore = (ScopedPreferenceStore)
        com.google.cloud.tools.eclipse.preferences.Activator.getDefault().getPreferenceStore();
    analyticsArea.setPreferenceStore(analyticsPreferenceStore);

    // TODO(chanseok): Remove the key itself, after we get rid of the on-the-fly opt-in dialog.
    analyticsPreferenceStore.setValue(AnalyticsPreferences.ANALYTICS_OPT_IN_REGISTERED, true);
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(Messages.getString("post.install.setup.dialog.title")); //$NON-NLS-1$
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);
    new Label(composite, SWT.NONE).setText(Messages.getString("welcome.message")); //$NON-NLS-1$
    Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
    GridDataFactory.fillDefaults().applyTo(separator);

    analyticsArea.createContents(composite);

    GridLayoutFactory.swtDefaults().margins(10, 10).spacing(5, 10).generateLayout(composite);
    return composite;
  }

  @Override
  protected void okPressed() {
    analyticsArea.performApply();
    super.okPressed();
    savePreferences();
  }

  @Override
  protected void cancelPressed() {
    super.cancelPressed();
    savePreferences();
  }

  /** When the dialog closes in other ways than pressing the buttons. */
  @Override
  protected void handleShellCloseEvent() {
    super.handleShellCloseEvent();
    savePreferences();
  }

  private void savePreferences() {
    try {
      analyticsPreferenceStore.save();
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Failed to save preferences.", ex);
    }
  }
}
