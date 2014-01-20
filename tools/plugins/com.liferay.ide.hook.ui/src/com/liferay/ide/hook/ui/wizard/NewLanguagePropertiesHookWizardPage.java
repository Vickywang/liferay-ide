/*******************************************************************************
 * Copyright (c) 2000-2014 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 *******************************************************************************/

package com.liferay.ide.hook.ui.wizard;

import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.hook.core.operation.INewHookDataModelProperties;
import com.liferay.ide.hook.ui.HookUI;
import com.liferay.ide.project.core.util.ProjectUtil;
import com.liferay.ide.ui.util.SWTUtil;
import com.liferay.ide.ui.wizard.StringArrayTableWizardSection;
import com.liferay.ide.ui.wizard.StringArrayTableWizardSectionCallback;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jst.j2ee.internal.plugin.J2EEUIMessages;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.wst.common.componentcore.internal.operation.IArtifactEditOperationDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.internal.datamodel.ui.DataModelWizardPage;

/**
 * @author Greg Amerson
 */
@SuppressWarnings( "restriction" )
public class NewLanguagePropertiesHookWizardPage extends DataModelWizardPage implements INewHookDataModelProperties
{

    protected Text contentFolder;

    protected StringArrayTableWizardSection languagePropertiesSection;

    public NewLanguagePropertiesHookWizardPage( IDataModel dataModel, String pageName )
    {
        super( dataModel, pageName, Msgs.createLanguageProperties, HookUI.imageDescriptorFromPlugin(
            HookUI.PLUGIN_ID, "/icons/wizban/hook_wiz.png" ) ); //$NON-NLS-1$

        setDescription( Msgs.createNewLanguagePropertiesFiles );
    }

