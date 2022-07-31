// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.xml.codeInsight.editorActions;

import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Caret;
import consulo.codeEditor.CaretAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.lookup.impl.LookupImpl;
import consulo.ide.impl.idea.openapi.editor.impl.DesktopEditorImpl;
import consulo.ide.impl.idea.pom.core.impl.PomModelImpl;
import consulo.language.Language;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.impl.file.MultiplePsiFilesPerDocumentFileViewProvider;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.template.TemplateLanguage;
import consulo.logging.Logger;
import consulo.logging.attachment.AttachmentFactory;
import consulo.project.Project;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.event.CommandEvent;
import consulo.undoRedo.event.CommandListener;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.application.options.editor.XmlEditorOptions;
import consulo.xml.codeInspection.htmlInspections.RenameTagBeginOrEndIntentionAction;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lang.xhtml.XHTMLLanguage;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.xml.XmlTokenType;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;

@ExtensionImpl
public final class XmlTagNameSynchronizer implements CommandListener, EditorFactoryListener
{
	private static final Key<Boolean> SKIP_COMMAND = Key.create("tag.name.synchronizer.skip.command");
	private static final Logger LOG = Logger.getInstance(XmlTagNameSynchronizer.class);
	private static final Set<Language> SUPPORTED_LANGUAGES = Set.of(HTMLLanguage.INSTANCE,
			XMLLanguage.INSTANCE,
			XHTMLLanguage.INSTANCE);

	private static final Key<TagNameSynchronizer> SYNCHRONIZER_KEY = Key.create("tag_name_synchronizer");

	@Inject
	XmlTagNameSynchronizer(Application application)
	{
		application.getMessageBus().connect().subscribe(CommandListener.class, this);
	}

	@Override
	public void editorCreated(@Nonnull EditorFactoryEvent event)
	{
		Editor editor = event.getEditor();
		Project project = editor.getProject();
		if(project == null || !(editor instanceof DesktopEditorImpl))
		{
			return;
		}

		Document document = editor.getDocument();
		VirtualFile file = FileDocumentManager.getInstance().getFile(document);
		Language language = findXmlLikeLanguage(project, file);
		if(language != null)
		{
			new TagNameSynchronizer((DesktopEditorImpl) editor, project, language).listenForDocumentChanges();
		}
	}

	private static Language findXmlLikeLanguage(Project project, VirtualFile file)
	{
		final PsiFile psiFile = file != null && file.isValid() ? PsiManager.getInstance(project).findFile(file) : null;
		if(psiFile != null)
		{
			for(Language language : psiFile.getViewProvider().getLanguages())
			{
				if((ContainerUtil.find(SUPPORTED_LANGUAGES, language::isKindOf) != null || HtmlUtil.supportsXmlTypedHandlers(psiFile)) &&
						!(language instanceof TemplateLanguage))
				{
					return language;
				}
			}
		}
		return null;
	}

	@Nonnull
	private static TagNameSynchronizer[] findSynchronizers(final Document document)
	{
		if(!XmlEditorOptions.getInstance().isSyncTagEditing() || document == null)
		{
			return TagNameSynchronizer.EMPTY;
		}
		final Editor[] editors = EditorFactory.getInstance().getEditors(document);

		return ContainerUtil.mapNotNull(editors, editor -> editor.getUserData(SYNCHRONIZER_KEY), TagNameSynchronizer.EMPTY);
	}

	@Override
	public void beforeCommandFinished(@Nonnull CommandEvent event)
	{
		final TagNameSynchronizer[] synchronizers = findSynchronizers(event.getDocument());
		for(TagNameSynchronizer synchronizer : synchronizers)
		{
			synchronizer.beforeCommandFinished();
		}
	}

	public static void runWithoutCancellingSyncTagsEditing(@Nonnull Document document, @Nonnull Runnable runnable)
	{
		document.putUserData(SKIP_COMMAND, Boolean.TRUE);
		try
		{
			runnable.run();
		}
		finally
		{
			document.putUserData(SKIP_COMMAND, null);
		}
	}

	private static class TagNameSynchronizer implements DocumentListener
	{
		private static final Key<Couple<RangeMarker>> MARKERS_KEY = Key.create("tag.name.synchronizer.markers");
		private static final TagNameSynchronizer[] EMPTY = new TagNameSynchronizer[0];
		private final PsiDocumentManager myDocumentManager;
		private final Language myLanguage;
		private final DesktopEditorImpl myEditor;
		private boolean myApplying;

