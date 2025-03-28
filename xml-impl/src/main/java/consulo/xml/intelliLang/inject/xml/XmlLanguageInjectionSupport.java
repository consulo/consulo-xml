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

package consulo.xml.intelliLang.inject.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.configurable.Configurable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.Language;
import consulo.language.inject.advanced.*;
import consulo.language.inject.advanced.ui.AbstractInjectionPanel;
import consulo.language.pattern.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.DialogBuilder;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.xml.intelliLang.inject.config.AbstractTagInjection;
import consulo.xml.intelliLang.inject.config.XmlAttributeInjection;
import consulo.xml.intelliLang.inject.config.XmlTagInjection;
import consulo.xml.intelliLang.inject.config.ui.XmlAttributePanel;
import consulo.xml.intelliLang.inject.config.ui.XmlTagPanel;
import consulo.xml.intelliLang.inject.config.ui.configurables.XmlAttributeInjectionConfigurable;
import consulo.xml.intelliLang.inject.config.ui.configurables.XmlTagInjectionConfigurable;
import consulo.xml.patterns.XmlPatterns;
import consulo.xml.psi.xml.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Gregory.Shrago
 */
@ExtensionImpl
public class XmlLanguageInjectionSupport extends AbstractLanguageInjectionSupport {

    @NonNls
    public static final String XML_SUPPORT_ID = "xml";

    private static boolean isMine(final PsiLanguageInjectionHost host) {
        if (host instanceof XmlAttributeValue) {
            final PsiElement p = host.getParent();
            if (p instanceof XmlAttribute) {
                final String s = ((XmlAttribute) p).getName();
                return !(s.equals("xmlns") || s.startsWith("xmlns:"));
            }
        }
        else if (host instanceof XmlText) {
            final XmlTag tag = ((XmlText) host).getParentTag();
            return tag != null/* && tag.getValue().getTextElements().length == 1 && tag.getSubTags().length == 0*/;
        }
        return false;
    }

    @Nonnull
    public String getId() {
        return XML_SUPPORT_ID;
    }

    @Nonnull
    public Class[] getPatternClasses() {
        return new Class[]{XmlPatterns.class};
    }

    @Override
    public boolean isApplicableTo(PsiLanguageInjectionHost host) {
        return host instanceof XmlElement;
    }

    @Nullable
    @Override
    public BaseInjection findCommentInjection(@Nonnull PsiElement host, @Nullable Ref<PsiElement> commentRef) {
        if (host instanceof XmlAttributeValue) {
            return null;
        }
        return InjectorUtils.findCommentInjection(host instanceof XmlText ? host.getParent() : host, getId(), commentRef);
    }

    @Override
    public boolean addInjectionInPlace(Language language, final PsiLanguageInjectionHost psiElement) {
        if (!isMine(psiElement)) {
            return false;
        }
        String id = language.getID();
        if (psiElement instanceof XmlAttributeValue) {
            return doInjectInAttributeValue((XmlAttributeValue) psiElement, id);
        }
        else if (psiElement instanceof XmlText) {
            return doInjectInXmlText((XmlText) psiElement, id);
        }
        return false;
    }

    public boolean removeInjectionInPlace(final PsiLanguageInjectionHost host) {
        return removeInjection(host);
    }

    @Override
    public boolean removeInjection(PsiElement host) {
        final Project project = host.getProject();
        final Configuration configuration = Configuration.getProjectInstance(project);
        final ArrayList<BaseInjection> injections = collectInjections(host, configuration);
        if (injections.isEmpty()) {
            return false;
        }
        final ArrayList<BaseInjection> newInjections = new ArrayList<BaseInjection>();
        for (BaseInjection injection : injections) {
            final BaseInjection newInjection = injection.copy();
            newInjection.setPlaceEnabled(null, false);
            if (InjectorUtils.canBeRemoved(newInjection)) {
                continue;
            }
            newInjections.add(newInjection);
        }
        configuration.replaceInjectionsWithUndo(
            project, newInjections, injections, Collections.<PsiElement>emptyList());
        return true;
    }

    public boolean editInjectionInPlace(final PsiLanguageInjectionHost host) {
        if (!isMine(host)) {
            return false;
        }
        final Project project = host.getProject();
        final Configuration configuration = Configuration.getProjectInstance(project);
        final ArrayList<BaseInjection> injections = collectInjections(host, configuration);
        if (injections.isEmpty()) {
            return false;
        }
        final BaseInjection originalInjection = injections.get(0);
        final BaseInjection xmlInjection = createFrom(originalInjection);
        final BaseInjection newInjection =
            xmlInjection == null ? showDefaultInjectionUI(project, originalInjection.copy()) : showInjectionUI(project, xmlInjection);
        if (newInjection != null) {
            configuration.replaceInjectionsWithUndo(
                project, Collections.singletonList(newInjection),
                Collections.singletonList(originalInjection),
                Collections.<PsiElement>emptyList());
        }
        return true;
    }

