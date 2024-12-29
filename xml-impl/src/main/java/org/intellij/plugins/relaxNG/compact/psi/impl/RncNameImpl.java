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

import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.CodeInsightUtilCore;
import consulo.language.editor.completion.lookup.LookupItem;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixProvider;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.template.*;
import consulo.language.psi.EmptyResolveMessageProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiReference;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import org.intellij.plugins.relaxNG.compact.RncElementTypes;
import org.intellij.plugins.relaxNG.compact.RncFileType;
import org.intellij.plugins.relaxNG.compact.RncTokenTypes;
import org.intellij.plugins.relaxNG.compact.psi.*;
import org.intellij.plugins.relaxNG.compact.psi.util.EscapeUtil;
import org.intellij.plugins.relaxNG.compact.psi.util.RenameUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 14.08.2007
 */
public class RncNameImpl extends RncElementImpl implements RncName, PsiReference,
    EmptyResolveMessageProvider, LocalQuickFixProvider {

  private enum Kind {
    NAMESPACE, DATATYPES
  }

  public RncNameImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public String getPrefix() {
    final String[] parts = EscapeUtil.unescapeText(getNode()).split(":", 2);
    return parts.length == 2 ? parts[0] : null;
  }

  @Override
  @Nonnull
  public String getLocalPart() {
    final String[] parts = EscapeUtil.unescapeText(getNode()).split(":", 2);
    return parts.length == 1 ? parts[0] : parts[1];
  }

  @Override
  public void accept(@Nonnull RncElementVisitor visitor) {
    visitor.visitName(this);
  }

  @Override
  public PsiReference getReference() {
    return getPrefix() == null ? null : this;
  }

  @Override
  public PsiElement getElement() {
    return this;
  }

  @Override
  public TextRange getRangeInElement() {
    return TextRange.from(0, getText().indexOf(':'));
  }

  @Override
  @Nullable
  public PsiElement resolve() {
    final MyResolver resolver = new MyResolver(getPrefix(), getKind());
    getContainingFile().processDeclarations(resolver, ResolveState.initial(), this, this);
    return resolver.getResult();
  }

  private Kind getKind() {
    final IElementType parent = getNode().getTreeParent().getElementType();
    if (parent == RncElementTypes.DATATYPE_PATTERN) {
      return Kind.DATATYPES;
    } else {
      return Kind.NAMESPACE;
    }
  }

  @Override
  @Nonnull
  public String getCanonicalText() {
    return getRangeInElement().substring(getText());
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    final ASTNode node = getNode();
    final ASTNode child = RenameUtil.createPrefixedNode(getManager(), newElementName, getLocalPart());
    node.getTreeParent().replaceChild(node, child);
    return child.getPsi();
  }

  @Override
  public PsiElement bindToElement(@Nonnull PsiElement element) throws consulo.language.util.IncorrectOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    return element instanceof RncElement && Comparing.equal(resolve(), element);
  }

  @Override
  @Nonnull
  public Object[] getVariants() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public boolean isSoft() {
    final String prefix = getPrefix();
    return "xsd".equals(prefix) || "xml".equals(prefix);
  }

  @Nonnull
  @Override
  public LocalizeValue buildUnresolvedMessage(@Nonnull String s) {
    return LocalizeValue.localizeTODO("Unresolved namespace prefix '" + s + "'");
  }

  @Nullable
  @Override
  public LocalQuickFix[] getQuickFixes() {
    if (getPrefix() != null) {
      return new LocalQuickFix[] { new CreateDeclFix(this) };
    }
    return LocalQuickFix.EMPTY_ARRAY;
  }

  private static class MyResolver implements PsiScopeProcessor {
    private final String myPrefix;
    private final Kind myKind;
    private PsiElement myResult;

    public MyResolver(String prefix, Kind kind) {
      myPrefix = prefix;
      myKind = kind;
    }

    @Override
    public boolean execute(@Nonnull PsiElement element, @Nonnull ResolveState substitutor) {
      final ASTNode node = element.getNode();
      if (node == null) return true;

      if (!(element instanceof RncDecl)) {
        return false;
      }

      final IElementType type = node.getElementType();
      if (myKind == Kind.NAMESPACE && type == RncElementTypes.NS_DECL) {
        if (checkDecl(element)) return false;
      } else if (myKind == Kind.DATATYPES && type == RncElementTypes.DATATYPES_DECL) {
        if (checkDecl(element)) return false;
      }

      return true;
    }

    private boolean checkDecl(PsiElement element) {
      if (myPrefix.equals(((RncDecl)element).getPrefix())) {
        myResult = element;
        return true;
      }
      return false;
    }

    public PsiElement getResult() {
      return myResult;
    }
  }

  public static class CreateDeclFix implements LocalQuickFix {
    private final RncNameImpl myReference;

    public CreateDeclFix(RncNameImpl reference) {
      myReference = reference;
    }

    @Override
    @Nonnull
    public String getName() {
      return getFamilyName() + " '" + myReference.getPrefix() + "'";
    }

    @Override
    @Nonnull
      public String getFamilyName() {
      return "Create " + myReference.getKind().name().toLowerCase() + " declaration";
    }

    @Override
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
      final String prefix = myReference.getPrefix();
      final PsiFileFactory factory = PsiFileFactory.getInstance(myReference.getProject());
      final RncFile psiFile = (RncFile)factory.createFileFromText("dummy.rnc",
                                                                  RncFileType.getInstance(),
                                                                   myReference.getKind().name().toLowerCase() + " " + prefix + " = \"###\"");
      final RncFile rncFile = (RncFile)myReference.getContainingFile();
      final RncDecl[] declarations = rncFile.getDeclarations();
      final RncDecl decl = psiFile.getDeclarations()[0];
      final RncDecl e;
      if (declarations.length > 0) {
        e = (RncDecl)rncFile.addAfter(decl, declarations[declarations.length - 1]);
      } else {
        final RncGrammar rncGrammar = rncFile.getGrammar();
        if (rncGrammar != null) {
          e = (RncDecl)rncFile.addBefore(decl, rncGrammar);
        } else {
          e = (RncDecl)rncFile.add(decl);
        }
      }

      final ASTNode blockNode = e.getParent().getNode();
      assert blockNode != null;

      final ASTNode newNode = e.getNode();
      assert newNode != null;

      CodeStyleManager.getInstance(e.getManager().getProject()).reformatNewlyAddedElement(blockNode, newNode);

      final PsiElement literal = e.getLastChild();
      assert literal != null;

      final ASTNode literalNode = literal.getNode();
      assert literalNode != null;

      assert literalNode.getElementType() == RncTokenTypes.LITERAL;

      final int offset = literal.getTextRange().getStartOffset();

      literal.delete();

      VirtualFile virtualFile = myReference.getElement().getContainingFile().getVirtualFile();
      if (virtualFile != null) {
        Editor editor = FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptorFactory.getInstance(project).builder(virtualFile).offset(offset).build(), true);
        if (editor != null) {
          RncDecl rncDecl = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(e);

          final TemplateManager manager = TemplateManager.getInstance(project);
          final Template t = manager.createTemplate("", "");
          t.addTextSegment(" \"");
          final Expression expression = new Expression() {
            @Override
            public Result calculateResult(ExpressionContext context) {
              return new TextResult("");
            }

            @Override
            public Result calculateQuickResult(ExpressionContext context) {
              return calculateResult(context);
            }

            @Override
            public LookupItem[] calculateLookupItems(ExpressionContext context) {
              return LookupItem.EMPTY_ARRAY;
            }
          };
          t.addVariable("uri", expression, expression, true);
          t.addTextSegment("\"");
          t.addEndVariable();

          editor.getCaretModel().moveToOffset(rncDecl.getTextRange().getEndOffset());
          manager.startTemplate(editor, t);
        }
      }
    }
  }
}
