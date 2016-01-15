/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.importview.views;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.wizards.ImportMavenProjectsJob;
import org.eclipse.m2e.e4.importview.MavenE4ImportViewPlugin;
import org.eclipse.m2e.e4.importview.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

/**
 * This view can be used to import Maven Projects into Eclipse Workspace.
 * 
 * @author Nikolaus Winter, comdirect bank AG
 */
@SuppressWarnings("restriction")
public class ProjectImportView extends ViewPart {

   public static final String ID = "org.eclipse.m2e.importview.views.ProjectImportView";

   private String rootDirectory;
   private List<MavenProjectInfo> projectsToImport = new ArrayList<>();
   private List<String> savedRootDirectories;

   // UI Elements
   private Combo rootDirectoryCombo;
   private TreeViewer projectTreeViewer;
   private Text filterText;
   private ListViewer projectImportListViewer;
   private Button removeEclipseFilesCheckbox;

   @Override
   public void createPartControl(final Composite parent) {

      parent.setLayout(new GridLayout(3, false));

      Composite left = new Composite(parent, SWT.NONE);
      left.setLayout(new GridLayout(4, false));
      GridData leftCompositeLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
      left.setLayoutData(leftCompositeLayoutData);

      Composite center = new Composite(parent, SWT.NONE);
      center.setLayout(new GridLayout(1, false));

      Composite right = new Composite(parent, SWT.NONE);
      right.setLayout(new GridLayout(2, true));
      GridData rightCompositeLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
      right.setLayoutData(rightCompositeLayoutData);

      final Label selectRootDirectoryLabel = new Label(left, SWT.NONE);
      selectRootDirectoryLabel.setLayoutData(new GridData());
      selectRootDirectoryLabel.setText(Messages.labelRootDirectory);

      rootDirectoryCombo = new Combo(left, SWT.READ_ONLY);
      rootDirectoryCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      rootDirectoryCombo.addSelectionListener(new RootDirectoryComboSelectionHandler());
      if (savedRootDirectories != null && !savedRootDirectories.isEmpty()) {
         for (String savedRootDirectory : savedRootDirectories) {
            rootDirectoryCombo.add(savedRootDirectory);
         }
      }

      final Button browseButton = new Button(left, SWT.NONE);
      browseButton.setText(Messages.buttonBrowseRootDirectory);
      browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      browseButton.addSelectionListener(new BrowseForDirectoryHandler(parent));

      final Button reloadButton = new Button(left, SWT.NONE);
      reloadButton.setImage(MavenE4ImportViewPlugin.getDefault().getImageRegistry().get(MavenE4ImportViewPlugin.ICON_RELOAD));
      reloadButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      reloadButton.addSelectionListener(new ReloadRepoHandler());

      final Label filterLabel = new Label(left, SWT.NONE);
      filterLabel.setLayoutData(new GridData());
      filterLabel.setText(Messages.labelFilterProjects);

      filterText = new Text(left, SWT.BORDER);
      filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
      filterText.addModifyListener(new FilterChangedHandler());

      final Label projectsLabel = new Label(left, SWT.NONE);
      projectsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
      projectsLabel.setText(Messages.labelProjectTreeViewer);

      projectTreeViewer = new TreeViewer(left, SWT.BORDER | SWT.MULTI);
      projectTreeViewer.setContentProvider(new ProjectSelectionTreeContentProvider());
      projectTreeViewer.setLabelProvider(new ProjectSelectionLabelProvider());
      projectTreeViewer.setComparator(new ProjectSelectionViewerComparator());
      projectTreeViewer.addDoubleClickListener(new SelectProjectByDoubleClickHandler());

      final Tree projectTree = projectTreeViewer.getTree();
      GridData projectTreeData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
      projectTree.setLayoutData(projectTreeData);

      final Button addAllButton = new Button(center, SWT.NONE);
      addAllButton.setImage(MavenE4ImportViewPlugin.getDefault().getImageRegistry().get(MavenE4ImportViewPlugin.ICON_ARROW_RIGHT));
      addAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      addAllButton.addSelectionListener(new AddAllSelectedProjectsToImportListHandler());

      final Button removeAllButton = new Button(center, SWT.NONE);
      removeAllButton.setImage(MavenE4ImportViewPlugin.getDefault().getImageRegistry().get(MavenE4ImportViewPlugin.ICON_ARROW_LEFT));
      removeAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      removeAllButton.addSelectionListener(new RemoveAllSelectedProjectsFromImportListHandler());

      final Label projectImportLabel = new Label(right, SWT.NONE);
      projectImportLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
      projectImportLabel.setText(Messages.labelProjectImportList);

      projectImportListViewer = new ListViewer(right, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
      // TODO: Prüfen, ob der Content Provider nicht ggf. zu speziell ist???
      projectImportListViewer.setContentProvider(new ProjectSelectionTreeContentProvider());
      projectImportListViewer.setLabelProvider(new ProjectSelectionLabelProvider());
      projectImportListViewer.setInput(this.projectsToImport);
      projectImportListViewer.addDoubleClickListener(new DeselectProjectByDoubleClickHandler());

      final org.eclipse.swt.widgets.List projectList = projectImportListViewer.getList();
      GridData projectListData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
      projectList.setLayoutData(projectListData);

      final Button clearProjectListButton = new Button(right, SWT.NONE);
      clearProjectListButton.setText(Messages.buttonClearProjectList);
      clearProjectListButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      clearProjectListButton.addSelectionListener(new ClearProjectsToImportListHandler());

      final Button importButton = new Button(right, SWT.NONE);
      importButton.setText(Messages.buttonImportProjects);
      importButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      importButton.addSelectionListener(new ImportProjectsHandler());

      removeEclipseFilesCheckbox = new Button(right, SWT.CHECK);
      removeEclipseFilesCheckbox.setText(Messages.labelRemoveEclipseFiles);
      removeEclipseFilesCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
      // FIXME: remember last state
      removeEclipseFilesCheckbox.setSelection(true);
   }

   @Override
   public void setFocus() {
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
    * Adds all selected {@link MavenProjectInfo} to the list to import.
    */
   private void addAllSelectedProjectsToImportList() {
      ITreeSelection selection = (ITreeSelection) projectTreeViewer.getSelection();
      Iterator<MavenProjectInfo> iterator = selection.iterator();
      while (iterator.hasNext()) {
         MavenProjectInfo projectInfo = iterator.next();
         addProjectToImportList(projectInfo);
      }
      projectTreeViewer.setSelection(null);
      projectTreeViewer.refresh();
      projectImportListViewer.refresh();
   }

   /**
    * Removes all selected {@link MavenProjectInfo} from the list to import.
    */
   private void removeAllSelectedProjectsFromImportList() {
      IStructuredSelection selection = (IStructuredSelection) projectImportListViewer.getSelection();
      Iterator<MavenProjectInfo> iterator = selection.iterator();
      while (iterator.hasNext()) {
         MavenProjectInfo projectInfo = iterator.next();
         removeProjectFromImportList(projectInfo);
      }
      projectImportListViewer.setSelection(null);
      projectImportListViewer.refresh();
      projectTreeViewer.refresh();
   }

   /**
    * Adds given {@link MavenProjectInfo} to the list to import.
    * 
    * @param projectInfo Maven project to add
    */
   private void addProjectToImportList(MavenProjectInfo projectInfo) {
      if (!projectsToImport.contains(projectInfo)) {
         projectsToImport.add(projectInfo);
      }
   }

   /**
    * Removes given {@link MavenProjectInfo} from the list to import.
    * 
    * @param projectInfo Maven project to add
    */
   private void removeProjectFromImportList(MavenProjectInfo projectInfo) {
      if (projectsToImport.contains(projectInfo)) {
         projectsToImport.remove(projectInfo);
      }
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
         dialog.setText(Messages.selectRootDirectoryDialogText);
         dialog.setMessage(Messages.selectRootDirectoryDialogMessage);
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
    * Handles Event "Reload"
    * 
    * @author Nikolaus Winter, comdirect bank AG
    */
   private final class ReloadRepoHandler extends SelectionAdapter {
      public void widgetSelected(SelectionEvent e) {
         String rootDirectory = ProjectImportView.this.rootDirectory;
         if (rootDirectory != null) {
            boolean nonEmptyListLoaded = loadProjectSelectionList(rootDirectory);
            if (!nonEmptyListLoaded) {
               // TODO: inform user (msg)
               return;
            }
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

   /**
    * Handles Event "Filter text changed"
    * 
    * @author Nikolaus Winter, comdirect bank AG
    */
   private final class FilterChangedHandler implements ModifyListener {

      @Override
      public void modifyText(ModifyEvent e) {
         final String newText = filterText.getText();
         if (newText.trim().length() == 0) {
            projectTreeViewer.resetFilters();
            projectTreeViewer.expandAll();
            return;
         }
         // FIXME: should not be case sensitive!
         projectTreeViewer.setFilters(new ViewerFilter[] { new MavenProjectInfoFilter(newText) });
         projectTreeViewer.expandAll();
      }
   }

   /**
    * Handles Event "Add all selected projects to import list"
    * 
    * @author Nikolaus Winter, comdirect bank AG
    */
   private final class AddAllSelectedProjectsToImportListHandler extends SelectionAdapter {
      @SuppressWarnings("unchecked")
      public void widgetSelected(SelectionEvent event) {
         addAllSelectedProjectsToImportList();
      }
   }

   /**
    * Handles Event "Remove all selected projects to import list"
    * 
    * @author Nikolaus Winter, comdirect bank AG
    */
   private final class RemoveAllSelectedProjectsFromImportListHandler extends SelectionAdapter {
      @SuppressWarnings("unchecked")
      public void widgetSelected(SelectionEvent event) {
         removeAllSelectedProjectsFromImportList();
      }
   }

   // TODO: QA
   /**
    * Handles Event "Import all projects of import list"
    * 
    * @author Nikolaus Winter, comdirect bank AG
    */
   private final class ImportProjectsHandler extends SelectionAdapter {
      public void widgetSelected(SelectionEvent event) {
         List<MavenProjectInfo> projectsToImport2 = getProjectsToImport();
         if (removeEclipseFilesCheckbox.getSelection()) {
            for (Iterator iterator = projectsToImport2.iterator(); iterator.hasNext();) {
               MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) iterator.next();
               removeEclipseFiles(mavenProjectInfo.getPomFile().getParentFile());
            }
         }
         ImportMavenProjectsJob job = new ImportMavenProjectsJob(projectsToImport2, new ArrayList<IWorkingSet>(), new ProjectImportConfiguration());
         job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
         job.schedule();
         projectsToImport.clear();
         projectImportListViewer.refresh();
      }

      private List<MavenProjectInfo> getProjectsToImport() {
         List<MavenProjectInfo> projectList = new ArrayList<>();
         Set<String> projectsInWorkspace = getGroupIdAndArtifactIdOfAllProjectsInWorkspace();
         Iterator<MavenProjectInfo> iterator = projectsToImport.iterator();
         MavenProjectInfo mavenProjectInfo;
         Model mavenModel;
         while (iterator.hasNext()) {
            mavenProjectInfo = (MavenProjectInfo) iterator.next();
            mavenModel = mavenProjectInfo.getModel();
            String groupId = getGroupId(mavenModel);
            if (!projectsInWorkspace.contains(String.format("%s:%s", groupId, mavenModel.getArtifactId()))) {
               projectList.add(mavenProjectInfo);
            } else {
            }
         }
         return projectList;
      }

      private String getGroupId(Model mavenModel) {
         if (mavenModel.getGroupId() != null) {
            return mavenModel.getGroupId();
         }
         if (mavenModel.getParent() != null) {
            return mavenModel.getParent().getGroupId();
         }
         return null;
      }

      private Set<String> getGroupIdAndArtifactIdOfAllProjectsInWorkspace() {
         HashSet<String> result = new HashSet<>();
         IMavenProjectFacade[] mavenProjectFacades = MavenPlugin.getMavenProjectRegistry().getProjects();
         for (int i = 0; i < mavenProjectFacades.length; i++) {
            result.add(String.format("%s:%s", mavenProjectFacades[i].getArtifactKey().getGroupId(), mavenProjectFacades[i].getArtifactKey().getArtifactId()));
         }
         return result;
      }

      private void removeEclipseFiles(File projectFolder) {
         new File(projectFolder, ".project").delete();
         new File(projectFolder, ".classpath").delete();
         File settingsDirectory = new File(projectFolder, ".settings");
         if (settingsDirectory.isDirectory()) {
            File[] settingsFiles = settingsDirectory.listFiles();
            for (int i = 0; i < settingsFiles.length; i++) {
               settingsFiles[i].delete();
            }
            settingsDirectory.delete();
         }
      }

   }

   /**
    * Handles Event "Double Click on Project in Tree"
    * 
    * @author Nikolaus Winter, comdirect bank AG
    *
    */
   private final class SelectProjectByDoubleClickHandler implements IDoubleClickListener {

      @Override
      public void doubleClick(DoubleClickEvent event) {
         addAllSelectedProjectsToImportList();
      }

   }

   /**
    * Handles Event "Double Click on List of Projects to import"
    * 
    * @author Nikolaus Winter, comdirect bank AG
    *
    */
   private final class DeselectProjectByDoubleClickHandler implements IDoubleClickListener {

      @Override
      public void doubleClick(DoubleClickEvent event) {
         removeAllSelectedProjectsFromImportList();
      }

   }

   /**
    * Handles Event "Clear projects to import list"
    * 
    * @author Nikolaus Winter, comdirect bank AG
    */
   private final class ClearProjectsToImportListHandler extends SelectionAdapter {
      public void widgetSelected(SelectionEvent e) {
         projectsToImport.clear();
         projectImportListViewer.refresh();
      }
   }

   @Override
   public void init(IViewSite site, IMemento memento) throws PartInitException {
      super.init(site, memento);
      if (memento == null) {
         return;
      }
      String rootDirectories = memento.getTextData();
      if (rootDirectories == null) {
         return;
      }
      StringTokenizer tokenizer = new StringTokenizer(rootDirectories, "#");
      if (tokenizer.countTokens() == 0) {
         return;
      }
      savedRootDirectories = new ArrayList<>();
      while (tokenizer.hasMoreTokens()) {
         savedRootDirectories.add(tokenizer.nextToken());
      }

   }

   @Override
   public void saveState(IMemento memento) {
      String[] rootDirectories = rootDirectoryCombo.getItems();
      StringBuffer textData = new StringBuffer();
      for (int i = 0; i < rootDirectories.length; i++) {
         textData.append("#");
         textData.append(rootDirectories[i]);
      }
      memento.putTextData(textData.toString());
      super.saveState(memento);
   }

}
