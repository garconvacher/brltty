/*
 * BRLTTY - A background process providing access to the console screen (when in
 *          text mode) for a blind person using a refreshable braille display.
 *
 * Copyright (C) 1995-2007 by The BRLTTY Developers.
 *
 * BRLTTY comes with ABSOLUTELY NO WARRANTY.
 *
 * This is free software, placed under the terms of the
 * GNU General Public License, as published by the Free Software
 * Foundation.  Please see the file COPYING for details.
 *
 * Web Page: http://mielke.cc/brltty/
 *
 * This software is maintained by Dave Mielke <dave@mielke.cc>.
 */

#include "prologue.h"

#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <sys/mount.h>

#include "misc.h"
#include "async.h"
#include "mount.h"

#if defined(HAVE_MNTENT_H)
#include <mntent.h>

#define MOUNT_OPTION_RW MNTOPT_RW

typedef struct mntent MountEntry;
#define mountPath mnt_dir
#define mountReference mnt_fsname
#define mountType mnt_type
#define mountOptions mnt_opts

static FILE *
openMountsTable (int update) {
  FILE *table = setmntent(MOUNTED, (update? "a": "r"));
  if (!table)
    LogPrint((errno == ENOENT)? LOG_WARNING: LOG_ERR,
             "mounted file systems table open erorr: %s: %s",
             MOUNTED, strerror(errno));
  return table;
}

static void
closeMountsTable (FILE *table) {
  endmntent(table);
}

static MountEntry *
readMountsTable (FILE *table) {
  return getmntent(table);
}

static int
addMountEntry (FILE *table, MountEntry *entry) {
  if (!addmntent(table, entry)) return 1;
  LogPrint(LOG_ERR, "mounts table entry add error: %s[%s] -> %s: %s",
           entry->mnt_type, entry->mnt_fsname, entry->mnt_dir, strerror(errno));
  return 0;
}

#else /* mount paradigm */
#warning mount support not available on this platform

#define MOUNT_OPTION_RW "rw"

typedef struct {
  char *mountPath;
  char *mountReference;
  char *mountType;
  char *mountOptions;
} MountEntry;

static FILE *
openMountsTable (int update) {
  return NULL;
}

static void
closeMountsTable (FILE *table) {
}

static MountEntry *
readMountsTable (FILE *table) {
  return NULL;
}

static int
addMountEntry (FILE *table, MountEntry *entry) {
  return 0;
}
#endif /* mount paradigm */

char *
getMountPoint (int (*test) (const char *path, const char *type)) {
  char *path = NULL;
  FILE *table;

  if ((table = openMountsTable(0))) {
    MountEntry *entry;

    while ((entry = readMountsTable(table))) {
      if (test(entry->mountPath, entry->mountType)) {
        path = strdupWrapper(entry->mountPath);
        break;
      }
    }

    closeMountsTable(table);
  }

  return path;
}

static void updateMountsTable (MountEntry *entry);

static void
retryMountsTableUpdate (void *data) {
  MountEntry *entry = data;
  updateMountsTable(entry);
}

static void
updateMountsTable (MountEntry *entry) {
  int retry = 0;

  {
    FILE *table;

    if ((table = openMountsTable(1))) {
      addMountEntry(table, entry);
      closeMountsTable(table);
    } else if ((errno == EROFS) || (errno == EACCES)) {
      retry = 1;
    }
  }

  if (retry) {
    asyncRelativeAlarm(5000, retryMountsTableUpdate, entry);
  } else {
    if (entry->mountPath) free(entry->mountPath);
    if (entry->mountReference) free(entry->mountReference);
    if (entry->mountType) free(entry->mountType);
    if (entry->mountOptions) free(entry->mountOptions);
    free(entry);
  }
}

int
mountFileSystem (const char *path, const char *reference, const char *type) {
  if (mount(reference, path, type, 0, NULL) != -1) {
    LogPrint(LOG_NOTICE, "file system mounted: %s[%s] -> %s",
             type, reference, path);

    {
      MountEntry *entry = mallocWrapper(sizeof(*entry));
      memset(entry, 0, sizeof(*entry));
      entry->mountPath = strdupWrapper(path);
      entry->mountReference = strdupWrapper(reference);
      entry->mountType = strdupWrapper(type);
      entry->mountOptions = strdupWrapper(MOUNT_OPTION_RW);
      updateMountsTable(entry);
    }

    return 1;
  } else {
    LogPrint(LOG_ERR, "file system mount error: %s[%s] -> %s: %s",
             type, reference, path, strerror(errno));
  }

  return 0;
}
