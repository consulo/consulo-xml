/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.relaxNG.compact.psi.impl;

import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixProvider;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.intellij.plugins.relaxNG.compact.RncFileType;
import org.intellij.plugins.relaxNG.compact.RncTokenTypes;
import org.intellij.plugins.relaxNG.compact.psi.*;
import org.intellij.plugins.relaxNG.compact.psi.util.EscapeUtil;
import org.intellij.plugins.relaxNG.compact.psi.util.RenameUtil;
import org.intellij.plugins.relaxNG.model.Define;
import org.intellij.plugins.relaxNG.model.resolve.DefinitionResolver;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author sweinreuter
 * @since 2007-08-13
 */
class PatternReference extends PsiReferenceBase.Poly<RncRef> implements Function<Define, ResolveResult>,
  LocalQuickFixProvider, EmptyResolveMessageProvider {

  public PatternReference(RncRef ref) {
    super(ref);
  }

  @Override
  public TextRange getRangeInElement() {
    final ASTNode node = findNameNode();
    if (node == null) return TextRange.from(0, 0);
    final int offset = myElement.getTextOffset();
    return TextRange.from(offset - myElement.getTextRange().getStartOffset(), node.getTextLength());
  }

  private ASTNode findNameNode() {
    final ASTNode node = myElement.getNode();
    assert node != null;
    return node.findChildByType(RncTokenTypes.IDENTIFIERS);
  }

  @Override
  @Nullable
  public PsiElement resolve() {
    final ResolveResult[] results = multiResolve(false);
    return results.length == 1 ? results[0].getElement() : null;
  }

  @Override
  @Nonnull
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    final RncGrammar scope = getScope();
    if (scope == null) {
      return ResolveResult.EMPTY_ARRAY;
    }

    final Set<Define> set = DefinitionResolver.resolve(scope, getCanonicalText());
    if (set == null || set.size() == 0) return ResolveResult.EMPTY_ARRAY;

    return ContainerUtil.map2Array(set, ResolveResult.class, this);
  }

  @Override
  public ResolveResult apply(Define rncDefine) {
    final PsiElement element = rncDefine.getPsiElement();
    return element != null ? new PsiElementResolveResult(element) : new ResolveResult() {
      @Override
      @Nullable
      public PsiElement getElement() {
        return null;
      }

      @Override
      public boolean isValidResult() {
        return false;
      }
    };
  }

  @Nullable
  protected RncGrammar getScope() {
    return PsiTreeUtil.getParentOfType(myElement, RncGrammar.class, true, PsiFile.class);
  }

  @Override
  @Nonnull
  public String getCanonicalText() {
    final ASTNode node = findNameNode();
    return node != null ? EscapeUtil.unescapeText(node) : "";
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws consulo.language.util.IncorrectOperationException {
    final ASTNode newNode = RenameUtil.createIdentifierNode(getElement().getManager(), newElementName);

    final ASTNode nameNode = findNameNode();
    nameNode.getTreeParent().replaceChild(nameNode, newNode);
    return getElement();
  }

  @Override
  public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nonnull
  public Object[] getVariants() {
    final RncGrammar scope = getScope();
    if (scope == null) {
      return ResolveResult.EMPTY_ARRAY;
    }

    final Map<String, Set<Define>> map = DefinitionResolver.getAllVariants(scope);
    if (map == null || map.size() == 0) return ArrayUtil.EMPTY_OBJECT_ARRAY;

    return ContainerUtil.mapNotNull(map.values(),
                                    (Function<Set<Define>, Object>)defines -> defines.size() == 0 ? null : defines.iterator()
                                                                                                                  .next()
                                                                                                                  .getPsiElement())
                        .toArray();
  }

  @Override
  public boolean isSoft() {
    return false;
  }

  @Nonnull
  @Override
  public LocalizeValue buildUnresolvedMessage(@Nonnull String s) {
    return LocalizeValue.localizeTODO("Unresolved pattern reference '" + s + "'");
  }

  @Nullable
  @Override
  public LocalQuickFix[] getQuickFixes() {
    if (getScope() != null) {
      return new LocalQuickFix[]{new CreatePatternFix(this)};
    }
    return LocalQuickFix.EMPTY_ARRAY;
  }

  static class CreatePatternFix implements LocalQuickFix {
    private final PatternReference myReference;

    public CreatePatternFix(PatternReference reference) {
      myReference = reference;
    }

    @Nonnull
    @Override
    public String getName() {
      return "Create Pattern '" + myReference.getCanonicalText() + "'";
    }

    @Override
    @Nonnull
    public String getFamilyName() {
      return "Create Pattern";
    }

    @Override
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
      final RncFile rncfile = (RncFile)PsiFileFactory.getInstance(myReference.getElement().getProject())
                                                     .createFileFromText("dummy.rnc", RncFileType.getInstance(), "dummy = xxx");

      final RncGrammar grammar = rncfile.getGrammar();
      assert grammar != null;

      final RncDefine def = (RncDefine)grammar.getFirstChild();

      final RncGrammar scope = myReference.getScope();
      assert scope != null;

      assert def != null;
      final RncDefine e = (RncDefine)scope.add(def);

      // ensures proper escaping (start -> \start)
      def.setName(myReference.getCanonicalText());

      final SmartPsiElementPointer<RncDefine> p = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(e);

      final ASTNode blockNode = e.getParent().getNode();
      assert blockNode != null;

      final ASTNode newNode = e.getNode();
      assert newNode != null;

      CodeStyleManager.getInstance(e.getManager().getProject()).reformatNewlyAddedElement(blockNode, newNode);

      final RncDefine d = p.getElement();
      assert d != null;

      final RncElement definition = d.getPattern();
      assert definition != null;

      final int offset = definition.getTextRange().getStartOffset();

      definition.delete();

      VirtualFile virtualFile = myReference.getElement().getContainingFile().getVirtualFile();
      if (virtualFile != null) {
        FileEditorManager.getInstance(project)
                         .openTextEditor(OpenFileDescriptorFactory.getInstance(project).builder(virtualFile).offset(offset).build(), true);
      }
    }
  }
}
