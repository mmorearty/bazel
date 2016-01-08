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

package com.google.devtools.build.lib.server;

import static com.google.common.truth.Truth.assertThat;

import com.google.devtools.build.lib.server.RPCService.UnknownCommandException;
import com.google.devtools.build.lib.util.io.OutErr;
import com.google.devtools.build.lib.util.io.RecordingOutErr;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Just makes sure the RPC service understands commands.
 */
public class RPCServiceTest extends TestCase {

  private ServerCommand helloWorldCommand = new ServerCommand() {
    @Override
    public int exec(List<String> args, OutErr outErr, long firstContactTime) throws Exception {
      outErr.printOut("Heelllloo....");
      outErr.printErr("...world!");
      return 42;
    }
    @Override
    public boolean shutdown() {
      return false;
    }
  };

  private RPCService service =
      new RPCService(helloWorldCommand);

  public void testUnknownCommandException() {
    try {
      service.executeRequest(Arrays.asList("unknown"), new RecordingOutErr(), 0);
      fail();
    } catch (UnknownCommandException e) {
      // success
    } catch (Exception e){
      fail();
    }
  }

  public void testCommandGetsExecuted() throws Exception {
    RecordingOutErr outErr = new RecordingOutErr();
    int exitStatus = service.executeRequest(Arrays.asList("blaze"), outErr, 0);

    assertEquals(42, exitStatus);
    assertEquals("Heelllloo....", outErr.outAsLatin1());
    assertEquals("...world!", outErr.errAsLatin1());
  }

  public void testDelimitation() throws Exception {
    final List<String> savedArgs = new ArrayList<>();

    RPCService service =
        new RPCService(new ServerCommand() {
            @Override
            public int exec(List<String> args, OutErr outErr, long firstContactTime)
                throws Exception {
              savedArgs.addAll(args);
              return 0;
            }
            @Override
            public boolean shutdown() {
              return false;
            }
          });

    List<String> args = Arrays.asList("blaze", "", " \n", "", "", "", "foo");
    service.executeRequest(args, new RecordingOutErr(), 0);
    assertEquals(args.subList(1, args.size()),
                 savedArgs);
  }

  public void testShutdownState() throws Exception {
    assertFalse(service.isShutdown());
    service.shutdown();
    assertTrue(service.isShutdown());
    service.shutdown();
    assertTrue(service.isShutdown());
  }

  public void testCommandFailsAfterShutdown() throws Exception {
    RecordingOutErr outErr = new RecordingOutErr();
    service.shutdown();
    try {
      service.executeRequest(Arrays.asList("blaze"), outErr, 0);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Received request after shutdown.");
      /* Make sure it does not execute the command! */
      assertThat(outErr.outAsLatin1()).isEmpty();
      assertThat(outErr.errAsLatin1()).isEmpty();
    }
  }

}
