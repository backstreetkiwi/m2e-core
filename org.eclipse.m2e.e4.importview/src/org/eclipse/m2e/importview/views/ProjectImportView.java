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

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.AbstractProjectScanner;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.part.ViewPart;

/**
 * This view can be used to import Maven Projects into Eclipse Workspace.
 * 
 * @author Nikolaus Winter, comdirect bank AG
 */
public class ProjectImportView extends ViewPart {

   public static final String ID = "org.eclipse.m2e.importview.views.ProjectImportView";

   static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

   // UI Elements
   protected Combo rootDirectoryCombo;
   protected CheckboxTreeViewer projectTreeViewer;

   @Override
   public void createPartControl(final Composite parent) {

      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayout(new GridLayout(3, false));

      final Label selectRootDirectoryLabel = new Label(composite, SWT.NONE);
      selectRootDirectoryLabel.setLayoutData(new GridData());
      // FIXME: externalize Strings
      // selectRootDirectoryLabel.setText(Messages.wizardImportPageRoot);
      selectRootDirectoryLabel.setText("Root Directory");

      // TODO: Fill with previously used Repos
      rootDirectoryCombo = new Combo(composite, SWT.NONE);
      rootDirectoryCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      rootDirectoryCombo.setFocus();

      final Button browseButton = new Button(composite, SWT.NONE);
      // FIXME: externalize Strings
      // browseButton.setText(Messages.wizardImportPageBrowse);
      browseButton.setText("Browse...");
      browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

      // TODO: externalize Listener
      browseButton.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.NONE);
            // FIXME: externalize Strings, find better text
            // dialog.setText(Messages.wizardImportPageSelectRootFolder);
            dialog.setText("TODO");
            String path = rootDirectoryCombo.getText();
            if (path.length() == 0) {
               path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();
            }
            dialog.setFilterPath(path);

            String result = dialog.open();
            if (result != null) {
               rootDirectoryCombo.setText(result);

               AbstractProjectScanner<MavenProjectInfo> projectScanner = getProjectScanner(result);

               // FIXME: show progress to user
               try {
                  projectScanner.run(new NullProgressMonitor());
               } catch (InterruptedException e1) {
                  // FIXME: handle interruption properly
                  System.out.println("INTERRUPTED");
                  e1.printStackTrace();
               }

               List<MavenProjectInfo> projects = projectScanner.getProjects();

               // FIXME: Sort adjacent projects alphabetically

               projectTreeViewer.setInput(projects);
               projectTreeViewer.expandAll();

               // TODO: use filters
               // projectTreeViewer.setFilters(filters);

               // TODO: couple scanner to combo box
               // if(rootDirectoryChanged()) {
               // scanProjects();
               // }
            }
         }
      });

      final Label projectsLabel = new Label(composite, SWT.NONE);
      projectsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
      // FIXME: externalize Strings
      // projectsLabel.setText(Messages.wizardImportPageProjects);
      projectsLabel.setText("Projektekram");

      projectTreeViewer = new CheckboxTreeViewer(composite, SWT.BORDER);

      // TODO: externalize contentprovider, format and proofread
      projectTreeViewer.setContentProvider(new ITreeContentProvider() {

         public Object[] getElements(Object element) {
            if (element instanceof List) {
               @SuppressWarnings("unchecked")
               List<MavenProjectInfo> projects = (List<MavenProjectInfo>) element;
               return projects.toArray(new MavenProjectInfo[projects.size()]);
            }
            return EMPTY_OBJECT_ARRAY;
         }

         public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof List) {
               @SuppressWarnings("unchecked")
               List<MavenProjectInfo> projects = (List<MavenProjectInfo>) parentElement;
               return projects.toArray(new MavenProjectInfo[projects.size()]);
            } else if (parentElement instanceof MavenProjectInfo) {
               MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) parentElement;
               Collection<MavenProjectInfo> projects = mavenProjectInfo.getProjects();
               return projects.toArray(new MavenProjectInfo[projects.size()]);
            }
            return EMPTY_OBJECT_ARRAY;
         }

         public Object getParent(Object element) {
            return null;
         }

         public boolean hasChildren(Object parentElement) {
            if (parentElement instanceof List) {
               List<?> projects = (List<?>) parentElement;
               return !projects.isEmpty();
            } else if (parentElement instanceof MavenProjectInfo) {
               MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) parentElement;
               return !mavenProjectInfo.getProjects().isEmpty();
            }
            return false;
         }

         public void dispose() {
         }

         public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
         }
      });

      // TODO: externalize label provider, format and proofread
      projectTreeViewer.setLabelProvider(new LabelProvider() {

         @Override
         public String getText(Object element) {
            // TODO: formatted text (highlighting of differentiation)
            // FIXME: nullsafety
            return ((MavenProjectInfo) element).getModel().getArtifactId();
         }

         @Override
         public Image getImage(Object element) {
            // FIXME: nullsafety
            // FIXME: implement
            return null;
         }
      });

      final Tree projectTree = projectTreeViewer.getTree();
      GridData projectTreeData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 5);
      projectTreeData.heightHint = 250;
      projectTreeData.widthHint = 500;
      projectTree.setLayoutData(projectTreeData);
   }

   // FIXME: comments
   // TODO: check if return type can be specialized in this case...
   protected AbstractProjectScanner<MavenProjectInfo> getProjectScanner(String location) {
      File root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
      MavenModelManager modelManager = MavenPlugin.getMavenModelManager();
      if (location == null || location.length() == 0) {
         return null;
      }
      return new LocalProjectScanner(root, location.trim(), false, modelManager);
   }

   @Override
   public void setFocus() {
      // TODO: rethink which control is focused best
      rootDirectoryCombo.setFocus();
   }
}
