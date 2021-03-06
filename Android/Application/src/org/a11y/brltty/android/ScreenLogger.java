/*
 * BRLTTY - A background process providing access to the console screen (when in
 *          text mode) for a blind person using a refreshable braille display.
 *
 * Copyright (C) 1995-2018 by The BRLTTY Developers.
 *
 * BRLTTY comes with ABSOLUTELY NO WARRANTY.
 *
 * This is free software, placed under the terms of the
 * GNU Lesser General Public License, as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any
 * later version. Please see the file LICENSE-LGPL for details.
 *
 * Web Page: http://brltty.app/
 *
 * This software is maintained by Dave Mielke <dave@mielke.cc>.
 */

package org.a11y.brltty.android;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import android.util.Log;
import android.os.Bundle;

import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import android.graphics.Rect;

public abstract class ScreenLogger {
  private static final String LOG_TAG = ScreenLogger.class.getName();

  private ScreenLogger () {
  }

  private static String getText (AccessibilityNodeInfo node) {
    String text = ScreenUtilities.getText(node);
    if (text != null) return text;
    return ScreenUtilities.getDescription(node);
  }

  private static String shrinkText (String text) {
    final int threshold = 50;
    final char delimiter = '\n';

    int length = text.length();
    int to = text.lastIndexOf(delimiter);
    int from = (to == -1)? length: text.indexOf(delimiter);
    to += 1;

    from = Math.min(from, threshold);
    to = Math.max(to, (length - threshold));

    if (from < to) {
      text = text.substring(0, from) + "[...]" + text.substring(to);
    }

    return text;
  }

  private static final void add (StringBuilder sb, String value) {
    if (value != null) {
      if (sb.length() > 0) sb.append(' ');
      sb.append(value);
    }
  }

  private static final void add (StringBuilder sb, boolean condition, String trueValue, String falseValue) {
    add(sb, (condition? trueValue: falseValue));
  }

  private static final void add (StringBuilder sb, boolean condition, String trueValue) {
    add(sb, condition, trueValue, null);
  }

  private static final void add (StringBuilder sb, String label, CharSequence value) {
    if (value != null) add(sb, String.format("%s=%s", label, value));
  }

  private static final void add (StringBuilder sb, String label, int value) {
    add(sb, label, Integer.toString(value));
  }

  private static final void add (StringBuilder sb, String label, int value, Map<Integer, String> names) {
    String name = names.get(value);

    if (name != null) {
      add(sb, label, name);
    } else {
      add(sb, label, value);
    }
  }

  private static void log (String message) {
    Log.d(LOG_TAG, message);
  }

  private static void log (String label, String data) {
    log((label + ": " + data));
  }

  private static class ActionLabelMap extends LinkedHashMap<Integer, String> {
    public final void put (String name, int action) {
      put(action, name);
    }

    public final void put (String name, AccessibilityNodeInfo.AccessibilityAction action) {
      put(name, action.getId());
    }
  }

