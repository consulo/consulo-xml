package org.intellij.plugins.relaxNG.compact;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.documentation.LanguageDocumentationProvider;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import org.intellij.plugins.relaxNG.compact.psi.RncDefine;
import org.intellij.plugins.relaxNG.compact.psi.RncElement;
import org.intellij.plugins.relaxNG.compact.psi.util.EscapeUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

@ExtensionImpl
public class RngDocumentationProvider implements DocumentationProvider, LanguageDocumentationProvider
{
	@Override
	@Nullable
	public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement)
	{
		return null;
	}

	@Override
	public List<String> getUrlFor(PsiElement element, PsiElement originalElement)
	{
		return null;
	}

	@Override
	@Nullable
	public String generateDoc(PsiElement element, PsiElement originalElement)
	{
		if(element instanceof RncElement)
		{
			PsiElement comment = element.getPrevSibling();
			while(comment instanceof PsiWhiteSpace)
			{
				comment = comment.getPrevSibling();
			}
			if(comment instanceof PsiComment)
			{
				final StringBuilder sb = new StringBuilder();
				do
				{
					sb.insert(0, EscapeUtil.unescapeText(comment).replaceAll("\n?##?", "") + "<br>");
					comment = comment.getPrevSibling();
				}
				while(comment instanceof PsiComment);

				if(element instanceof RncDefine)
				{
					sb.insert(0, "Define: <b>" + ((RncDefine) element).getName() + "</b><br>");
				}

				return sb.toString();
			}
		}
		return null;
	}

	@Override
	@Nullable
	public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element)
	{
		return null;
	}

	@Override
	@Nullable
	public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context)
	{
		return null;
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return RngCompactLanguage.INSTANCE;
	}
}
