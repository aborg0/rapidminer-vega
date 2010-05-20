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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Tobias Malbrecht
 */
public class LineParser {
	
	private enum SplitMachineState {
		NEW_SPLIT,
		START_WHITESPACE,
		WRITE_NOT_QUOTE,
		QUOTE_OPENED,
		QUOTE_CLOSED,
		WRITE_QUOTE,
		ERROR,
		END_OF_LINE
	}

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
			if (splitExpression.length() > 1) {
				return split(line, splitExpression, trimLine, quoteCharacter, quoteEscapeCharacter);
			} else {
				return fastSplit(line, splitExpression.charAt(0), trimLine, quoteCharacter, quoteEscapeCharacter);
			}
		} else {
			return split(line, splitExpression, trimLine);
		}
	}
	
	public static String[] split(String line, String splitExpression, boolean trimLine) {
		String[] splittedString = trimLine ? line.trim().split(splitExpression) : line.split(splitExpression);
		return splittedString;
	}
	
	public static String[] split(String line, String splitExpression, boolean trimLine, char quoteCharacter, char quoteEscapeCharacter) {
		String s = Tools.escapeQuoteCharsInQuotes(trimLine ? line.trim() : line, Pattern.compile(splitExpression), quoteCharacter, quoteEscapeCharacter, true);
		return Tools.quotedSplit(trimLine ? s.trim() : s, Pattern.compile(splitExpression), quoteCharacter, quoteEscapeCharacter);		
	}
	
	public static String[] fastSplit(String line, char splitChar, boolean trimLine, char quoteChar, char escapeChar) {
		List<String> resultList = new ArrayList<String>();
		/** holding the temporary split string */
		StringBuffer tempString = new StringBuffer();
		/** character read in iteration i */
		Character currentChar;
		/** character which would be read in iteration i+1 */
		Character nextChar;
		/** error message to display */
		String errorMessage = "";
		/** column index where the error occured */
		int errorColumnIndex = 0;
		/** string with the last 10 characters read before the error occured */
		String errorLastFewReadChars = "";
		/** current state of the SplitMachine */
		SplitMachineState machineState = SplitMachineState.NEW_SPLIT;
		
		// trim wanted?
		line = trimLine  ? line.trim() : line;
		// go through the line
		for (int i = 0; i<line.length(); i++) {
			// read current character and next character (if applicaple)
			currentChar = line.charAt(i);
			if (i<line.length()-1) {
				nextChar = line.charAt(i+1);
			} else {
				nextChar = null;
			}
			// run through our split machine
			switch(machineState) {
			case NEW_SPLIT:
				tempString = new StringBuffer();
				if (currentChar == splitChar) {
					resultList.add("");
					continue;
				}
				if (currentChar == ' ' || currentChar == '\t') {
					machineState = SplitMachineState.START_WHITESPACE;
					continue;
				}
				if (currentChar == quoteChar) {
					tempString.append(currentChar);
					machineState = SplitMachineState.QUOTE_OPENED;
					continue;
				}
				if (currentChar == escapeChar) {
					if (nextChar == null) {
						// special case: escape char followed by EndOfLine -> empty value
						resultList.add("");
						machineState = SplitMachineState.NEW_SPLIT;
						continue;
					}
					// next character was escaped, therefore add it to the string and bypass it in loop
					tempString.append(nextChar);
					i++;
					machineState = SplitMachineState.WRITE_NOT_QUOTE;
					continue;
				}
				tempString.append(currentChar);
				machineState = SplitMachineState.WRITE_NOT_QUOTE;
				continue;
			case START_WHITESPACE:
				if (currentChar == splitChar) {
					resultList.add("");
					machineState = SplitMachineState.NEW_SPLIT;
					continue;
				}
				if (currentChar == ' ' || currentChar == '\t') {
					continue;
				}
				if (currentChar == quoteChar) {
					tempString.append(currentChar);
					machineState = SplitMachineState.QUOTE_OPENED;
					continue;
				}
				if (currentChar == escapeChar) {
					if (nextChar == null) {
						// special case: escape char followed by EndOfLine -> empty value
						resultList.add("");
						machineState = SplitMachineState.END_OF_LINE;
						continue;
					}
					// next character was escaped, therefore add it to the string and bypass it in loop
					tempString.append(nextChar);
					i++;
					machineState = SplitMachineState.WRITE_NOT_QUOTE;
					continue;
				}
				tempString.append(currentChar);
				machineState = SplitMachineState.WRITE_NOT_QUOTE;
				continue;
			case WRITE_NOT_QUOTE:
				if (currentChar == splitChar) {
					resultList.add(tempString.toString());
					machineState = SplitMachineState.NEW_SPLIT;
					continue;
				}
				if (currentChar == escapeChar) {
					if (nextChar == null) {
						// special case: escape char followed by EndOfLine -> string read so far w/o escape char
						resultList.add(tempString.toString());
						machineState = SplitMachineState.END_OF_LINE;
						continue;
					}
					// next character was escaped, therefore add it to the string and bypass it in loop
					tempString.append(nextChar);
					i++;
					continue;
				}
				if (currentChar == quoteChar) {
					errorMessage = "Value quote misplaced";
					errorColumnIndex = i;
					if (tempString.length() < 10) {
						errorLastFewReadChars = tempString.toString() + currentChar;
					} else {
						errorLastFewReadChars = tempString.substring(tempString.length()-9).toString() + currentChar;
					}
					machineState = SplitMachineState.ERROR;
					continue;
				}
				tempString.append(currentChar);
				continue;
			case END_OF_LINE:
				// nothing to do here
				break;
			case QUOTE_OPENED:
				if (currentChar == quoteChar) {
					tempString.append(currentChar);
					machineState = SplitMachineState.QUOTE_CLOSED;
					continue;
				}
				if (currentChar == escapeChar) {
					if (nextChar == null) {
						// special case: quote char followed by escape char followed by EndOfLine -> error
						errorMessage = "Value quotes malformed";
						errorColumnIndex = i;
						if (tempString.length() < 10) {
							errorLastFewReadChars = tempString.toString() + currentChar;
						} else {
							errorLastFewReadChars = tempString.substring(tempString.length()-9).toString() + currentChar;
						}
						machineState = SplitMachineState.ERROR;
						continue;
					}
					// next character was escaped, therefore add it to the string and bypass it in loop
					tempString.append(nextChar);
					i++;
					machineState = SplitMachineState.WRITE_QUOTE;
					continue;
				}
				tempString.append(currentChar);
				machineState = SplitMachineState.WRITE_QUOTE;
				continue;
			case WRITE_QUOTE:
				if (currentChar == quoteChar) {
					tempString.append(currentChar);
					machineState = SplitMachineState.QUOTE_CLOSED;
					continue;
				}
				if (currentChar == escapeChar) {
					if (nextChar == null) {
						// special case: quote char followed by char* followed by escape char followed by EndOfLine -> error
						errorMessage = "Value quotes malformed";
						errorColumnIndex = i;
						if (tempString.length() < 10) {
							errorLastFewReadChars = tempString.toString() + currentChar;
						} else {
							errorLastFewReadChars = tempString.substring(tempString.length()-9).toString() + currentChar;
						}
						machineState = SplitMachineState.ERROR;
						continue;
					}
					// next character was escaped, therefore add it to the string and bypass it in loop
					tempString.append(nextChar);
					i++;
					continue;
				}
				tempString.append(currentChar);
				continue;
			case QUOTE_CLOSED:
				if (currentChar == splitChar) {
					resultList.add(tempString.toString());
					machineState = SplitMachineState.NEW_SPLIT;
					continue;
				}
				if (currentChar == ' ' || currentChar == '\t') {
					continue;
				}
				errorMessage = "Unexpected character after closed value quote";
				errorColumnIndex = i;
				if (tempString.length() < 10) {
					errorLastFewReadChars = tempString.toString() + currentChar;
				} else {
					errorLastFewReadChars = tempString.substring(tempString.length()-9).toString() + currentChar;
				}
				machineState = SplitMachineState.ERROR;
				continue;
			case ERROR:
				throw new IllegalArgumentException(errorMessage + " at position " + i + ". Last characters read: " + errorLastFewReadChars);
			}
		}
		
		// last string handling
		if (machineState == SplitMachineState.QUOTE_OPENED || machineState == SplitMachineState.WRITE_QUOTE) {
			errorMessage = "Value quotes not closed";
			errorColumnIndex = line.length()-1;
			if (tempString.length() < 10) {
				errorLastFewReadChars = tempString.toString();
			} else {
				errorLastFewReadChars = tempString.substring(tempString.length()-10).toString();
			}
			throw new IllegalArgumentException(errorMessage + " at position " + errorColumnIndex + ". Last characters read: " + errorLastFewReadChars);
		} else {
			if (tempString.length() > 0) {
				resultList.add(tempString.toString());
			}
		}
		for (String s : resultList) {
			s = s.trim();
			if (s.charAt(0) == quoteChar && s.charAt(s.length()-1) == quoteChar) {
				s = s.substring(1, s.length()-1);
				s.trim();
			}
		}
		String[] resultArray = new String[resultList.size()];
		resultList.toArray(resultArray);
		
		return resultArray;
	}
}
