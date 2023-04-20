package consulo.xml.util.xml.impl;

import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SimpleReference;
import consulo.xml.util.xml.*;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author peter
 */
public class GetInvocation implements Invocation
{
	private static final Key<CachedValue<List<Pair<Converter, Object>>>> DOM_VALUE_KEY = Key.create("Dom element value key");
	private final Converter myConverter;

	protected GetInvocation(final Converter converter)
	{
		assert converter != null;
		myConverter = converter;
	}

	@Override
	public Object invoke(final DomInvocationHandler<?, ?> handler, final Object[] args) throws Throwable
	{
		if(myConverter == Converter.EMPTY_CONVERTER)
		{
			return getValueInner(handler, myConverter);
		}

		CachedValue<List<Pair<Converter, Object>>> value = handler.getUserData(DOM_VALUE_KEY);
		if(value == null)
		{
			final DomManagerImpl domManager = handler.getManager();
			final Project project = domManager.getProject();
			final CachedValuesManager cachedValuesManager = CachedValuesManager.getManager(project);
			handler.putUserData(DOM_VALUE_KEY, value = cachedValuesManager.createCachedValue(new CachedValueProvider<List<Pair<Converter, Object>>>()
			{
				@Override
				public Result<List<Pair<Converter, Object>>> compute()
				{
					List<Pair<Converter, Object>> list = Lists.newLockFreeCopyOnWriteList();
					return Result
							.create(list, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT, domManager, ProjectRootManager.getInstance(project));
				}
			}, false));
		}

		return getOrCalcValue(handler, value.getValue());
	}

	@Nullable
	private Object getOrCalcValue(final DomInvocationHandler<?, ?> handler, final List<Pair<Converter, Object>> list)
	{
		if(!list.isEmpty())
		{
			//noinspection ForLoopReplaceableByForEach
			for(int i = 0; i < list.size(); i++)
			{
				Pair<Converter, Object> pair = list.get(i);
				if(pair.first == myConverter)
				{
					return pair.second;
				}
			}
		}
		final Object returnValue = getValueInner(handler, myConverter);
		list.add(Pair.create(myConverter, returnValue));
		return returnValue;
	}

	@Nullable
	private static Object getValueInner(DomInvocationHandler<?, ?> handler, Converter converter)
	{
		final SubTag annotation = handler.getAnnotation(SubTag.class);
		if(annotation != null && annotation.indicator())
		{
			final boolean tagNotNull = handler.getXmlTag() != null;
			if(converter == Converter.EMPTY_CONVERTER)
			{
				return tagNotNull ? "" : null;
			}
			else
			{
				return tagNotNull;
			}
		}

		SimpleReference<String> tagValue = SimpleReference.create(handler.getValue());

		ConvertContext context = ConvertContextFactory.createConvertContext(handler);

		DomReferenceInjector.walk(context.getProject(), DomUtil.getFileElement(handler).getFileDescription(), each ->
		{
			tagValue.set(each.resolveString(tagValue.get(), context));
		});

		return converter.fromString(tagValue.get(), context);
	}
}
