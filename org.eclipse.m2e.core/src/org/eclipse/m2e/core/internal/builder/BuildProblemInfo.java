/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.builder;

import com.google.common.base.Throwables;

import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


class BuildProblemInfo extends MavenProblemInfo {
  public BuildProblemInfo(Throwable error, MojoExecutionKey mojoExecutionKey, SourceLocation markerLocation) {
    super(formatMessage(error, mojoExecutionKey), markerLocation);
  }

  private static String formatMessage(Throwable error, MojoExecutionKey mojoExecutionKey) {
    StringBuilder msg = new StringBuilder(String.valueOf(error.getMessage()));
    if(mojoExecutionKey != null) {
      msg.append(" (").append(mojoExecutionKey.getKeyString()).append(')'); //$NON-NLS-1$ $NON-NLS-2$
    }
    msg.append("\n\n").append(Throwables.getStackTraceAsString(error)); //$NON-NLS-1$
    return msg.toString();
  }
}