    @Nullable
    private static BaseInjection showInjectionUI(final Project project, final BaseInjection xmlInjection) {
        final DialogBuilder builder = new DialogBuilder(project);
        final AbstractInjectionPanel panel;
        if (xmlInjection instanceof XmlTagInjection) {
            panel = new XmlTagPanel((XmlTagInjection) xmlInjection, project);
            builder.setHelpId("reference.settings.injection.language.injection.settings.xml.tag");
        }
        else if (xmlInjection instanceof XmlAttributeInjection) {
            panel = new XmlAttributePanel((XmlAttributeInjection) xmlInjection, project);
            builder.setHelpId("reference.settings.injection.language.injection.settings.xml.attribute");
        }
        else {
            throw new AssertionError();
        }
        panel.reset();
        builder.addOkAction();
        builder.addCancelAction();
        builder.setCenterPanel(panel.getComponent());
        builder.setTitle("Language Injection Settings");
        builder.setOkOperation(new Runnable() {
            public void run() {
                panel.apply();
                builder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
            }
        });
        if (builder.show() == DialogWrapper.OK_EXIT_CODE) {
            return new AbstractTagInjection().copyFrom(xmlInjection);
        }
        return null;
    }

    @Nullable
    private static BaseInjection createFrom(final BaseInjection injection) {
        if (injection.getInjectionPlaces().length == 0 || injection.getInjectionPlaces().length > 1) {
            return null;
        }

        AbstractTagInjection result;
        final InjectionPlace place = injection.getInjectionPlaces()[0];
        final ElementPattern<PsiElement> rootPattern = place.getElementPattern();
        final ElementPatternCondition<PsiElement> rootCondition = rootPattern.getCondition();
        final Class<PsiElement> elementClass = rootCondition.getInitialCondition().getAcceptedClass();
        if (XmlAttribute.class.equals(elementClass)) {
            result = new XmlAttributeInjection().copyFrom(injection);
        }
        else if (XmlTag.class.equals(elementClass)) {
            result = new XmlTagInjection().copyFrom(injection);
        }
        else {
            return null;
        }
        result.setInjectionPlaces(InjectionPlace.EMPTY_ARRAY);
        for (PatternCondition<? super PsiElement> condition : rootCondition.getConditions()) {
            final String value = extractValue(condition);
            if ("withLocalName".equals(condition.getDebugMethodName())) {
                if (value == null) {
                    return null;
                }
                if (result instanceof XmlAttributeInjection) {
                    ((XmlAttributeInjection) result).setAttributeName(value);
                }
                else {
                    result.setTagName(value);
                }
            }
            else if ("withNamespace".equals(condition.getDebugMethodName())) {
                if (value == null) {
                    return null;
                }
                if (result instanceof XmlAttributeInjection) {
                    ((XmlAttributeInjection) result).setAttributeNamespace(value);
                }
                else {
                    result.setTagNamespace(value);
                }
            }
            else if (result instanceof XmlAttributeInjection &&
                "inside".equals(condition.getDebugMethodName()) && condition instanceof PatternConditionPlus) {
                final ElementPattern<?> insidePattern = ((PatternConditionPlus) condition).getValuePattern();
                if (!XmlTag.class.equals(insidePattern.getCondition().getInitialCondition().getAcceptedClass())) {
                    return null;
                }
                for (PatternCondition<?> insideCondition : insidePattern.getCondition().getConditions()) {
                    final String tagValue = extractValue(insideCondition);
                    if (tagValue == null) {
                        return null;
                    }
                    if ("withLocalName".equals(insideCondition.getDebugMethodName())) {
                        result.setTagName(tagValue);
                    }
                    else if ("withNamespace".equals(insideCondition.getDebugMethodName())) {
                        result.setTagNamespace(tagValue);
                    }

                }
            }
            else {
                return null;
            }
        }
        result.generatePlaces();
        return result;
    }

    @Nullable
    private static String extractValue(PatternCondition<?> condition) {
        if (!(condition instanceof PatternConditionPlus)) {
            return null;
        }
        final ElementPattern valuePattern = ((PatternConditionPlus) condition).getValuePattern();
        final ElementPatternCondition<?> rootCondition = valuePattern.getCondition();
        if (!String.class.equals(rootCondition.getInitialCondition().getAcceptedClass())) {
            return null;
        }
        if (rootCondition.getConditions().size() != 1) {
            return null;
        }
        final PatternCondition<?> valueCondition = rootCondition.getConditions().get(0);
        if (!(valueCondition instanceof ValuePatternCondition<?>)) {
            return null;
        }
        final Collection values = ((ValuePatternCondition) valueCondition).getValues();
        if (values.size() == 1) {
            final Object value = values.iterator().next();
            return value instanceof String ? (String) value : null;
        }
        else if (!values.isEmpty()) {
            for (Object value : values) {
                if (!(value instanceof String)) {
                    return null;
                }
            }
            //noinspection unchecked
            return StringUtil.join(values, "|");
        }
        return null;
    }

