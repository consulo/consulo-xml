/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.xml.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.xml.codeInspection.XmlQuickFixFactory;
import consulo.xml.codeInspection.htmlInspections.RemoveAttributeIntentionAction;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.function.Condition;
import consulo.util.lang.StringUtil;
import consulo.language.ast.RoleFinder;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlChildRole;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.XmlTagRuleProvider;

public abstract class XmlTagRuleProviderBase extends XmlTagRuleProvider {

    public static RequireAttributeOneOf requireAttr(String... attributeNames) {
        return new RequireAttributeOneOf(attributeNames);
    }

    public static ShouldHaveParams shouldHaveParams() {
        return new ShouldHaveParams();
    }

    public static Rule unusedIfPresent(String attrPresent, String... attrUnused) {
        Effect[] effects = new Effect[attrUnused.length];
        for (int i = 0; i < effects.length; i++) {
            effects[i] = unused(
                attrUnused[i],
                "The attribute '" + attrUnused[i] + "' is unused because the attribute '" + attrPresent + "' is present"
            );
        }

        return new ConditionRule(ifAttrPresent(attrPresent), effects);
    }

    public static Rule unusedAllIfPresent(String attrPresent, String... attrUnused) {
        return new ConditionRule(
            ifAttrPresent(attrPresent),
            new InvalidAllExpectSome(
                "The attribute is unused because the attribute " + attrPresent + " is present",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                ArrayUtil.append(attrUnused, attrPresent)
            )
        );
    }

    public static Effect invalid(String attrName, String text) {
        return new InvalidAttrEffect(attrName, text, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }

    public static Effect unused(String attrName) {
        return new InvalidAttrEffect(attrName, "Attribute '" + attrName + "' is unused", ProblemHighlightType.LIKE_UNUSED_SYMBOL);
    }

    public static Effect unused(String attrName, String text) {
        return new InvalidAttrEffect(attrName, text, ProblemHighlightType.LIKE_UNUSED_SYMBOL);
    }

    public static Effect unusedAll(String text, String... attrNames) {
        return new InvalidAllExpectSome(text, ProblemHighlightType.LIKE_UNUSED_SYMBOL, attrNames);
    }

    public static Rule rule(Condition<XmlTag> condition, Effect... effect) {
        return new ConditionRule(condition, effect);
    }

    @Nullable
    public static PsiElement getXmlElement(RoleFinder roleFinder, XmlElement tag) {
        ASTNode tagNode = tag.getNode();
        if (tagNode == null) {
            return null;
        }

        ASTNode nameElement = roleFinder.findChild(tagNode);
        if (nameElement == null) {
            return null;
        }

        return nameElement.getPsi();
    }

    @Nullable
    public static PsiElement getTagNameElement(XmlTag tag) {
        return getXmlElement(XmlChildRole.START_TAG_NAME_FINDER, tag);
    }

    @Nullable
    public static PsiElement getAttributeNameElement(XmlAttribute attribute) {
        return getXmlElement(XmlChildRole.ATTRIBUTE_NAME_FINDER, attribute);
    }

    public static boolean isClosedTag(XmlTag tag) {
        return getXmlElement(XmlChildRole.EMPTY_TAG_END_FINDER, tag) != null
            || getXmlElement(XmlChildRole.CLOSING_TAG_START_FINDER, tag) != null;
    }

    public static Condition<XmlTag> ifAttrPresent(final String attrName) {
        return tag -> tag.getAttribute(attrName) != null;
    }

    // ---=== Classes ===---

    public abstract static class Effect {
        public abstract void annotate(@Nonnull XmlTag tag, @Nonnull ProblemsHolder holder);
    }

    public static class InvalidAttrEffect extends Effect {
        private final String myAttrName;
        private final String myText;
        private final ProblemHighlightType myType;

        public InvalidAttrEffect(String attrName, String text, ProblemHighlightType type) {
            myAttrName = attrName;
            myText = text;
            myType = type;
        }

        @Override
        public void annotate(@Nonnull XmlTag tag, @Nonnull ProblemsHolder holder) {
            XmlAttribute attribute = tag.getAttribute(myAttrName);
            if (attribute != null) {
                PsiElement attributeNameElement = getAttributeNameElement(attribute);
                if (attributeNameElement != null) {
                    holder.registerProblem(attributeNameElement, myText, myType, new RemoveAttributeIntentionAction(myAttrName));
                }
            }
        }
    }

    public static class InvalidAllExpectSome extends Effect {
        private final String[] myAttrNames;
        private final String myText;
        private final ProblemHighlightType myType;

        public InvalidAllExpectSome(String text, ProblemHighlightType type, String... attrNames) {
            myAttrNames = attrNames;
            myText = text;
            myType = type;
        }

        @Override
        public void annotate(@Nonnull XmlTag tag, @Nonnull ProblemsHolder holder) {
            for (XmlAttribute xmlAttribute : tag.getAttributes()) {
                String attrName = xmlAttribute.getName();
                if (!ArrayUtil.contains(attrName, myAttrNames)) {
                    PsiElement attributeNameElement = getAttributeNameElement(xmlAttribute);
                    if (attributeNameElement != null) {
                        holder.registerProblem(attributeNameElement, myText, myType, new RemoveAttributeIntentionAction(attrName));
                    }
                }
            }
        }
    }

    public static class ConditionRule extends Rule {
        private final Condition<XmlTag> myCondition;
        private final Effect[] myEffect;

        public ConditionRule(Condition<XmlTag> condition, Effect... effect) {
            this.myCondition = condition;
            this.myEffect = effect;
        }

        @Override
        public void annotate(@Nonnull XmlTag tag, @Nonnull ProblemsHolder holder) {
            if (myCondition.value(tag)) {
                for (Effect effect : myEffect) {
                    effect.annotate(tag, holder);
                }
            }
        }
    }

    public static class ShouldHaveParams extends Rule {
        @Override
        public boolean needAtLeastOneAttribute(@Nonnull XmlTag tag) {
            return true;
        }
    }

    public static class RequireAttributeOneOf extends ShouldHaveParams {
        private final String[] myAttributeNames;
        private final ProblemHighlightType myProblemHighlightType;

        public RequireAttributeOneOf(String... attributeNames) {
            myAttributeNames = attributeNames;
            myProblemHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
        }

        public RequireAttributeOneOf(@Nonnull ProblemHighlightType problemHighlightType, String... attributeNames) {
            assert attributeNames.length > 0;
            myAttributeNames = attributeNames;
            myProblemHighlightType = problemHighlightType;
        }

        public String[] getAttributeNames() {
            return myAttributeNames;
        }

        @Override
        public void annotate(@Nonnull XmlTag tag, @Nonnull ProblemsHolder holder) {
            for (String attributeName : myAttributeNames) {
                if (tag.getAttribute(attributeName) != null) {
                    return;
                }
            }

            if (!isClosedTag(tag)) {
                return;
            }

            PsiElement tagNameElement = getTagNameElement(tag);
            if (tagNameElement == null) {
                return;
            }

            LocalQuickFix[] fixes = new LocalQuickFix[myAttributeNames.length];
            for (int i = 0; i < myAttributeNames.length; i++) {
                fixes[i] = XmlQuickFixFactory.getInstance().insertRequiredAttributeFix(tag, myAttributeNames[i]);
            }

            holder.registerProblem(
                tagNameElement,
                "Tag should have one of following attributes: " + StringUtil.join(myAttributeNames, ", "),
                myProblemHighlightType,
                fixes
            );
        }
    }
}
