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
import org.junit.Rule;
import org.junit.Test;

public class PostInstallSetupDialogTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  private final SWTBot bot = new SWTWorkbenchBot();

  private final Runnable clickOk = new Runnable() {
    @Override
    public void run() {
      bot.button("OK").click();
    }
  };

  private final Runnable clickCancel = new Runnable() {
    @Override
    public void run() {
      bot.button("Cancel").click();
    }
  };

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
  public void testClickOk() throws InterruptedException {
    openDialogAndDoAction(clickOk);
  }

  @Test
  public void testClickCancel() throws InterruptedException {
    openDialogAndDoAction(clickCancel);
  }

  private void openDialogAndDoAction(final Runnable action)
      throws InterruptedException {
    Thread botThread = new Thread(new Runnable() {
      @Override
      public void run() {
        bot.waitUntil(Conditions.shellIsActive(
            "Google Cloud Tools for Eclipse Post-Install Setup"));
        UIThreadRunnable.syncExec(new Result<Boolean>() {
          @Override
          public Boolean run() {
            action.run();
            return null;
          }
        });
      }
    });
    botThread.start();

    PostInstallSetupDialog dialog = new PostInstallSetupDialog(shellResource.getShell());
    dialog.open();
    botThread.join();
  }
}
