package consulo.xml.util.xml.impl;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * @author peter
 */
public class AddChildInvocation implements Invocation
{
	private final CollectionChildDescriptionImpl myDescription;
	private final Type myType;
	private final Function<Object[], Integer> myIndexGetter;
	private final Function<Object[], Type> myClassGetter;

	public AddChildInvocation(final Function<Object[], Type> classGetter,
							  final Function<Object[], Integer> indexGetter,
							  final CollectionChildDescriptionImpl tagName,
							  final Type type)
	{
		myClassGetter = classGetter;
		myIndexGetter = indexGetter;
		myDescription = tagName;
		myType = type;
	}

	public Object invoke(final DomInvocationHandler<?, ?> handler, final Object[] args) throws Throwable
	{
		return handler.addCollectionChild(myDescription, myClassGetter.apply(args), myIndexGetter.apply(args));
	}
}
