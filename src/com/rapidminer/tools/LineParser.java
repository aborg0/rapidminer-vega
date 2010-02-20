/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import java.nio.charset.Charset;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Tobias Malbrecht
 */
public class LineParser {

	public static final String DEFAULT_COMMENT_CHARACTER_STRING = "#";
	
	public static final String DEFAULT_SPLIT_EXPRESSION = ",\\s*|;\\s*";

	public static final String SPLIT_BY_TAB_EXPRESSION = "\t";

	public static final String SPLIT_BY_SPACE_EXPRESSION = "\\s";

	public static final String SPLIT_BY_COMMA_EXPRESSION = ",";

	public static final String SPLIT_BY_SEMICOLON_EXPRESSION = ";";

	public static final char DEFAULT_QUOTE_CHARACTER = '"';

	public static final char DEFAULT_QUOTE_ESCAPE_CHARACTER = '\\';
	
	private Charset encoding;
	
	private boolean skipComments = true;
	
	private String commentCharacterString = DEFAULT_COMMENT_CHARACTER_STRING;

	private String splitExpression = DEFAULT_SPLIT_EXPRESSION;

	private boolean useQuotes = true;

	private char quoteCharacter = DEFAULT_QUOTE_CHARACTER;

	private char quoteEscapeCharacter = DEFAULT_QUOTE_ESCAPE_CHARACTER;

	private boolean trimLine = true;
	
	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}
	
	public Charset getEncoding() {
		return encoding;
	}
	
	public boolean isTrimLine() {
		return trimLine;
	}

	public boolean isSkipComments() {
		return skipComments;
	}

	public String getSplitExpression() {
		return splitExpression;
	}
	
	public String getCommentCharacters() {
		return commentCharacterString;
	}

	public boolean isUseQuotes() {
		return useQuotes;
	}

	public char getQuoteCharacter() {
		return quoteCharacter;
	}

	public char getQuoteEscapeCharacter() {
		return quoteEscapeCharacter;
	}
	
	public void setTrimLine(boolean trimLine) {
		this.trimLine = trimLine;
	}
	
	public void setSkipComments(boolean skipComments) {
		this.skipComments = skipComments;
	}
	
	public void setCommentCharacters(String commentCharacters) {
		this.commentCharacterString = commentCharacters; 
	}
	
	public void setSplitExpression(String splitExpression) {
		this.splitExpression = splitExpression;
	}
	
	public void setUseQuotes(boolean useQuotes) {
		this.useQuotes = useQuotes;
	}
	
	public void setQuoteCharacter(char quoteCharacter) {
		this.quoteCharacter = quoteCharacter;
	}
	
	public void setQuoteEscapeCharacter(char quoteEscapeCharacter) {
		this.quoteEscapeCharacter = quoteEscapeCharacter;
	}
	
	public String[] parse(String line) {
		line = removeComment(line);
		if (line == null || "".equals(line.trim())) {
			return null;
		}
		return split(line);
	}
	
	public String removeComment(String line) {
		String resultingLine = line;
		if (skipComments) {
			for (int i = 0; i < commentCharacterString.length(); i++) {
				int commentCharacterIndex = line.indexOf(commentCharacterString.charAt(i)); 
				if (commentCharacterIndex >= 0) {
					resultingLine = line.substring(0, commentCharacterIndex);
					if (line.trim().length() == 0) {
						return null;
					}
				}
			}
		}
		return resultingLine;
	}

	public String[] split(String line) {
		if (splitExpression == null) {
			return new String[] { line };
		}
		try {
			Pattern.compile(splitExpression);
		} catch (PatternSyntaxException e) {
			return new String[] { line };
		}
		if (useQuotes) {
			return split(line, splitExpression, trimLine, quoteCharacter, quoteEscapeCharacter);
		} else {
			return split(line, splitExpression, trimLine);
		}
	}
	
	public static String[] split(String line, String splitExpression, boolean trimLine) {
		return trimLine ? line.trim().split(splitExpression) : line.split(splitExpression);		
	}
	
	public static String[] split(String line, String splitExpression, boolean trimLine, char quoteCharacter, char quoteEscapeCharacter) {
		return Tools.quotedSplit(trimLine ? line.trim() : line, Pattern.compile(splitExpression), quoteCharacter, quoteEscapeCharacter);		
	}
}