  private final static ActionLabelMap actionLabels = new ActionLabelMap()
  {
    {
      put("clk", AccessibilityNodeInfo.ACTION_CLICK);
      put("lck", AccessibilityNodeInfo.ACTION_LONG_CLICK);
      put("scf", AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
      put("scb", AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
      put("mvn", AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY);
      put("mvp", AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY);
      put("mhn", AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT);
      put("mhp", AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT);
      put("sls", AccessibilityNodeInfo.ACTION_SELECT);
      put("slc", AccessibilityNodeInfo.ACTION_CLEAR_SELECTION);
      put("ifs", AccessibilityNodeInfo.ACTION_FOCUS);
      put("ifc", AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
      put("afs", AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
      put("afc", AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
      put("sel", AccessibilityNodeInfo.ACTION_SET_SELECTION);
      put("cbc", AccessibilityNodeInfo.ACTION_COPY);
      put("cbx", AccessibilityNodeInfo.ACTION_CUT);
      put("cbp", AccessibilityNodeInfo.ACTION_PASTE);
      put("dsms", AccessibilityNodeInfo.ACTION_DISMISS);
      put("clps", AccessibilityNodeInfo.ACTION_COLLAPSE);
      put("xpnd", AccessibilityNodeInfo.ACTION_EXPAND);
      put("txs", AccessibilityNodeInfo.ACTION_SET_TEXT);

      if (ApplicationUtilities.haveMarshmallow) {
        put("cck", AccessibilityNodeInfo.AccessibilityAction.ACTION_CONTEXT_CLICK);
      }
    }
  };

  public static String toString (AccessibilityNodeInfo node) {
    StringBuilder sb = new StringBuilder();
    add(sb, ScreenUtilities.getClassName(node));

    {
      String text = ScreenUtilities.getText(node);

      if (text != null) {
        sb.append(' ');
        sb.append('"');
        sb.append(shrinkText(text));
        sb.append('"');
      }
    }

    {
      String description = ScreenUtilities.getDescription(node);

      if (description != null) {
        sb.append(' ');
        sb.append('(');
        sb.append(shrinkText(description));
        sb.append(')');
      }
    }

    {
      AccessibilityNodeInfo parent = node.getParent();

      if (parent != null) {
        parent.recycle();
        parent = null;
      } else {
        add(sb, "root");
      }
    }

    {
      int count = node.getChildCount();
      if (count > 0) add(sb, "cld", count);
    }

    if (ApplicationUtilities.haveJellyBean) {
      add(sb, !node.isVisibleToUser(), "inv");
    }

    add(sb, !node.isEnabled(), "dsb");
    add(sb, node.isSelected(), "sld");
    add(sb, node.isScrollable(), "scr");
    add(sb, node.isFocusable(), "ifb");
    add(sb, node.isFocused(), "ifd");

    if (ApplicationUtilities.haveJellyBean) {
      add(sb, node.isAccessibilityFocused(), "afd");
    }

    add(sb, node.isClickable(), "clb");
    add(sb, node.isLongClickable(), "lcb");

    if (ApplicationUtilities.haveMarshmallow) {
      add(sb, node.isContextClickable(), "ccb");
    }
       
    add(sb, node.isCheckable(), "ckb");
    add(sb, node.isChecked(), "ckd");
    add(sb, node.isPassword(), "pwd");

    if (ApplicationUtilities.haveJellyBeanMR2) {
      add(sb, ScreenUtilities.isEditable(node), "edt");

      {
        int start = node.getTextSelectionStart();
        int end = node.getTextSelectionEnd();

        if (!((start == -1) && (end == -1))) {
          add(sb, "sel");
          sb.append('(');
          sb.append(start);

          if (end != start) {
            sb.append("..");
            sb.append(end);
          }

          sb.append(')');
        }
      }
    }

    if (ApplicationUtilities.haveKitkat) {
      {
        AccessibilityNodeInfo.RangeInfo range = node.getRangeInfo();

        if (range != null) {
          add(sb, "rng");
          String format = ScreenUtilities.getRangeValueFormat(range);

          sb.append('(');
          sb.append(String.format(format, range.getMin()));
          sb.append("..");
          sb.append(String.format(format, range.getMax()));
          sb.append('@');
          sb.append(String.format(format, range.getCurrent()));
          sb.append(')');
        }
      }

      {
        AccessibilityNodeInfo.CollectionInfo collection = node.getCollectionInfo();

        if (collection != null) {
          add(sb, "col");
          sb.append('(');

          sb.append(collection.getColumnCount());
          sb.append('x');
          sb.append(collection.getRowCount());

          sb.append(',');
          sb.append(collection.isHierarchical()? "tree": "flat");

          {
            String mode = ScreenUtilities.getSelectionModeLabel(collection);

            if (mode != null) {
              sb.append(',');
              sb.append(mode);
            }
          }

          sb.append(')');
        }
      }

      {
        AccessibilityNodeInfo.CollectionItemInfo item = node.getCollectionItemInfo();

        if (item != null) {
          add(sb, "itm");
          sb.append('(');

          sb.append(item.getColumnSpan());
          sb.append('x');
          sb.append(item.getRowSpan());
          sb.append('+');
          sb.append(item.getColumnIndex());
          sb.append('+');
          sb.append(item.getRowIndex());

          if (item.isHeading()) {
            sb.append(',');
            sb.append("hdg");
          }

          if (ApplicationUtilities.haveLollipop) {
            if (item.isSelected()) {
              sb.append(',');
              sb.append("sel");
            }
          }

          sb.append(')');
        }
      }
    }

    if (ApplicationUtilities.haveLollipop) {
      for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
        String label = actionLabels.get(action.getId());
        if (label != null) add(sb, label);
      }
    } else {
      int actions = node.getActions();

      for (Integer action : actionLabels.keySet()) {
        if ((actions & action) != 0) {
          add(sb, actionLabels.get(action));
        }
      }
    }

    if (ApplicationUtilities.haveJellyBeanMR1) {
      AccessibilityNodeInfo subnode = node.getLabelFor();

      if (subnode != null) {
        add(sb, "lbf", getText(subnode));
        subnode.recycle();
        subnode = null;
      }
    }

    if (ApplicationUtilities.haveJellyBeanMR1) {
      AccessibilityNodeInfo subnode = node.getLabeledBy();

      if (subnode != null) {
        add(sb, "lbd", getText(subnode));
        subnode.recycle();
        subnode = null;
      }
    }

    {
      Rect location = new Rect();
      node.getBoundsInScreen(location);
      add(sb, location.toShortString());
    }

    add(sb, "obj", node.getClassName());
    add(sb, "pkg", node.getPackageName());
    add(sb, "win", node.getWindowId());

    if (ApplicationUtilities.haveJellyBeanMR2) {
      add(sb, "vrn", node.getViewIdResourceName());
    }

    if (ApplicationUtilities.haveKitkat) {
      Bundle extras = node.getExtras();

      if (extras != null) {
        if (extras.size() > 0) {
          add(sb, "extras: ");
          sb.append(extras.toString());
        }
      }
    }

    return sb.toString();
  }

  private static void log (AccessibilityNodeInfo node, String name, boolean descend) {
    log(name, toString(node));

    if (descend) {
      int childCount = node.getChildCount();

      for (int childIndex=0; childIndex<childCount; childIndex+=1) {
        AccessibilityNodeInfo child = node.getChild(childIndex);

        if (child != null) {
          log(child, (name + "." + childIndex), true);
          child.recycle();
          child = null;
        }
      }
    }
  }

  public static void log (AccessibilityNodeInfo root) {
    log("begin node tree");
    log(root, "root", true);
    log("end node tree");
  }

  private static final Map<Integer, String> windowTypeNames =
               new HashMap<Integer, String>()
  {
    {
      put(AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY, "acc");
      put(AccessibilityWindowInfo.TYPE_APPLICATION, "app");
      put(AccessibilityWindowInfo.TYPE_INPUT_METHOD, "ime");
      put(AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER, "ssd");
      put(AccessibilityWindowInfo.TYPE_SYSTEM, "sys");
    }
  };

  public static String toString (AccessibilityWindowInfo window) {
    StringBuilder sb = new StringBuilder();
    add(sb, "id", window.getId());

    if (ApplicationUtilities.haveNougat) {
      CharSequence title = window.getTitle();

      if (title != null) {
        if (title.length() > 0) {
          sb.append(' ');
          sb.append('"');
          sb.append(title);
          sb.append('"');
        }
      }
    }

    {
      AccessibilityWindowInfo parent = window.getParent();

      if (parent != null) {
        parent.recycle();
        parent = null;
      } else {
        add(sb, "root");
      }
    }

    {
      int count = window.getChildCount();
      if (count > 0) add(sb, "cld", count);
    }

    add(sb, "type", window.getType(), windowTypeNames);
    add(sb, "layer", window.getLayer());
    add(sb, window.isActive(), "act");
    add(sb, window.isFocused(), "ifd");
    add(sb, window.isAccessibilityFocused(), "afd");

    if (ApplicationUtilities.haveOreo) {
      add(sb, window.isInPictureInPictureMode(), "pip");
    }

    {
      Rect location = new Rect();
      window.getBoundsInScreen(location);
      add(sb, location.toShortString());
    }

    return sb.toString();
  }

  private static void log (AccessibilityWindowInfo window, String name, boolean descend, boolean nodes) {
    log(name, toString(window));

    if (nodes) {
      AccessibilityNodeInfo root = window.getRoot();

      if (root != null) {
        log(root);
        root.recycle();
        root = null;
      }
    }

    if (descend) {
      int childCount = window.getChildCount();

      for (int childIndex=0; childIndex<childCount; childIndex+=1) {
        AccessibilityWindowInfo child = window.getChild(childIndex);

        if (child != null) {
          log(child, (name + '.' + childIndex), true, nodes);
          child.recycle();
          child = null;
        }
      }
    }
  }

  public static void log () {
    log("begin screen log");

    if (ApplicationUtilities.haveLollipop) {
      int index = 0;

      for (AccessibilityWindowInfo window : ScreenUtilities.getWindows()) {
        log(window, ("window." + index), true, true);
        index += 1;
      }
    } else if (ApplicationUtilities.haveJellyBean) {
      AccessibilityNodeInfo root = ScreenUtilities.getRootNode();

      if (root != null) {
        log(root);
        root.recycle();
        root = null;
      }
    }

    log("end screen log");
  }
}
