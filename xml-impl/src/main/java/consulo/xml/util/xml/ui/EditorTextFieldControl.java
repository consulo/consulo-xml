/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.highlighting.DomElementAnnotationsManager;
import consulo.xml.util.xml.highlighting.DomElementProblemDescriptor;
import consulo.xml.util.xml.highlighting.DomElementsProblemsHolder;
import consulo.application.ApplicationManager;
import consulo.application.WriteAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.MarkupModel;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.undoRedo.CommandProcessor;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
public abstract class EditorTextFieldControl<T extends JComponent> extends BaseModifiableControl<T, String>
{
	private static final JTextField J_TEXT_FIELD = new JTextField()
	{
		public void addNotify()
		{
			throw new UnsupportedOperationException("Shouldn't be shown");
		}

		public void setVisible(boolean aFlag)
		{
			throw new UnsupportedOperationException("Shouldn't be shown");
		}
	};
	private final boolean myCommitOnEveryChange;
	private final DocumentListener myListener = new DocumentAdapter()
	{
		public void documentChanged(DocumentEvent e)
		{
			setModified();
			if(myCommitOnEveryChange)
			{
				commit();
			}
		}
	};

	protected EditorTextFieldControl(final DomWrapper<String> domWrapper, final boolean commitOnEveryChange)
	{
		super(domWrapper);
		myCommitOnEveryChange = commitOnEveryChange;
	}


	protected EditorTextFieldControl(final DomWrapper<String> domWrapper)
	{
		this(domWrapper, false);
	}

	protected abstract EditorTextField getEditorTextField(@Nonnull T component);

	protected void doReset()
	{
		final EditorTextField textField = getEditorTextField(getComponent());
		textField.getDocument().removeDocumentListener(myListener);
		super.doReset();
		textField.getDocument().addDocumentListener(myListener);
	}

	protected JComponent getComponentToListenFocusLost(final T component)
	{
		return getEditorTextField(getComponent());
	}

	protected JComponent getHighlightedComponent(final T component)
	{
		return J_TEXT_FIELD;
	}

	protected T createMainComponent(T boundedComponent)
	{
		final Project project = getProject();
		boundedComponent = createMainComponent(boundedComponent, project);

		final EditorTextField editorTextField = getEditorTextField(boundedComponent);
		editorTextField.setSupplementary(true);
		editorTextField.getDocument().addDocumentListener(myListener);
		return boundedComponent;
	}

	protected abstract T createMainComponent(T boundedComponent, Project project);

	@Nonnull
	protected String getValue()
	{
		return getEditorTextField(getComponent()).getText();
	}

	protected void setValue(final String value)
	{
		CommandProcessor.getInstance().runUndoTransparentAction(new Runnable()
		{
			public void run()
			{
				WriteAction.run(() -> {
					final T component = getComponent();
					final Document document = getEditorTextField(component).getDocument();
					document.replaceString(0, document.getTextLength(), value == null ? "" : value);
				});
			}
		});
	}

	protected void updateComponent()
	{
		final DomElement domElement = getDomElement();
		if(domElement == null || !domElement.isValid())
		{
			return;
		}

		final EditorTextField textField = getEditorTextField(getComponent());
		final Project project = getProject();
		ApplicationManager.getApplication().invokeLater(new Runnable()
		{
			public void run()
			{
				if(!project.isOpen())
				{
					return;
				}
				if(!getDomWrapper().isValid())
				{
					return;
				}

				final DomElement domElement = getDomElement();
				if(domElement == null || !domElement.isValid())
				{
					return;
				}

				final DomElementAnnotationsManager manager = DomElementAnnotationsManager.getInstance(project);
				final DomElementsProblemsHolder holder = manager.getCachedProblemHolder(domElement);
				final List<DomElementProblemDescriptor> errorProblems = holder.getProblems(domElement);
				final List<DomElementProblemDescriptor> warningProblems = new ArrayList<DomElementProblemDescriptor>(holder.getProblems(domElement, true, HighlightSeverity.WARNING));
				warningProblems.removeAll(errorProblems);

				Color background = getDefaultBackground();
				if(errorProblems.size() > 0 && textField.getText().trim().length() == 0)
				{
					background = getErrorBackground();
				}
				else if(warningProblems.size() > 0)
				{
					background = getWarningBackground();
				}

				final Editor editor = textField.getEditor();
				if(editor != null)
				{
					final MarkupModel markupModel = editor.getMarkupModel();
					markupModel.removeAllHighlighters();
					if(!errorProblems.isEmpty() && editor.getDocument().getLineCount() > 0)
					{
						final TextAttributes attributes = TextAttributesUtil.toTextAttributes(SimpleTextAttributes.ERROR_ATTRIBUTES);
						attributes.setEffectType(EffectType.WAVE_UNDERSCORE);
						attributes.setEffectColor(attributes.getForegroundColor());
						markupModel.addLineHighlighter(0, 0, attributes);
						editor.getContentComponent().setToolTipText(errorProblems.get(0).getDescriptionTemplate());
					}
				}

				textField.setBackground(background);
			}
		});

	}

	public boolean canNavigate(final DomElement element)
	{
		return getDomElement().equals(element);
	}

	public void navigate(final DomElement element)
	{
		final EditorTextField field = getEditorTextField(getComponent());
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				field.requestFocus();
				field.selectAll();
			}
		});
	}
}
