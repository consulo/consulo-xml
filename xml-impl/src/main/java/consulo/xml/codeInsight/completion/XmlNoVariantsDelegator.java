/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.xml.codeInsight.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.xml.lang.xml.XMLLanguage;

import jakarta.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 * @since 2011-12-19
 */
@ExtensionImpl(order = "before xmlNonFirst")
public class XmlNoVariantsDelegator extends CompletionContributor {
    @Override
    @RequiredReadAction
    public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
        boolean empty = result.runRemainingContributors(parameters, true).isEmpty();

        if (!empty && parameters.getInvocationCount() == 0) {
            result.restartCompletionWhenNothingMatches();
        }

        if (empty && parameters.getCompletionType() == CompletionType.BASIC) {
            XmlCompletionContributor.completeTagName(parameters, result);
        }
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
}
