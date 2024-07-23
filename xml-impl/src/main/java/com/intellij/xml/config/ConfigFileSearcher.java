package com.intellij.xml.config;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.language.util.ModuleUtilCore;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.util.collection.MultiMap;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;

public abstract class ConfigFileSearcher {
    private final MultiMap<Module, PsiFile> myFiles = new MultiMap<>();
    private final MultiMap<VirtualFile, PsiFile> myJars = new MultiMap<>();
    private final MultiMap<VirtualFile, PsiFile> myVirtualFiles = new MultiMap<>();
    @Nullable
    private final Module myModule;
    @Nonnull
    private final Project myProject;

    public ConfigFileSearcher(@Nullable Module module, @Nonnull Project project) {
        myModule = module;
        myProject = project;
    }

    public void search() {
        myFiles.clear();
        myJars.clear();

        PsiManager psiManager = PsiManager.getInstance(myProject);
        for (PsiFile file : search(myModule, myProject)) {
            VirtualFile jar = ArchiveVfsUtil.getVirtualFileForJar(file.getVirtualFile());
            if (jar != null) {
                myJars.putValue(jar, file);
            }
            else {
                Module module = ModuleUtilCore.findModuleForPsiElement(file);
                if (module != null) {
                    myFiles.putValue(module, file);
                }
                else {
                    VirtualFile virtualFile = file.getVirtualFile();
                    myVirtualFiles.putValue(virtualFile.getParent(), psiManager.findFile(virtualFile));
                }
            }
        }
    }

    public abstract Set<PsiFile> search(@Nullable Module module, @Nonnull Project project);

    public MultiMap<Module, PsiFile> getFilesByModules() {
        return myFiles;
    }

    public MultiMap<VirtualFile, PsiFile> getJars() {
        return myJars;
    }

    public MultiMap<VirtualFile, PsiFile> getVirtualFiles() {
        return myVirtualFiles;
    }
}
