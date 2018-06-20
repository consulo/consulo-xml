package com.intellij.util.xml;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import com.intellij.util.xml.converters.DelimitedListProcessor;

/**
 * @author Dmitry Avdeev
 */
public class DelimitedListProcessorTest
{
	@Test
	public void testProcessor()
	{
		doTest("a; ; ", Arrays.asList("a", " ", " "));
	}

	private void doTest(final String text, final List<String> expected)
	{
		final ArrayList<String> tokens = new ArrayList<String>();
		new DelimitedListProcessor(";")
		{
			@Override
			protected void processToken(final int start, final int end, final boolean delimitersOnly)
			{
				tokens.add(text.substring(start, end));
			}
		}.processText(text);
		assertEquals(expected, tokens);
	}
}
