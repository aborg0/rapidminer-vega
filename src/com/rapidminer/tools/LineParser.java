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

import com.rapidminer.operator.OperatorException;

/**
 * @author Tobias Malbrecht, Marco Boeck
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

	private Pattern splitPattern = Pattern.compile(DEFAULT_SPLIT_EXPRESSION);

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
		return splitPattern.toString();
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
	
	public void setSplitExpression(String splitExpression) throws OperatorException {
		try {
			this.splitPattern = Pattern.compile(splitExpression);
		} catch (PatternSyntaxException e) {
			throw new OperatorException("Malformed split expression: " + splitExpression);
		}
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
		if (splitPattern == null) {
			return new String[] { line };
		}
		if (useQuotes) {
			if (splitPattern.toString().length() > 1) {
				return split(line, splitPattern, trimLine, quoteCharacter, quoteEscapeCharacter);
			} else {
				return fastSplit(line, splitPattern.toString().charAt(0), trimLine, quoteCharacter, quoteEscapeCharacter);
			}
		} else {
			return split(line, splitPattern, trimLine);
		}
	}
	
	public static String[] split(String line, Pattern splitPattern, boolean trimLine) {
		String[] splittedString = splitPattern.split(trimLine ? line.trim() : line);
		return splittedString;
	}
	
	public static String[] split(String line, Pattern splitPattern, boolean trimLine, char quoteCharacter, char quoteEscapeCharacter) {
		String s = Tools.escapeQuoteCharsInQuotes(trimLine ? line.trim() : line, splitPattern, quoteCharacter, quoteEscapeCharacter, true);
		return Tools.quotedSplit(trimLine ? s.trim() : s, splitPattern, quoteCharacter, quoteEscapeCharacter);		
	}
	
	/**
	 * Splits the given line at each split character which is not in quotes and not following an escape character.
	 * 
	 * @param line the string to be splitted
	 * @param splitChar the character which seperates the values, e.g. for the line {@code value1;"30.545";value3} it would be ';'
	 * @param trimLine true if preceding and appending whitespaces of the line should be removed; false otherwise
	 * @param quoteChar the character used for value quotes, e.g. for the line {@code value1;"30.545";value3} it would be '"'
	 * @param escapeChar the character used to escape the following character. The character following an escape character will always be part of the result
	 * 			and will not be used as a quote or split character. For example, if set to '\', the line {@code val\;ue1;"30.545";value3} would result in
	 * 			[val;ue1],[30.545],[value3]
	 * @return an array with the splitted strings
	 */
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
				try {
					nextChar = line.charAt(i+1);
				} catch (IndexOutOfBoundsException e) {
					nextChar = null;
				}
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
					resultList.add(tempString.toString().trim());
					machineState = SplitMachineState.NEW_SPLIT;
					continue;
				}
				if (currentChar == escapeChar) {
					if (nextChar == null) {
						// special case: escape char followed by EndOfLine -> string read so far w/o escape char
						resultList.add(tempString.toString().trim());
						machineState = SplitMachineState.END_OF_LINE;
						continue;
					}
					// next character was escaped, therefore add it to the string and bypass it in loop
					tempString.append(nextChar);
					i++;
					continue;
				}
				if (currentChar == quoteChar) {
					// error handling
					errorMessage = "Value quote misplaced";
					errorColumnIndex = i;
					if (tempString.length() < 10) {
						StringBuffer errorCharBuf = new StringBuffer();
						errorCharBuf.append(tempString);
						if (errorCharBuf.length() > 0) {
							errorCharBuf.insert(0, splitChar);
						}
						for (int j=errorCharBuf.length(), k = resultList.size()-1;j<20;j++, k--) {
							if (k < 0) {
								break;
							}
							errorCharBuf.insert(0, resultList.get(k));
							if (errorCharBuf.length() < 18) {
								errorCharBuf.insert(0, splitChar);
							}
							j = errorCharBuf.length();
						}
						errorCharBuf.reverse();
						errorCharBuf.setLength(19);
						errorCharBuf.reverse();
						errorCharBuf.append(currentChar);
						errorLastFewReadChars = errorCharBuf.toString();
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
							StringBuffer errorCharBuf = new StringBuffer();
							errorCharBuf.append(tempString);
							if (errorCharBuf.length() > 0) {
								errorCharBuf.insert(0, splitChar);
							}
							for (int j=errorCharBuf.length(), k = resultList.size()-1;j<20;j++, k--) {
								if (k < 0) {
									break;
								}
								errorCharBuf.insert(0, resultList.get(k));
								if (errorCharBuf.length() < 18) {
									errorCharBuf.insert(0, splitChar);
								}
								j = errorCharBuf.length();
							}
							errorCharBuf.reverse();
							errorCharBuf.setLength(19);
							errorCharBuf.reverse();
							errorCharBuf.append(currentChar);
							errorLastFewReadChars = errorCharBuf.toString();
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
							StringBuffer errorCharBuf = new StringBuffer();
							errorCharBuf.append(tempString);
							if (errorCharBuf.length() > 0) {
								errorCharBuf.insert(0, splitChar);
							}
							for (int j=errorCharBuf.length(), k = resultList.size()-1;j<20;j++, k--) {
								if (k < 0) {
									break;
								}
								errorCharBuf.insert(0, resultList.get(k));
								if (errorCharBuf.length() < 18) {
									errorCharBuf.insert(0, splitChar);
								}
								j = errorCharBuf.length();
							}
							errorCharBuf.reverse();
							errorCharBuf.setLength(19);
							errorCharBuf.reverse();
							errorCharBuf.append(currentChar);
							errorLastFewReadChars = errorCharBuf.toString();
						} else {
							errorLastFewReadChars = tempString.substring(tempString.length()-9).toString() + currentChar;
						}
						machineState = SplitMachineState.ERROR;
						// needs to be thrown here as the loop exists after this pass
						throw new IllegalArgumentException(errorMessage + " at position " + i + ". Last characters read: " + errorLastFewReadChars);
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
					// remove quotes
					if (tempString.charAt(0) == quoteChar && tempString.charAt(tempString.length()-1) == quoteChar) {
						resultList.add(tempString.substring(1, tempString.length()-1));
					} else {
						// this should not occur, malformed quotes should be caught earlier
						resultList.add(tempString.toString());
					}
					
					machineState = SplitMachineState.NEW_SPLIT;
					continue;
				}
				if (currentChar == ' ' || currentChar == '\t') {
					// delete whitespaces after closing quotes
					continue;
				}
				// error handling
				errorMessage = "Unexpected character after closed value quote";
				errorColumnIndex = i;
				if (tempString.length() < 10) {
					StringBuffer errorCharBuf = new StringBuffer();
					errorCharBuf.append(tempString);
					if (errorCharBuf.length() > 0) {
						errorCharBuf.insert(0, splitChar);
					}
					for (int j=errorCharBuf.length(), k = resultList.size()-1;j<20;j++, k--) {
						if (k < 0) {
							break;
						}
						errorCharBuf.insert(0, resultList.get(k));
						if (errorCharBuf.length() < 18) {
							errorCharBuf.insert(0, splitChar);
						}
						j = errorCharBuf.length();
					}
					errorCharBuf.reverse();
					errorCharBuf.setLength(19);
					errorCharBuf.reverse();
					errorCharBuf.append(currentChar);
					errorLastFewReadChars = errorCharBuf.toString();
				} else {
					errorLastFewReadChars = tempString.substring(tempString.length()-9).toString() + currentChar;
				}
				// needs to be thrown here as the loop exists after this pass
				throw new IllegalArgumentException(errorMessage + " at position " + i + ". Last characters read: " + errorLastFewReadChars);
			case ERROR:
				throw new IllegalArgumentException(errorMessage + " at position " + i + ". Last characters read: " + errorLastFewReadChars);
			}
		}
		
		// last string handling
		// error state, malformed quote
		if (machineState == SplitMachineState.QUOTE_OPENED || machineState == SplitMachineState.WRITE_QUOTE) {
			errorMessage = "Value quotes not closed";
			errorColumnIndex = line.length()-1;
			if (tempString.length() < 10) {
				StringBuffer errorCharBuf = new StringBuffer();
				errorCharBuf.append(tempString);
				if (errorCharBuf.length() > 0) {
					errorCharBuf.insert(0, splitChar);
				}
				for (int j=errorCharBuf.length(), k = resultList.size()-1;j<20;j++, k--) {
					if (k < 0) {
						break;
					}
					errorCharBuf.insert(0, resultList.get(k));
					if (errorCharBuf.length() < 18) {
						errorCharBuf.insert(0, splitChar);
					}
					j = errorCharBuf.length();
				}
				errorCharBuf.reverse();
				errorCharBuf.setLength(20);
				errorCharBuf.reverse();
				errorLastFewReadChars = errorCharBuf.toString();
			} else {
				errorLastFewReadChars = tempString.substring(tempString.length()-10).toString();
			}
			throw new IllegalArgumentException(errorMessage + " at position " + errorColumnIndex + ". Last characters read: " + errorLastFewReadChars);
		} else {
			// add the last string to the list
			if (tempString.length() > 0) {
				// remove quotes if state QUOTE_CLOSED was reached
				if (machineState == SplitMachineState.QUOTE_CLOSED && tempString.charAt(0) == quoteChar && tempString.charAt(tempString.length()-1) == quoteChar) {
					resultList.add(tempString.substring(1, tempString.length()-1));
				} else {
					// this should not occur, malformed quotes should be caught earlier
					resultList.add(tempString.toString());
				}
			}
		}
		
		String[] resultArray = new String[resultList.size()];
		resultList.toArray(resultArray);
		
		return resultArray;
	}
}