    protected void createContentFolderGroup( Composite topComposite )
    {
        Composite composite = SWTUtil.createTopComposite( topComposite, 3 );

        GridLayout gl = new GridLayout( 3, false );
        gl.marginLeft = 5;

        composite.setLayout( gl );
        composite.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 3, 1 ) );

        SWTUtil.createLabel( composite, SWT.LEAD, Msgs.contentFolder, 1 );

        contentFolder = SWTUtil.createText( composite, 1 );
        this.synchHelper.synchText( contentFolder, CONTENT_FOLDER, null );

        Button iconFileBrowse = SWTUtil.createPushButton( composite, Msgs.browse, null );
        iconFileBrowse.addSelectionListener( new SelectionAdapter()
        {

            @Override
            public void widgetSelected( SelectionEvent e )
            {
                handleFileBrowseButton( NewLanguagePropertiesHookWizardPage.this.contentFolder );
            }
        } );
    }

    protected void createLanguagePropertiesGroup( Composite parent )
    {
        Composite composite = SWTUtil.createTopComposite( parent, 2 );
        composite.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 3, 1 ) );

        languagePropertiesSection =
            new StringArrayTableWizardSection(
                composite, Msgs.languagePropertyFiles, Msgs.languagePropertyFileTitle, Msgs.add, Msgs.edit, Msgs.remove,
                new String[] { Msgs.add }, new String[] { Msgs.languagePropertyFileLabel }, null, getDataModel(),
                LANGUAGE_PROPERTIES_ITEMS );

        GridData gd = new GridData( SWT.FILL, SWT.CENTER, true, true, 1, 1 );
        gd.heightHint = 175;

        languagePropertiesSection.setLayoutData( gd );
        languagePropertiesSection.setCallback( new StringArrayTableWizardSectionCallback() );
    }

    @Override
    protected Composite createTopLevelComposite( Composite parent )
    {
        Composite topComposite = SWTUtil.createTopComposite( parent, 3 );

        createContentFolderGroup( topComposite );

        createLanguagePropertiesGroup( topComposite );

        return topComposite;
    }

    @Override
    protected void enter()
    {
        super.enter();

        this.synchHelper.synchAllUIWithModel();
    }

    protected ISelectionStatusValidator getContainerDialogSelectionValidator()
    {
        return new ISelectionStatusValidator()
        {

            public IStatus validate( Object[] selection )
            {
                if( selection != null && selection.length > 0 && selection[0] != null &&
                    !( selection[0] instanceof IProject ) && !( selection[0] instanceof IFile ) )
                {
                    return Status.OK_STATUS;
                }

                return HookUI.createErrorStatus( Msgs.chooseValidFolder );
            }
        };
    }

    protected ViewerFilter getContainerDialogViewerFilter()
    {
        return new ViewerFilter()
        {
            @SuppressWarnings( "deprecation" )
            public boolean select( Viewer viewer, Object parent, Object element )
            {
                if( element instanceof IProject )
                {
                    IProject project = (IProject) element;

                    return project.getName().equals(
                        model.getProperty( IArtifactEditOperationDataModelProperties.PROJECT_NAME ) );
                }
                else if( element instanceof IFolder )
                {
                    IFolder folder = (IFolder) element;

                    // only show source folders
                    IProject project =
                        CoreUtil.getProject( model.getStringProperty( IArtifactEditOperationDataModelProperties.PROJECT_NAME ) );

                    IPackageFragmentRoot[] sourceFolders = J2EEProjectUtilities.getSourceContainers( project );

                    for( int i = 0; i < sourceFolders.length; i++ )
                    {
                        if( sourceFolders[i].getResource() != null && sourceFolders[i].getResource().equals( folder ) )
                        {
                            return true;
                        }
                        else if( ProjectUtil.isParent( folder, sourceFolders[i].getResource() ) )
                        {
                            return true;
                        }
                    }
                }

                return false;
            }
        };
    }

    @Override
    protected String[] getValidationPropertyNames()
    {
        return new String[] { CONTENT_FOLDER, LANGUAGE_PROPERTIES_ITEMS };
    }

    protected void handleFileBrowseButton( final Text text )
    {
        ISelectionStatusValidator validator = getContainerDialogSelectionValidator();

        ViewerFilter filter = getContainerDialogViewerFilter();

        ITreeContentProvider contentProvider = new WorkbenchContentProvider();

        ILabelProvider labelProvider =
            new DecoratingLabelProvider(
                new WorkbenchLabelProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator() );

        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog( getShell(), labelProvider, contentProvider );
        dialog.setValidator( validator );
        dialog.setTitle( J2EEUIMessages.CONTAINER_SELECTION_DIALOG_TITLE );
        dialog.setMessage( J2EEUIMessages.CONTAINER_SELECTION_DIALOG_DESC );
        dialog.addFilter( filter );
        dialog.setInput( CoreUtil.getWorkspaceRoot() );

        if( dialog.open() == Window.OK )
        {
            Object element = dialog.getFirstResult();

            try
            {
                if( element instanceof IFolder )
                {
                    IFolder folder = (IFolder) element;

                    if( folder.equals( CoreUtil.getFirstSrcFolder( getDataModel().getStringProperty( PROJECT_NAME ) ) ) )
                    {
                        folder = folder.getFolder( "content" ); //$NON-NLS-1$
                    }

                    text.setText( folder.getFullPath().toPortableString() );
                }
            }
            catch( Exception ex )
            {
                // Do nothing
            }

        }
    }

    private static class Msgs extends NLS
    {
        public static String add;
        public static String browse;
        public static String chooseValidFolder;
        public static String contentFolder;
        public static String createLanguageProperties;
        public static String createNewLanguagePropertiesFiles;
        public static String edit;
        public static String languagePropertyFileLabel;
        public static String languagePropertyFiles;
        public static String languagePropertyFileTitle;
        public static String remove;

        static
        {
            initializeMessages( NewLanguagePropertiesHookWizardPage.class.getName(), Msgs.class );
        }
    }
}
