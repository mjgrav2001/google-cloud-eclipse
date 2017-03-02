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

import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PostInstallSetupDialogTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  @Mock private ScopedPreferenceStore analyticsPreferenceStore;

  private final SWTBot bot = new SWTWorkbenchBot();

  @Before
  public void setUp() {

  }

  @Test
  public void testDialogArea() {
    Composite composite = new Composite(shellResource.getShell(), SWT.NONE);
    new PostInstallSetupDialog(null).createDialogArea(composite);
    assertTrue(containsWidgetWithText(composite,
        "Take a moment to complete optional preference setup."));
    assertTrue(containsWidgetWithText(composite,
        "&Share anonymous usage statistics of Cloud Tools for Eclipse with Google"));
  }

  private static boolean containsWidgetWithText(Control control, String text) {
    if (control instanceof Label) {
      return ((Label) control).getText().equals(text);
    } else if (control instanceof Button) {
      return ((Button) control).getText().equals(text);
    }

    for (Control child : ((Composite) control).getChildren()) {
      if (containsWidgetWithText(child, text)) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void testClickOk() throws InterruptedException, ExecutionException, TimeoutException {
    openDialogAndDoAction(new ClickOkAction());
  }

  @Test
  public void testClickCancel() throws InterruptedException, ExecutionException, TimeoutException {
    openDialogAndDoAction(new ClickCancelAction());
  }

  private void openDialogAndDoAction(final Result<Boolean> action)
      throws InterruptedException, ExecutionException, TimeoutException {
    FutureTask<Boolean> botThread = new FutureTask<>(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        bot.waitUntil(Conditions.shellIsActive(
            "Google Cloud Tools for Eclipse Post-Install Setup"));
        return UIThreadRunnable.syncExec(action);
      }
    });
    botThread.run();

    new PostInstallSetupDialog(shellResource.getShell(), analyticsPreferenceStore).open();
    botThread.get(30, TimeUnit.SECONDS);
  }

  private class ClickOkAction implements Result<Boolean> {
    @Override
    public Boolean run() {
      bot.button("OK").click();
      return true;
    }
  };

  private class ClickCancelAction implements Result<Boolean> {
    @Override
    public Boolean run() {
      bot.button("Cancel").click();
      return true;
    }
  };
}
