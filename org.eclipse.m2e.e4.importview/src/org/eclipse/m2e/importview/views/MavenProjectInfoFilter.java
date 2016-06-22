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

package org.eclipse.m2e.importview.views;

import java.util.Iterator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.m2e.core.project.MavenProjectInfo;

/**
 * Filters Maven Project Info by contained text
 * 
 * @author Nikolaus Winter, comdirect bank AG
 */
final class MavenProjectInfoFilter extends ViewerFilter {

   private final String filterText;

   MavenProjectInfoFilter(String filterText) {
      this.filterText = filterText;
   }

   @Override
   public boolean select(Viewer viewer, Object parentElement, Object element) {
      if (!(element instanceof MavenProjectInfo)) {
         return false;
      }
      MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) element;
      return select(mavenProjectInfo);
   }

   private boolean select(MavenProjectInfo mavenProjectInfo) {

      // leaf
      if (mavenProjectInfo.getProjects().isEmpty()) {
         return mavenProjectInfo.getModel().getArtifactId().contains(filterText);
      }

      // node matching by name
      if (mavenProjectInfo.getModel().getArtifactId().contains(filterText)) {
         return true;
      }

      // non-matching node with children
      Iterator<MavenProjectInfo> iterator = mavenProjectInfo.getProjects().iterator();
      boolean matchingChildFound = false;
      while (!matchingChildFound && iterator.hasNext()) {
         matchingChildFound = select(iterator.next());
      }
      return matchingChildFound;
   }

}