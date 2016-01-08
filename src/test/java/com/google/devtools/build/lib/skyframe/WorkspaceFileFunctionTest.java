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

package com.google.devtools.build.lib.skyframe;

import com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import com.google.devtools.build.lib.bazel.rules.BazelRulesModule;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.NoSuchTargetException;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.PackageFactory;
import com.google.devtools.build.lib.packages.Rule;
import com.google.devtools.build.lib.testutil.MoreAsserts;
import com.google.devtools.build.lib.testutil.TestRuleClassProvider;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.RootedPath;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyKey;

import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;

/**
 * Test for {@link WorkspaceFileFunction}.
 */
public class WorkspaceFileFunctionTest extends BuildViewTestCase {

  private WorkspaceFileFunction skyFunc;
  private FakeFileValue fakeWorkspaceFileValue;

  private static class FakeFileValue extends FileValue {
    private boolean exists;
    private long size;

    FakeFileValue() {
      super();
      exists = true;
      size = 0L;
    }

    @Override
    public RootedPath realRootedPath() {
      throw new UnsupportedOperationException();
    }

    @Override
    public FileStateValue realFileStateValue() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists() {
      return exists;
    }

    private void setExists(boolean exists) {
      this.exists = exists;
    }

    @Override
    public long getSize() {
      return size;
    }

    private void setSize(long size) {
      this.size = size;
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    ConfiguredRuleClassProvider ruleClassProvider = TestRuleClassProvider.getRuleClassProvider();
    skyFunc =
        new WorkspaceFileFunction(
            ruleClassProvider,
            new PackageFactory(
                TestRuleClassProvider.getRuleClassProvider(),
                new BazelRulesModule().getPackageEnvironmentExtension()),
            directories);
    fakeWorkspaceFileValue = new FakeFileValue();
  }

  private Label getLabelMapping(Package pkg, String name) throws NoSuchTargetException {
    return (Label) ((Rule) pkg.getTarget(name)).getAttributeContainer().getAttr("actual");
  }

  private RootedPath createWorkspaceFile(String... contents) throws IOException {
    Path workspacePath = scratch.overwriteFile("WORKSPACE", contents);
    fakeWorkspaceFileValue.setSize(workspacePath.getFileSize());
    return RootedPath.toRootedPath(
        workspacePath.getParentDirectory(), new PathFragment(workspacePath.getBaseName()));
  }

  private SkyFunction.Environment getEnv() {
    SkyFunction.Environment env = Mockito.mock(SkyFunction.Environment.class);
    Mockito.when(env.getValue(Matchers.<SkyKey>any())).thenReturn(fakeWorkspaceFileValue);
    return env;
  }

  public void testInvalidRepo() throws Exception {
    RootedPath workspacePath = createWorkspaceFile("workspace(name = 'foo$')");
    PackageValue value =
        (PackageValue) skyFunc.compute(PackageValue.workspaceKey(workspacePath), getEnv());
    Package pkg = value.getPackage();
    assertTrue(pkg.containsErrors());
    MoreAsserts.assertContainsEvent(pkg.getEvents(), "target names may not contain '$'");
  }

  public void testBindFunction() throws Exception {
    String lines[] = {"bind(name = 'foo/bar',", "actual = '//foo:bar')"};
    RootedPath workspacePath = createWorkspaceFile(lines);

    SkyKey key = PackageValue.workspaceKey(workspacePath);
    PackageValue value = (PackageValue) skyFunc.compute(key, getEnv());
    Package pkg = value.getPackage();
    assertEquals(Label.parseAbsolute("//foo:bar"), getLabelMapping(pkg, "foo/bar"));
    MoreAsserts.assertNoEvents(pkg.getEvents());
  }

  public void testBindArgsReversed() throws Exception {
    String lines[] = {"bind(actual = '//foo:bar', name = 'foo/bar')"};
    RootedPath workspacePath = createWorkspaceFile(lines);

    SkyKey key = PackageValue.workspaceKey(workspacePath);
    PackageValue value = (PackageValue) skyFunc.compute(key, getEnv());
    Package pkg = value.getPackage();
    assertEquals(Label.parseAbsolute("//foo:bar"), getLabelMapping(pkg, "foo/bar"));
    MoreAsserts.assertNoEvents(pkg.getEvents());
  }

  public void testNonExternalBinding() throws Exception {
    // name must be a valid label name.
    String lines[] = {"bind(name = 'foo:bar', actual = '//bar/baz')"};
    RootedPath workspacePath = createWorkspaceFile(lines);

    PackageValue value =
        (PackageValue) skyFunc.compute(PackageValue.workspaceKey(workspacePath), getEnv());
    Package pkg = value.getPackage();
    assertTrue(pkg.containsErrors());
    MoreAsserts.assertContainsEvent(pkg.getEvents(), "target names may not contain ':'");
  }

  public void testWorkspaceFileParsingError() throws Exception {
    // //external:bar:baz is not a legal package.
    String lines[] = {"bind(name = 'foo/bar', actual = '//external:bar:baz')"};
    RootedPath workspacePath = createWorkspaceFile(lines);

    PackageValue value =
        (PackageValue) skyFunc.compute(PackageValue.workspaceKey(workspacePath), getEnv());
    Package pkg = value.getPackage();
    assertTrue(pkg.containsErrors());
    MoreAsserts.assertContainsEvent(pkg.getEvents(), "target names may not contain ':'");
  }

  public void testNoWorkspaceFile() throws Exception {
    // Even though the WORKSPACE exists, Skyframe thinks it doesn't, so it doesn't.
    String lines[] = {"bind(name = 'foo/bar', actual = '//foo:bar')"};
    RootedPath workspacePath = createWorkspaceFile(lines);
    fakeWorkspaceFileValue.setExists(false);

    PackageValue value =
        (PackageValue) skyFunc.compute(PackageValue.workspaceKey(workspacePath), getEnv());
    Package pkg = value.getPackage();
    assertFalse(pkg.containsErrors());
    MoreAsserts.assertNoEvents(pkg.getEvents());
  }

  public void testListBindFunction() throws Exception {
    String lines[] = {
        "L = ['foo', 'bar']", "bind(name = '%s/%s' % (L[0], L[1]),", "actual = '//foo:bar')"};
    RootedPath workspacePath = createWorkspaceFile(lines);

    SkyKey key = PackageValue.workspaceKey(workspacePath);
    PackageValue value = (PackageValue) skyFunc.compute(key, getEnv());
    Package pkg = value.getPackage();
    assertEquals(Label.parseAbsolute("//foo:bar"), getLabelMapping(pkg, "foo/bar"));
    MoreAsserts.assertNoEvents(pkg.getEvents());
  }
}
