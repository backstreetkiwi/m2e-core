/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Fred Bricon (Red Hat, Inc.) - auto update project configuration
 *******************************************************************************/

package org.eclipse.m2e.e4.importview;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class MavenE4ImportViewPlugin extends AbstractUIPlugin {

   public static final String PLUGIN_ID = "org.eclipse.m2e.e4.importview";

   public static final String ICON_ARROW_RIGHT = "icons/arrow_right.gif";
   public static final String ICON_ARROW_LEFT = "icons/arrow_left.gif";
   public static final String ICON_RELOAD = "icons/reload.gif";
   public static final String ICON_EXPORT = "icons/export.gif";
   public static final String ICON_IMPORT = "icons/import.gif";

   private static MavenE4ImportViewPlugin instance;

   public MavenE4ImportViewPlugin() {
      MavenE4ImportViewPlugin.instance = this;
   }

   @Override
   public void start(BundleContext context) throws Exception {
      super.start(context);
      getImageRegistry().put(ICON_ARROW_RIGHT, imageDescriptorFromPlugin(PLUGIN_ID, ICON_ARROW_RIGHT));
      getImageRegistry().put(ICON_ARROW_LEFT, imageDescriptorFromPlugin(PLUGIN_ID, ICON_ARROW_LEFT));
      getImageRegistry().put(ICON_RELOAD, imageDescriptorFromPlugin(PLUGIN_ID, ICON_RELOAD));
      getImageRegistry().put(ICON_EXPORT, imageDescriptorFromPlugin(PLUGIN_ID, ICON_EXPORT));
      getImageRegistry().put(ICON_IMPORT, imageDescriptorFromPlugin(PLUGIN_ID, ICON_IMPORT));
   }

   @Override
   public void stop(BundleContext context) throws Exception {
      super.stop(context);
   }

   /**
    * Logs given message with given status.
    * 
    * @param status status of message
    * @param message text of message
    * @see IStatus
    */
   public void log(int status, String message) {
      this.getLog().log(new Status(status, PLUGIN_ID, message));
   }

   /**
    * Logs given message with given status and throwable.
    * 
    * @param status status of message
    * @param message text of message
    * @param throwable Throwable
    * @see IStatus
    */
   public void log(int status, String message, Throwable throwable) {
      this.getLog().log(new Status(status, PLUGIN_ID, message, throwable));
   }

   public static MavenE4ImportViewPlugin getDefault() {
      return instance;
   }

}
