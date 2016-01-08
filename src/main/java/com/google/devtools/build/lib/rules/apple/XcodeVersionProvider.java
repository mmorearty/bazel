// Copyright 2015 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.rules.apple;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.analysis.TransitiveInfoProvider;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;

import java.util.List;

/**
 * Provides the information in a single {@code xcode_version} target. A single target of this rule
 * contains an official version label decided by Apple and a number of supported aliases one might
 * use to reference this version.
 *
 * <p>For example, one may want to reference official xcode version 7.0.1 using the "7" or
 * "7.0" aliases.
 */
@Immutable
public final class XcodeVersionProvider implements TransitiveInfoProvider {
  private final Label label;
  private final DottedVersion version;
  private final ImmutableList<String> aliases;
  
  XcodeVersionProvider(Label label, DottedVersion version, List<String> aliases) {
    this.label = label;
    this.version = version;
    this.aliases = ImmutableList.copyOf(aliases);
  }

  /**
   * Returns the label of the owning target of this provider.
   */
  public Label getLabel() {
    return label;
  }

  /**
   * Returns the official xcode version the owning {@code xcode_version} target is referencing.
   */
  public DottedVersion getVersion() {
    return version;
  }

  /**
   * Returns the accepted string aliases for this xcode version.
   */
  public List<String> getAliases() {
    return aliases;
  }
}
