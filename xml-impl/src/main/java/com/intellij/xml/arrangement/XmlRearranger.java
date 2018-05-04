package com.intellij.xml.arrangement;

import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.EntryType.XML_ATTRIBUTE;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.EntryType.XML_TAG;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.General.ORDER;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.General.TYPE;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Order.BY_NAME;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Order.KEEP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.arrangement.ArrangementSettings;
import com.intellij.psi.codeStyle.arrangement.ArrangementSettingsSerializer;
import com.intellij.psi.codeStyle.arrangement.ArrangementUtil;
import com.intellij.psi.codeStyle.arrangement.DefaultArrangementSettingsSerializer;
import com.intellij.psi.codeStyle.arrangement.Rearranger;
import com.intellij.psi.codeStyle.arrangement.group.ArrangementGroupingRule;
import com.intellij.psi.codeStyle.arrangement.match.ArrangementEntryMatcher;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementEntryMatcher;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchCondition;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsAware;
import com.intellij.psi.codeStyle.arrangement.std.CompositeArrangementSettingsToken;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementSettings;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens;
import com.intellij.util.containers.ContainerUtilRt;

/**
 * @author Eugene.Kudelevsky
 */
public class XmlRearranger implements Rearranger<XmlElementArrangementEntry>, ArrangementStandardSettingsAware
{

	private static final Set<ArrangementSettingsToken> SUPPORTED_TYPES = ContainerUtilRt.newLinkedHashSet(XML_TAG, XML_ATTRIBUTE);
	private static final List<StdArrangementMatchRule> DEFAULT_MATCH_RULES = new ArrayList<StdArrangementMatchRule>();

	private static final StdArrangementSettings DEFAULT_SETTINGS;

	static
	{
		DEFAULT_MATCH_RULES.add(new StdArrangementMatchRule(new StdArrangementEntryMatcher(new ArrangementAtomMatchCondition(StdArrangementTokens
				.Regexp.NAME, "xmlns:.*"))));
		DEFAULT_SETTINGS = StdArrangementSettings.createByMatchRules(Collections.<ArrangementGroupingRule>emptyList(), DEFAULT_MATCH_RULES);
	}

	private static final DefaultArrangementSettingsSerializer SETTINGS_SERIALIZER = new DefaultArrangementSettingsSerializer(DEFAULT_SETTINGS);

	@Nonnull
	public static StdArrangementMatchRule attrArrangementRule(
			@Nonnull String nameFilter, @Nonnull String namespaceFilter, @Nonnull ArrangementSettingsToken orderType)
	{
		return new StdArrangementMatchRule(new StdArrangementEntryMatcher(ArrangementUtil.combine(new ArrangementAtomMatchCondition
				(StdArrangementTokens.Regexp.NAME, nameFilter), new ArrangementAtomMatchCondition(StdArrangementTokens.Regexp.XML_NAMESPACE,
				namespaceFilter))), orderType);
	}

	@Nonnull
	@Override
	public ArrangementSettingsSerializer getSerializer()
	{
		return SETTINGS_SERIALIZER;
	}

	@Nullable
	@Override
	public StdArrangementSettings getDefaultSettings()
	{
		return DEFAULT_SETTINGS;
	}

	@Override
	public boolean isEnabled(@Nonnull ArrangementSettingsToken token, @Nullable ArrangementMatchCondition current)
	{
		return SUPPORTED_TYPES.contains(token) || StdArrangementTokens.Regexp.NAME.equals(token) || StdArrangementTokens.Regexp.XML_NAMESPACE.equals
				(token) || KEEP.equals(token) || BY_NAME.equals(token) || SUPPORTED_TYPES.contains(token);
	}

	@Nonnull
	@Override
	public Collection<Set<ArrangementSettingsToken>> getMutexes()
	{
		return Collections.singleton(SUPPORTED_TYPES);
	}

	@Nullable
	@Override
	public Pair<XmlElementArrangementEntry, List<XmlElementArrangementEntry>> parseWithNew(
			@Nonnull PsiElement root,
			@Nullable Document document,
			@Nonnull Collection<TextRange> ranges,
			@Nonnull PsiElement element,
			@Nonnull ArrangementSettings settings)
	{
		final XmlArrangementParseInfo newEntryInfo = new XmlArrangementParseInfo();
		element.accept(new XmlArrangementVisitor(newEntryInfo, Collections.singleton(element.getTextRange())));

		if(newEntryInfo.getEntries().size() != 1)
		{
			return null;
		}
		final XmlElementArrangementEntry entry = newEntryInfo.getEntries().get(0);
		final XmlArrangementParseInfo existingEntriesInfo = new XmlArrangementParseInfo();
		root.accept(new XmlArrangementVisitor(existingEntriesInfo, ranges));
		return Pair.create(entry, existingEntriesInfo.getEntries());
	}

	@Nonnull
	@Override
	public List<XmlElementArrangementEntry> parse(
			@Nonnull PsiElement root, @Nullable Document document, @Nonnull Collection<TextRange> ranges, @Nonnull ArrangementSettings settings)
	{
		final XmlArrangementParseInfo parseInfo = new XmlArrangementParseInfo();
		root.accept(new XmlArrangementVisitor(parseInfo, ranges));
		return parseInfo.getEntries();
	}

	@Override
	public int getBlankLines(
			@Nonnull CodeStyleSettings settings,
			@Nullable XmlElementArrangementEntry parent,
			@Nullable XmlElementArrangementEntry previous,
			@Nonnull XmlElementArrangementEntry target)
	{
		return -1;
	}

	@Nullable
	@Override
	public List<CompositeArrangementSettingsToken> getSupportedGroupingTokens()
	{
		return null;
	}

	@Nullable
	@Override
	public List<CompositeArrangementSettingsToken> getSupportedMatchingTokens()
	{
		return ContainerUtilRt.newArrayList(new CompositeArrangementSettingsToken(TYPE, SUPPORTED_TYPES),
				new CompositeArrangementSettingsToken(StdArrangementTokens.Regexp.NAME), new CompositeArrangementSettingsToken(StdArrangementTokens
				.Regexp.XML_NAMESPACE), new CompositeArrangementSettingsToken(ORDER, KEEP, BY_NAME));
	}

	@Nonnull
	@Override
	public ArrangementEntryMatcher buildMatcher(@Nonnull ArrangementMatchCondition condition) throws IllegalArgumentException
	{
		throw new IllegalArgumentException("Can't build a matcher for condition " + condition);
	}
}