    public BaseInjection createInjection(final Element element) {
        if (element.getName().equals(XmlAttributeInjection.class.getSimpleName())) {
            return new XmlAttributeInjection();
        }
        else if (element.getName().equals(XmlTagInjection.class.getSimpleName())) {
            return new XmlTagInjection();
        }
        return new AbstractTagInjection();
    }

    public Configurable[] createSettings(final Project project, final Configuration configuration) {
        return new Configurable[0];
    }

    private static boolean doInjectInXmlText(final XmlText host, final String languageId) {
        final XmlTag tag = host.getParentTag();
        if (tag != null) {
            final XmlTagInjection injection = new XmlTagInjection();
            injection.setInjectedLanguageId(languageId);
            injection.setTagName(tag.getLocalName());
            injection.setTagNamespace(tag.getNamespace());
            injection.generatePlaces();
            doEditInjection(host.getProject(), injection);
            return true;
        }
        return false;
    }

    private static void doEditInjection(final Project project, final XmlTagInjection template) {
        final Configuration configuration = InjectorUtils.getEditableInstance(project);
        final AbstractTagInjection originalInjection = (AbstractTagInjection) configuration.findExistingInjection(template);

        final XmlTagInjection newInjection = originalInjection == null ? template : new XmlTagInjection().copyFrom(originalInjection);

        ShowSettingsUtil.getInstance().editConfigurable(project, new XmlTagInjectionConfigurable(newInjection, null, project)).doWhenDone(() -> {
            configuration.replaceInjectionsWithUndo(
                project, Collections.singletonList(newInjection),
                ContainerUtil.createMaybeSingletonList(originalInjection),
                Collections.<PsiElement>emptyList());
        });
    }

    private static boolean doInjectInAttributeValue(final XmlAttributeValue host, final String languageId) {
        final XmlAttribute attribute = PsiTreeUtil.getParentOfType(host, XmlAttribute.class, true);
        final XmlTag tag = attribute == null ? null : attribute.getParent();
        if (tag != null) {
            final XmlAttributeInjection injection = new XmlAttributeInjection();
            injection.setInjectedLanguageId(languageId);
            injection.setAttributeName(attribute.getLocalName());
            injection.setAttributeNamespace(attribute.getNamespace());
            injection.setTagName(tag.getLocalName());
            injection.setTagNamespace(tag.getNamespace());
            injection.generatePlaces();
            doEditInjection(host.getProject(), injection);
            return true;
        }
        return false;
    }

    private static void doEditInjection(final Project project, final XmlAttributeInjection template) {
        final Configuration configuration = InjectorUtils.getEditableInstance(project);
        final BaseInjection originalInjection = configuration.findExistingInjection(template);
        final BaseInjection newInjection = originalInjection == null ? template : originalInjection.copy();

        ShowSettingsUtil.getInstance().editConfigurable(project, new XmlAttributeInjectionConfigurable((XmlAttributeInjection) newInjection, null, project)).doWhenDone(() -> {
            configuration.replaceInjectionsWithUndo(
                project, Collections.singletonList(newInjection),
                ContainerUtil.createMaybeSingletonList(originalInjection),
                Collections.<PsiElement>emptyList());
        });
    }

    private static ArrayList<BaseInjection> collectInjections(final PsiElement host,
                                                              final Configuration configuration) {
        final ArrayList<BaseInjection> result = new ArrayList<BaseInjection>();
        final PsiElement element = host instanceof XmlText ? ((XmlText) host).getParentTag() :
            host instanceof XmlAttributeValue ? host.getParent() : host;
        for (BaseInjection injection : configuration.getInjections(XML_SUPPORT_ID)) {
            if (injection.acceptsPsiElement(element)) {
                result.add(injection);
            }
        }
        return result;
    }

    @Override
    public AnAction[] createAddActions(final Project project, final Consumer<BaseInjection> consumer) {
        return new AnAction[]{
            new AnAction("XML Tag Injection", null, AllIcons.Nodes.Tag) {
                @Override
                public void actionPerformed(final AnActionEvent e) {
                    final BaseInjection newInjection = showInjectionUI(project, new XmlTagInjection());
                    if (newInjection != null) {
                        consumer.accept(newInjection);
                    }
                }
            },
            new AnAction("XML Attribute Injection", null, AllIcons.Nodes.Annotationtype) {
                @Override
                public void actionPerformed(final AnActionEvent e) {
                    final BaseInjection injection = showInjectionUI(project, new XmlAttributeInjection());
                    if (injection != null) {
                        consumer.accept(injection);
                    }
                }
            }
        };
    }

    @Override
    public AnAction createEditAction(final Project project, final Supplier<BaseInjection> producer) {
        return new AnAction() {
            @Override
            public void actionPerformed(final AnActionEvent e) {
                final BaseInjection originalInjection = producer.get();
                final BaseInjection injection = createFrom(originalInjection);
                if (injection != null) {
                    final BaseInjection newInjection = showInjectionUI(project, injection);
                    if (newInjection != null) {
                        originalInjection.copyFrom(newInjection);
                    }
                }
                else {
                    createDefaultEditAction(project, producer).actionPerformed(null);
                }
            }
        };
    }
}
