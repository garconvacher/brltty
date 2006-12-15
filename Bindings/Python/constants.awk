###############################################################################
# BRLTTY - A background process providing access to the console screen (when in
#          text mode) for a blind person using a refreshable braille display.
#
# Copyright (C) 1995-2006 by The BRLTTY Developers.
#
# BRLTTY comes with ABSOLUTELY NO WARRANTY.
#
# This is free software, placed under the terms of the
# GNU General Public License, as published by the Free Software
# Foundation.  Please see the file COPYING for details.
#
# Web Page: http://mielke.cc/brltty/
#
# This software is maintained by Dave Mielke <dave@mielke.cc>.
###############################################################################

function brlCommand(name, symbol, value, help) {
  writeCommandDefinition(name, value)
}

function brlBlock(name, symbol, value, help) {
  if (name == "PASSCHAR") return
  if (name == "PASSKEY") return

  if (value ~ /^0[xX][0-9a-fA-F]+00$/) {
    writeCommandDefinition(name, tolower(value) "00")
  }
}

function brlKey(name, symbol, value, help) {
}

function brlFlag(name, symbol, value, help) {
  if (value ~ /^0[xX][0-9a-fA-F]+0000$/) {
    value = tolower(substr(value, 1, length(value)-4))
    if (name ~ /^CHAR_/) {
      name = substr(name, 6)
    } else {
      value = value "00"
    }
  } else if (value ~ /^\(/) {
    gsub("BRL_FLG_", "KEY_FLG_", value)
  } else {
    return
  }
  writePythonAssignment("KEY_FLG_" name, value)
}

function brlDot(number, symbol, value, help) {
  writePythonAssignment("DOT" number, value)
}

function apiMask(name, symbol, value, help) {
  writePythonAssignment("KEY_" name, "c_brlapi." symbol)
}

function apiShift(name, symbol, value, help) {
  value = hexadecimalValue(value)
  writePythonAssignment("KEY_" name, value)
}

function apiType(name, symbol, value, help) {
  value = hexadecimalValue(value)
  writePythonAssignment("KEY_TYPE_" name, value)
}

function apiKey(name, symbol, value, help) {
  value = hexadecimalValue(value)

  if (name == "FUNCTION") {
    for (i=0; i<35; ++i) {
      writeKeyDefinition("F" (i+1), "(" value " + " i ")")
    }
  } else {
    writeKeyDefinition(name, value)
  }
}

function writeCommandDefinition(name, value) {
  writePythonAssignment("KEY_CMD_" name, value)
}

function writeKeyDefinition(name, value) {
  writePythonAssignment("KEY_SYM_" name, value)
}

function writePythonAssignment(name, value) {
  print name " = " value
}

function hexadecimalValue(value) {
  value = tolower(value)
  return value
}

