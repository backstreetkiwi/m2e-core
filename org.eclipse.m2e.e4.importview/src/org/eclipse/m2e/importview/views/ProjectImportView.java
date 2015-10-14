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
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.e4.importview.MavenE4ImportViewPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

   private String rootDirectory;

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
      rootDirectoryCombo = new Combo(composite, SWT.READ_ONLY);
      rootDirectoryCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      rootDirectoryCombo.setFocus();
      rootDirectoryCombo.addSelectionListener(new RootDirectoryComboSelectionHandler());

      final Button browseButton = new Button(composite, SWT.NONE);
      // FIXME: externalize Strings
      // browseButton.setText(Messages.wizardImportPageBrowse);
      browseButton.setText("Browse...");
      browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      browseButton.addSelectionListener(new BrowseForDirectoryHandler(parent));

      // TODO: use filters
      // projectTreeViewer.setFilters(filters);

      final Label projectsLabel = new Label(composite, SWT.NONE);
      projectsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
      // FIXME: externalize Strings
      // projectsLabel.setText(Messages.wizardImportPageProjects);
      projectsLabel.setText("Projektekram");

      projectTreeViewer = new CheckboxTreeViewer(composite, SWT.BORDER);
      projectTreeViewer.setContentProvider(new ProjectSelectionTreeContentProvider());
      projectTreeViewer.setLabelProvider(new ProjectSelectionLabelProvider());

      final Tree projectTree = projectTreeViewer.getTree();
      GridData projectTreeData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 5);
      projectTreeData.heightHint = 250;
      projectTreeData.widthHint = 500;
      projectTree.setLayoutData(projectTreeData);
   }

   @Override
   public void setFocus() {
      // TODO: rethink which control is focused best
      rootDirectoryCombo.setFocus();
   }

   /**
    * Reads the project(s) from the given root location and fills the project selection tree viewer.
    * 
    * @param location root location of Maven Project Folder
    * 
    * @return Has a non-empty project list been loaded?
    */
   private boolean loadProjectSelectionList(String location) {

      if (location == null || location.length() == 0) {
         return false;
      }

      File workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
      MavenModelManager modelManager = MavenPlugin.getMavenModelManager();
      LocalProjectScanner scanner = new LocalProjectScanner(workspaceRoot, location.trim(), false, modelManager);

      // TODO: show progress to user (no null progress monitor, instead go async)
      try {
         scanner.run(new NullProgressMonitor());
      } catch (InterruptedException e) {
         MavenE4ImportViewPlugin.getDefault().log(IStatus.ERROR, "Scanning of projects interrupted.", e);
         return false;
      }

      // FIXME: Sort adjacent projects alphabetically

      List<MavenProjectInfo> projectList = scanner.getProjects();

      if (projectList == null || projectList.isEmpty()) {
         return false;
      }

      projectTreeViewer.setInput(projectList);
      projectTreeViewer.expandAll();
      return true;
   }

   /**
    * Handles Event "Browse for Directory"
    * 
    * @author Nikolaus Winter, comdirect bank AG
    */
   private final class BrowseForDirectoryHandler extends SelectionAdapter {
      private final Composite parent;

      private BrowseForDirectoryHandler(Composite parent) {
         this.parent = parent;
      }

      public void widgetSelected(SelectionEvent e) {
         DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.NONE);
         // FIXME: externalize Strings
         // dialog.setText(Messages.wizardImportPageSelectRootFolder);
         dialog.setText("Select Root Directory");
         dialog.setMessage("Please select root directory of Maven project(s).");
         String currentRootDirectory = rootDirectoryCombo.getText();
         if (currentRootDirectory.length() == 0) {
            currentRootDirectory = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();
         }
         dialog.setFilterPath(currentRootDirectory);
         String newRootDirectory = dialog.open();
         if (newRootDirectory != null) {
            boolean nonEmptyListLoaded = loadProjectSelectionList(newRootDirectory);
            if (!nonEmptyListLoaded) {
               // TODO: inform user (msg)
               return;
            }
            if (rootDirectoryCombo.indexOf(newRootDirectory) != -1) {
               rootDirectoryCombo.remove(newRootDirectory);
            }
            rootDirectoryCombo.add(newRootDirectory, 0);
            rootDirectoryCombo.setText(newRootDirectory);
            rootDirectory = newRootDirectory;
         }
      }
   }

   /**
    * Handles Event "Item from Root Directory Combo is selected"
    * 
    * @author Nikolaus Winter, comdirect bank AG
    */
   private final class RootDirectoryComboSelectionHandler extends SelectionAdapter {

      @Override
      public void widgetSelected(SelectionEvent e) {
         String selectedRootDirectory = rootDirectoryCombo.getText();
         if (selectedRootDirectory != null && selectedRootDirectory.equals(rootDirectory)) {
            return;
         }
         boolean nonEmptyListLoaded = loadProjectSelectionList(selectedRootDirectory);
         if (!nonEmptyListLoaded) {
            // TODO: inform user (msg)
            return;
         }
         rootDirectory = selectedRootDirectory;
      }

   }

}
