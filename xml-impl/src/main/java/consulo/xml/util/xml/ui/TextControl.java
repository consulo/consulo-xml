/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.xml.util.xml.ui;

import consulo.codeEditor.EditorEx;
import consulo.document.Document;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.editor.ui.awt.ReferenceEditorWithBrowseButton;
import consulo.language.plain.PlainTextFileType;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFileFactory;
import consulo.project.Project;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.awt.DialogBuilder;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Function;

/**
 * @author peter
 */
public class TextControl extends EditorTextFieldControl<TextPanel>
{
	public TextControl(final DomWrapper<String> domWrapper)
	{
		super(domWrapper);
	}

	public TextControl(final DomWrapper<String> domWrapper, final boolean commitOnEveryChange)
	{
		super(domWrapper, commitOnEveryChange);
	}

	@Override
	protected EditorTextField getEditorTextField(@Nonnull final TextPanel panel)
	{
		final Component component = panel.getComponent(0);
		if(component instanceof ReferenceEditorWithBrowseButton)
		{
			return ((ReferenceEditorWithBrowseButton) component).getEditorTextField();
		}
		return (EditorTextField) component;
	}

	@Override
	protected TextPanel createMainComponent(TextPanel boundedComponent, final Project project)
	{
		if(boundedComponent == null)
		{
			boundedComponent = new TextPanel();
		}
		boundedComponent.removeAll();
		final Function<String, Document> factory = new Function<String, Document>()
		{
			@Override
			public Document apply(final String s)
			{
				return PsiDocumentManager.getInstance(project).getDocument(PsiFileFactory.getInstance(project).createFileFromText("a.txt",
						PlainTextLanguage.INSTANCE, "", true, false));
			}
		};
		final TextPanel boundedComponent1 = boundedComponent;
		final EditorTextField editorTextField = new EditorTextField(factory.apply(""), project, PlainTextFileType.INSTANCE)
		{
			@Override
			protected EditorEx createEditor()
			{
				final EditorEx editor = super.createEditor();
				return boundedComponent1 instanceof MultiLineTextPanel ? makeBigEditor(editor, ((MultiLineTextPanel) boundedComponent1).getRowCount
						()) : editor;
			}
		};
		editorTextField.setOneLineMode(false);

		if(boundedComponent instanceof BigTextPanel)
		{
			final ReferenceEditorWithBrowseButton editor = new ReferenceEditorWithBrowseButton(null, editorTextField, factory);
			boundedComponent.add(editor);
			editor.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					EditorTextField textArea = new EditorTextField(editorTextField.getDocument(), project, PlainTextFileType.INSTANCE)
					{
						@Override
						protected EditorEx createEditor()
						{
							final EditorEx editor = super.createEditor();
							editor.setEmbeddedIntoDialogWrapper(true);
							return makeBigEditor(editor, 5);
						}
					};
					textArea.setOneLineMode(false);

					DialogBuilder builder = new DialogBuilder(project);
					builder.setDimensionServiceKey("TextControl");
					builder.setCenterPanel(textArea);
					builder.setPreferredFocusComponent(textArea);
					builder.setTitle(UIBundle.message("big.text.control.window.title"));
					builder.addCloseButton();
					builder.show();
				}
			});
			return boundedComponent;
		}

		boundedComponent.add(editorTextField);
		return boundedComponent;
	}

	private static EditorEx makeBigEditor(final EditorEx editor, int rowCount)
	{
		editor.setVerticalScrollbarVisible(true);
		final JTextArea area = new JTextArea(10, 50);
		area.setRows(rowCount);
		editor.getComponent().setPreferredSize(area.getPreferredSize());
		return editor;
	}


}
