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

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PostInstallSetupStartupTest {

  @Mock private IEclipsePreferences preferences;
  @Mock private Runnable dialogOpener;

  @Test
  public void testShowSetupDialogOnce_showIfNeverShown() {
    mockPreferenceOnlyFor("com.google.cloud.tools.eclipse.postinstall.setupDone", false);

    new PostInstallSetupStartup().showSetupDialogOnce(preferences, dialogOpener);
    verify(dialogOpener, times(1)).run();
  }

  @Test
  public void testShowSetupDialogOnce_doNotShowIfEverShown() {
    mockPreferenceOnlyFor("com.google.cloud.tools.eclipse.postinstall.setupDone", true);

    new PostInstallSetupStartup().showSetupDialogOnce(preferences, dialogOpener);
    verify(dialogOpener, never()).run();
  }

  private void mockPreferenceOnlyFor(String key, boolean value) {
    when(preferences.getBoolean(anyString(), eq(true))).thenReturn(!value);
    when(preferences.getBoolean(anyString(), eq(false))).thenReturn(!value);

    when(preferences.getBoolean(eq(key), eq(true))).thenReturn(value);
    when(preferences.getBoolean(eq(key), eq(false))).thenReturn(value);
  }
}
