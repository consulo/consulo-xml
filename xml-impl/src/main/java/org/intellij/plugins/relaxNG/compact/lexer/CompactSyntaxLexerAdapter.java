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

package org.intellij.plugins.relaxNG.compact.lexer;

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.lexer.LexerBase;
import consulo.logging.Logger;
import consulo.util.collection.primitive.ints.IntIntMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.lang.CharArrayCharSequence;
import consulo.util.lang.CharArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.plugins.relaxNG.compact.RncTokenTypes;
import org.kohsuke.rngom.parse.compact.CompactSyntaxConstants;
import org.kohsuke.rngom.parse.compact.CompactSyntaxTokenManager;
import org.kohsuke.rngom.parse.compact.Token;
import org.kohsuke.rngom.parse.compact.TokenMgrError;

import java.io.CharArrayReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.LinkedList;

/**
 * An adapter to use the lexer ("TokenManager") generated from a javacc grammar.
 * <p>
 * Not sure if it was easier to write this than hacking my own lexer...
 */
public class CompactSyntaxLexerAdapter extends LexerBase {
    private static final Logger LOG = Logger.getInstance(CompactSyntaxLexerAdapter.class);

    private static final Token START = new Token();

    private CompactSyntaxTokenManager myLexer;
    private final LinkedList<Token> myTokenQueue = new LinkedList<>();
    private Token myCurrentToken;
    private int myCurrentOffset;
    private int myCurrentEnd;

    private IElementType myCurrentTokenType;
    private CharSequence myBuffer;
    private int myEndOffset;
    private IntIntMap myLengthMap;

    @Override
    public void advance() {
        try {
            myCurrentToken = nextToken();
            myCurrentOffset = myCurrentEnd;

            if (myCurrentToken != null) {

                myCurrentEnd = myCurrentOffset + myCurrentToken.image.length();
                for (int i = myCurrentOffset; i < myCurrentEnd; i++) {
                    myCurrentEnd += myLengthMap.getInt(i);
                }

                if (myCurrentToken.kind == CompactSyntaxConstants.EOF) {
                    assert myCurrentOffset == myEndOffset : "actual: " + myCurrentOffset + ", expected: " + myEndOffset;
                    myCurrentToken = null;
                }
            }

            //      if (myCurrentToken != null) {
            //        System.out.println("token = <" + RncTokenTypes.get(myCurrentToken.kind).toString() + "> [" + myCurrentToken.image + "]");
            //      }
        }
        catch (TokenMgrError e) {
            LOG.error(e);
            myCurrentToken = null;
        }

        if (myCurrentToken == null) {
            myCurrentTokenType = null;
        }
        else {
            myCurrentTokenType = RncTokenTypes.get(myCurrentToken.kind);

            // collapse whitespace tokens into TokenType.WHITE_SPACE [IDEA-12106]
            if (RncTokenTypes.WHITESPACE.contains(myCurrentTokenType)) {
                myCurrentTokenType = TokenType.WHITE_SPACE;
            }
        }
    }

    private Token nextToken() {
        if (myTokenQueue.size() > 0) {
            return myTokenQueue.removeFirst();
        }

        final Token t = myLexer.getNextToken();
        if (t.specialToken != null) {
            myTokenQueue.addFirst(t);
            for (Token s = t.specialToken; s != null; s = s.specialToken) {
                myTokenQueue.addFirst(s);
            }
            return myTokenQueue.removeFirst();
        }
        else {
            return t;
        }
    }

    @Deprecated
    public char[] getBuffer() {
        return CharArrayUtil.fromSequence(myBuffer);
    }

    @Nonnull
    @Override
    public CharSequence getBufferSequence() {
        return myBuffer;
    }

    @Override
    public int getBufferEnd() {
        return myEndOffset;
    }

    @Override
    public int getState() {
        return myLexer.getCurLexState();
    }

    @Override
    public int getTokenEnd() {
        return myCurrentEnd;
    }

    @Override
    public int getTokenStart() {
        return myCurrentToken == null ? 0 : myCurrentOffset;
    }

    @Override
    @Nullable
    public IElementType getTokenType() {
        if (myCurrentToken == null) {
            return null;
        }
        else {
            return myCurrentTokenType;
        }
    }

    @Deprecated
    public void start(char[] buffer, int startOffset, int endOffset, int initialState) {
        myBuffer = new CharArrayCharSequence(buffer, startOffset, endOffset);

        final CharArrayReader reader = new CharArrayReader(buffer, startOffset, endOffset - startOffset);
        init(startOffset, endOffset, reader, initialState);
    }

    @Override
    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    public void start(@Nonnull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        myBuffer = buffer;

        final Reader reader = new CharSequenceReader(buffer, startOffset, endOffset);
        init(startOffset, endOffset, reader, initialState);
    }

    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    private void init(int startOffset, int endOffset, Reader reader, int initialState) {
        myEndOffset = endOffset;
        myLengthMap = IntMaps.newIntIntHashMap();

        myLexer = createTokenManager(initialState, new EscapePreprocessor(reader, startOffset, myLengthMap));

        myCurrentToken = START;
        myCurrentOffset = startOffset;
        myCurrentEnd = startOffset;
        myTokenQueue.clear();
        advance();
    }

    private static CompactSyntaxTokenManager createTokenManager(int initialState, EscapePreprocessor preprocessor) {
        return new CompactSyntaxTokenManager(new SimpleCharStream(preprocessor, 1, 1), initialState);
    }

    // adapted from com.intellij.util.text.CharSequenceReader with start- and endOffset support
    static class CharSequenceReader extends Reader {
        private final CharSequence myText;
        private final int myEndOffset;
        private int myCurPos;

        public CharSequenceReader(final CharSequence text, int startOffset, int endOffset) {
            myText = text;
            myEndOffset = endOffset;
            myCurPos = startOffset;
        }

        @Override
        public void close() {
        }

        @Override
        public int read(char[] cbuf, int off, int len) {
            if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }
            else if (len == 0) {
                return 0;
            }

            if (myText instanceof CharArrayCharSequence) { // Optimization
                final int readChars = ((CharArrayCharSequence) myText).readCharsTo(myCurPos, cbuf, off, len);
                if (readChars < 0) {
                    return -1;
                }
                myCurPos += readChars;
                return readChars;
            }

            int charsToCopy = Math.min(len, myEndOffset - myCurPos);
            if (charsToCopy <= 0) {
                return -1;
            }

            for (int n = 0; n < charsToCopy; n++) {
                cbuf[n + off] = myText.charAt(n + myCurPos);
            }

            myCurPos += charsToCopy;
            return charsToCopy;
        }

        @Override
        public int read() {
            if (myCurPos >= myEndOffset) {
                return -1;
            }
            return myText.charAt(myCurPos++);
        }
    }
}
