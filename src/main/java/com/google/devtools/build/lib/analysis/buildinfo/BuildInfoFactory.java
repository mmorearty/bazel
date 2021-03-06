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
package com.google.devtools.build.lib.analysis.buildinfo;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.Root;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.vfs.PathFragment;

import java.io.Serializable;

/**
 * A factory for language-specific build-info files. Use this to translate the build-info into
 * target-independent language-specific files. The generated actions are registered into the action
 * graph on every build, but only executed if anything depends on them.
 */
public interface BuildInfoFactory extends Serializable {
  /**
   * Type of the build-data artifact.
   */
  public enum BuildInfoType {
    /**
     * Ignore changes to this file for the purposes of determining whether an action needs to be
     * re-executed. I.e., the action is only re-executed if at least one other input has changed.
     */
    NO_REBUILD,

    /**
     * Changes to this file trigger re-execution of actions, similar to source file changes.
     */
    FORCE_REBUILD_IF_CHANGED;
  }

  /**
   * Context for the creation of build-info artifacts.
   */
  public interface BuildInfoContext {
    Artifact getBuildInfoArtifact(PathFragment rootRelativePath, Root root, BuildInfoType type);
    Root getBuildDataDirectory();
  }

  /**
   * Build-info key for lookup from the {@link
   * com.google.devtools.build.lib.analysis.AnalysisEnvironment}.
   */
  public static final class BuildInfoKey implements Serializable {
    private final String name;

    public BuildInfoKey(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof BuildInfoKey)) {
        return false;
      }
      return name.equals(((BuildInfoKey) o).name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }

  /**
   * Create actions and artifacts for language-specific build-info files.
   */
  BuildInfoCollection create(BuildInfoContext context, BuildConfiguration config,
      Artifact buildInfo, Artifact buildChangelist);

  /**
   * Returns the key for the information created by this factory.
   */
  BuildInfoKey getKey();

  /**
   * Returns false if this build info factory is disabled based on the configuration (usually by
   * checking if all required configuration fragments are present).
   */
  boolean isEnabled(BuildConfiguration config);
}
