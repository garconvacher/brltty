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

#ifndef BRLTTY_INCLUDED_API_CONTROL
#define BRLTTY_INCLUDED_API_CONTROL

#include "prologue.h"

#include "brl.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#ifdef ENABLE_API
extern void api_identify (int full);
extern const char *const api_parameters[];

extern int api_start (BrailleDisplay *brl, char **parameters);
extern void api_stop (BrailleDisplay *brl);

extern void api_link (BrailleDisplay *brl);
extern void api_unlink (BrailleDisplay *brl);

extern void api_suspend (BrailleDisplay *brl);
extern int api_resume (BrailleDisplay *brl);

extern int api_claimDriver (BrailleDisplay *brl);
extern void api_releaseDriver (BrailleDisplay *brl);

extern int api_flush (BrailleDisplay *brl);
extern int api_handleCommand (int command);
extern int api_handleKeyEvent (unsigned char set, unsigned char key, int press);

extern int apiStarted;
extern void apiClaimDriver (void);
extern void apiReleaseDriver (void);
#endif /* ENABLE_API */

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* BRLTTY_INCLUDED_API_CONTROL */