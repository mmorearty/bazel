// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.rules.java;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.TransitiveInfoProvider;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;

import javax.annotation.Nullable;

/**
 * Provides information about jar files produced by a Java rule.
 */
@Immutable
public final class JavaRuleOutputJarsProvider implements TransitiveInfoProvider {
  @Nullable private final Artifact classJar;
  private final Artifact srcJar;
  private final Artifact genClassJar;
  private final Artifact gensrcJar;

  public JavaRuleOutputJarsProvider(
      Artifact classJar, Artifact srcJar, Artifact genClassJar, Artifact gensrcJar) {
    this.classJar = classJar;
    this.srcJar = srcJar;
    this.genClassJar = genClassJar;
    this.gensrcJar = gensrcJar;
  }

  @Nullable
  public Artifact getClassJar() {
    return classJar;
  }

  public Artifact getSrcJar() {
    return srcJar;
  }

  public Artifact getGenClassJar() {
    return genClassJar;
  }

  public Artifact getGensrcJar() {
    return gensrcJar;
  }
}