		private TagNameSynchronizer(DesktopEditorImpl editor, Project project, Language language)
		{
			myEditor = editor;
			myLanguage = language;
			myDocumentManager = PsiDocumentManager.getInstance(project);
		}

		private void listenForDocumentChanges()
		{
			final Disposable disposable = myEditor.getDisposable();
			final Document document = myEditor.getDocument();
			document.addDocumentListener(this, disposable);
			myEditor.putUserData(SYNCHRONIZER_KEY, this);
		}

		@Override
		public void beforeDocumentChange(@Nonnull DocumentEvent event)
		{
			if(!XmlEditorOptions.getInstance().isSyncTagEditing())
			{
				return;
			}

			final Document document = event.getDocument();
			Project project = Objects.requireNonNull(myEditor.getProject());
			if(myApplying || project.isDefault() || ProjectUndoManager.getInstance(project).isUndoInProgress() ||
					!PomModelImpl.isAllowPsiModification() || document.isInBulkUpdate())
			{
				return;
			}

			final int offset = event.getOffset();
			final int oldLength = event.getOldLength();
			final CharSequence fragment = event.getNewFragment();
			final int newLength = event.getNewLength();

			if(document.getUserData(SKIP_COMMAND) == Boolean.TRUE)
			{
				// xml completion inserts extra space after tag name to ensure correct parsing
				// js auto-import may change beginning of the document when component is imported
				// we need to ignore it
				return;
			}

			Caret caret = myEditor.getCaretModel().getCurrentCaret();

			for(int i = 0; i < newLength; i++)
			{
				if(!XmlUtil.isValidTagNameChar(fragment.charAt(i)))
				{
					clearMarkers(caret);
					return;
				}
			}

			Couple<RangeMarker> markers = caret.getUserData(MARKERS_KEY);
			if(markers != null && !fitsInMarker(markers, offset, oldLength))
			{
				clearMarkers(caret);
				markers = null;
			}
			if(markers == null)
			{
				final PsiFile file = myDocumentManager.getPsiFile(document);
				if(file == null || myDocumentManager.isUnderSynchronization(document))
				{
					return;
				}

				final RangeMarker leader = createTagNameMarker(caret);
				if(leader == null)
				{
					return;
				}
				leader.setGreedyToLeft(true);
				leader.setGreedyToRight(true);

				if(myDocumentManager.isUncommited(document))
				{
					myDocumentManager.commitDocument(document);
				}

				final RangeMarker support = findSupport(leader, file, document);
				if(support == null)
				{
					return;
				}
				support.setGreedyToLeft(true);
				support.setGreedyToRight(true);
				markers = Couple.of(leader, support);
				if(!fitsInMarker(markers, offset, oldLength))
				{
					return;
				}
				caret.putUserData(MARKERS_KEY, markers);
			}
		}

		private static boolean fitsInMarker(Couple<RangeMarker> markers, int offset, int oldLength)
		{
			RangeMarker leader = markers.first;
			return leader.isValid() && offset >= leader.getStartOffset() && offset + oldLength <= leader.getEndOffset();
		}

		private static void clearMarkers(Caret caret)
		{
			Couple<RangeMarker> markers = caret.getUserData(MARKERS_KEY);
			if(markers != null)
			{
				markers.first.dispose();
				markers.second.dispose();
				caret.putUserData(MARKERS_KEY, null);
			}
		}

		private RangeMarker createTagNameMarker(Caret caret)
		{
			final int offset = caret.getOffset();
			final Document document = myEditor.getDocument();
			final CharSequence sequence = document.getCharsSequence();
			int start = -1;
			boolean seenColon = false;
			for(int i = offset - 1; i >= Math.max(0, offset - 50); i--)
			{
				try
				{
					final char c = sequence.charAt(i);
					if(c == '<' || c == '/' && i > 0 && sequence.charAt(i - 1) == '<')
					{
						start = i + 1;
						break;
					}
					if(!XmlUtil.isValidTagNameChar(c))
					{
						break;
					}
					seenColon |= c == ':';
				}
				catch(IndexOutOfBoundsException e)
				{
					LOG.error("incorrect offset:" + i + ", initial: " + offset, AttachmentFactory.get().create("document.txt", sequence.toString()));
					return null;
				}
			}
			if(start < 0)
			{
				return null;
			}
			int end = -1;
			for(int i = offset; i < Math.min(document.getTextLength(), offset + 50); i++)
			{
				final char c = sequence.charAt(i);
				if(!XmlUtil.isValidTagNameChar(c) || seenColon && c == ':')
				{
					end = i;
					break;
				}
				seenColon |= c == ':';
			}
			if(end < 0 || start > end)
			{
				return null;
			}
			return document.createRangeMarker(start, end, true);
		}

