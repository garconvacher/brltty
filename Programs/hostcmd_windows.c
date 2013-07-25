/*
 * BRLTTY - A background process providing access to the console screen (when in
 *          text mode) for a blind person using a refreshable braille display.
 *
 * Copyright (C) 1995-2013 by The BRLTTY Developers.
 *
 * BRLTTY comes with ABSOLUTELY NO WARRANTY.
 *
 * This is free software, placed under the terms of the
 * GNU General Public License, as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any
 * later version. Please see the file LICENSE-GPL for details.
 *
 * Web Page: http://mielke.cc/brltty/
 *
 * This software is maintained by Dave Mielke <dave@mielke.cc>.
 */

#include "prologue.h"

#include "log.h"
#include "sys_windows.h"
#include "hostcmd_windows.h"
#include "hostcmd_internal.h"

int
isHostCommand (const char *path) {
  return 0;
}

void
subconstructHostCommandStream (HostCommandStream *hcs) {
}

void
subdestructHostCommandStream (HostCommandStream *hcs) {
}

int
prepareHostCommandStream (HostCommandStream *hcs) {
  return 1;
}

int
runCommand (
  int *result,
  const char *const *command,
  HostCommandStream *streams,
  int asynchronous
) {
  int ok = 0;
  char *line = makeWindowsCommandLine(command);

  if (line) {
    STARTUPINFO startup;
    PROCESS_INFORMATION process;

    memset(&startup, 0, sizeof(startup));
    startup.cb = sizeof(startup);

    memset(&process, 0, sizeof(process));

    logMessage(LOG_DEBUG, "host command: %s", line);
    if (CreateProcess(NULL, line, NULL, NULL, TRUE,
                      CREATE_NEW_PROCESS_GROUP,
                      NULL, NULL, &startup, &process)) {
      DWORD status;

      ok = 1;

      if (asynchronous) {
        *result = 0;
      } else {
        *result = 0XFF;

        while ((status = WaitForSingleObject(process.hProcess, INFINITE)) == WAIT_TIMEOUT);

        if (status == WAIT_OBJECT_0) {
          DWORD code;

          if (GetExitCodeProcess(process.hProcess, &code)) {
            *result = code;
          } else {
            logWindowsSystemError("GetExitCodeProcess");
          }
        } else {
          logWindowsSystemError("WaitForSingleObject");
        }
      }

      CloseHandle(process.hProcess);
      CloseHandle(process.hThread);
    } else {
      logWindowsSystemError("CreateProcess");
    }

    free(line);
  } else {
    logMallocError();
  }

  return ok;
}