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

package org.eclipse.m2e.e4.service;

import javax.inject.Inject;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.IEclipseContext;


/**
 * MavenImportService
 *
 * @author Nikolaus Winter, comdirect bank AG
 */
public class MavenImportServiceImpl implements MavenImportService {

  public void foo() {
    MavenE4ServicePlugin.getDefault().log(IStatus.INFO, "foo() called");
  }

  /**
   * Registers MavenImportService at {@link IEclipseContext} (E4).
   * 
   * @param context {@link IEclipseContext} (E4)
   */
  @Inject
  public void setEclipseContext(IEclipseContext context) {
    context.set(MavenImportService.class, this);
    MavenE4ServicePlugin.getDefault().log(IStatus.INFO, "Registered MavenImportService");
  }
}
