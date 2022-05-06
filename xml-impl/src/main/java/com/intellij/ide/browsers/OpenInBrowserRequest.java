package com.intellij.ide.browsers;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Url;

public abstract class OpenInBrowserRequest
{
	private Collection<Url> result;
	protected PsiFile file;

	public OpenInBrowserRequest(@Nonnull PsiFile file)
	{
		this.file = file;
	}

	public OpenInBrowserRequest()
	{
	}

	@Nullable
	public static OpenInBrowserRequest create(@Nonnull final PsiElement element)
	{
		PsiFile psiFile = element.isValid() ? element.getContainingFile() : null;
		if(psiFile == null || psiFile.getVirtualFile() == null)
		{
			return null;
		}

		return new OpenInBrowserRequest(psiFile)
		{
			@Nonnull
			@Override
			public PsiElement getElement()
			{
				return element;
			}
		};
	}

	@Nonnull
	public PsiFile getFile()
	{
		return file;
	}

	@Nonnull
	public VirtualFile getVirtualFile()
	{
		return file.getVirtualFile();
	}

	@Nonnull
	public Project getProject()
	{
		return file.getProject();
	}

	@Nonnull
	public abstract PsiElement getElement();

	public void setResult(@Nonnull Collection<Url> result)
	{
		this.result = result;
	}

	@Nullable
	public Collection<Url> getResult()
	{
		return result;
	}
}