		void beforeCommandFinished()
		{
			CaretAction action = caret -> {
				Couple<RangeMarker> markers = caret.getUserData(MARKERS_KEY);
				if(markers == null || !markers.first.isValid() || !markers.second.isValid())
				{
					return;
				}
				final Document document = myEditor.getDocument();
				final Runnable apply = () -> {
					final RangeMarker leader = markers.first;
					final RangeMarker support = markers.second;
					if(document.getTextLength() < leader.getEndOffset())
					{
						return;
					}
					final String name = document.getText(new TextRange(leader.getStartOffset(), leader.getEndOffset()));
					if(document.getTextLength() >= support.getEndOffset() &&
							!name.equals(document.getText(new TextRange(support.getStartOffset(), support.getEndOffset()))))
					{
						document.replaceString(support.getStartOffset(), support.getEndOffset(), name);
					}
				};
				ApplicationManager.getApplication().runWriteAction(() -> {
					final LookupImpl lookup = (LookupImpl) LookupManager.getActiveLookup(myEditor);
					if(lookup != null)
					{
						lookup.performGuardedChange(apply);
					}
					else
					{
						apply.run();
					}
				});
			};
			myApplying = true;
			try
			{
				if(myEditor.getCaretModel().isIteratingOverCarets())
				{
					action.perform(myEditor.getCaretModel().getCurrentCaret());
				}
				else
				{
					myEditor.getCaretModel().runForEachCaret(action);
				}
			}
			finally
			{
				myApplying = false;
			}
		}

		private RangeMarker findSupport(RangeMarker leader, PsiFile file, Document document)
		{
			final int offset = leader.getStartOffset();
			PsiElement element = InjectedLanguageManager.getInstance(file.getProject()).findElementAtNoCommit(file, offset);
			PsiElement support = findSupportElement(element);
			if(support == null && file.getViewProvider() instanceof MultiplePsiFilesPerDocumentFileViewProvider)
			{
				element = file.getViewProvider().findElementAt(offset, myLanguage);
				support = findSupportElement(element);
			}

			if(support == null)
			{
				return findSupportForTagList(leader, element, document);
			}

			final TextRange range = support.getTextRange();
			TextRange realRange = InjectedLanguageManager.getInstance(file.getProject()).injectedToHost(element.getContainingFile(), range);
			return document.createRangeMarker(realRange.getStartOffset(), realRange.getEndOffset(), true);
		}

		private static RangeMarker findSupportForTagList(RangeMarker leader, PsiElement element, Document document)
		{
			if(leader.getStartOffset() != leader.getEndOffset() || element == null)
			{
				return null;
			}

			PsiElement support = null;
			if("<>".equals(element.getText()))
			{
				PsiElement last = element.getParent().getLastChild();
				if("</>".equals(last.getText()))
				{
					support = last;
				}
			}
			if("</>".equals(element.getText()))
			{
				PsiElement first = element.getParent().getFirstChild();
				if("<>".equals(first.getText()))
				{
					support = first;
				}
			}
			if(support != null)
			{
				TextRange range = support.getTextRange();
				return document.createRangeMarker(range.getEndOffset() - 1, range.getEndOffset() - 1, true);
			}
			return null;
		}

		private static PsiElement findSupportElement(PsiElement element)
		{
			if(element == null || TreeUtil.findSibling(element.getNode(), XmlTokenType.XML_TAG_END) == null)
			{
				return null;
			}
			PsiElement support = RenameTagBeginOrEndIntentionAction.findOtherSide(element, false);
			support = support == null || element == support ? RenameTagBeginOrEndIntentionAction.findOtherSide(element, true) : support;
			return support != null && StringUtil.equals(element.getText(), support.getText()) ? support : null;
		}
	}
}
