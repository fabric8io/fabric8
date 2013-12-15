/*******************************************************************************
 * Copyright (c) 2013 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ralf Sternberg - initial implementation and API
 ******************************************************************************/
package io.fabric8.utils.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class JsonReader {

    public static Object read(Reader reader) throws IOException {
        return new JsonReader(reader).parse();
    }

    public static Object read(InputStream is) throws IOException {
        return new JsonReader(new InputStreamReader(is)).parse();
    }

    //
    // Implementation
    //

    private final Reader reader;
    private final StringBuilder recorder;
    private int current;
    private int line = 1;
    private int column = 0;

    JsonReader(Reader reader) {
        this.reader = reader;
        recorder = new StringBuilder();
    }

    public Object parse() throws IOException {
        read();
        skipWhiteSpace();
        Object result = readValue();
        skipWhiteSpace();
        if (!endOfText()) {
            throw error("Unexpected character");
        }
        return result;
    }

    private Object readValue() throws IOException {
        switch (current) {
            case 'n':
                return readNull();
            case 't':
                return readTrue();
            case 'f':
                return readFalse();
            case '"':
                return readString();
            case '[':
                return readArray();
            case '{':
                return readObject();
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return readNumber();
            default:
                throw expected("value");
        }
    }

    private Collection<?> readArray() throws IOException {
        read();
        Collection<Object> array = new ArrayList<Object>();
        skipWhiteSpace();
        if (readChar(']')) {
            return array;
        }
        do {
            skipWhiteSpace();
            array.add(readValue());
            skipWhiteSpace();
        } while (readChar(','));
        if (!readChar(']')) {
            throw expected("',' or ']'");
        }
        return array;
    }

    private Map<String, Object> readObject() throws IOException {
        read();
        Map<String, Object> object = new HashMap<String, Object>();
        skipWhiteSpace();
        if (readChar('}')) {
            return object;
        }
        do {
            skipWhiteSpace();
            String name = readName();
            skipWhiteSpace();
            if (!readChar(':')) {
                throw expected("':'");
            }
            skipWhiteSpace();
            object.put(name, readValue());
            skipWhiteSpace();
        } while (readChar(','));
        if (!readChar('}')) {
            throw expected("',' or '}'");
        }
        return object;
    }

    private Object readNull() throws IOException {
        read();
        readRequiredChar('u');
        readRequiredChar('l');
        readRequiredChar('l');
        return null;
    }

    private Boolean readTrue() throws IOException {
        read();
        readRequiredChar('r');
        readRequiredChar('u');
        readRequiredChar('e');
        return Boolean.TRUE;
    }

    private Boolean readFalse() throws IOException {
        read();
        readRequiredChar('a');
        readRequiredChar('l');
        readRequiredChar('s');
        readRequiredChar('e');
        return Boolean.FALSE;
    }

    private void readRequiredChar(char ch) throws IOException {
        if (!readChar(ch)) {
            throw expected("'" + ch + "'");
        }
    }

    private String readString() throws IOException {
        read();
        recorder.setLength(0);
        while (current != '"') {
            if (current == '\\') {
                readEscape();
            } else if (current < 0x20) {
                throw expected("valid string character");
            } else {
                recorder.append((char) current);
                read();
            }
        }
        read();
        return recorder.toString();
    }

    private void readEscape() throws IOException {
        read();
        switch (current) {
            case '"':
            case '/':
            case '\\':
                recorder.append((char) current);
                break;
            case 'b':
                recorder.append('\b');
                break;
            case 'f':
                recorder.append('\f');
                break;
            case 'n':
                recorder.append('\n');
                break;
            case 'r':
                recorder.append('\r');
                break;
            case 't':
                recorder.append('\t');
                break;
            case 'u':
                char[] hexChars = new char[4];
                for (int i = 0; i < 4; i++) {
                    read();
                    if (!isHexDigit(current)) {
                        throw expected("hexadecimal digit");
                    }
                    hexChars[i] = (char) current;
                }
                recorder.append((char) Integer.parseInt(String.valueOf(hexChars), 16));
                break;
            default:
                throw expected("valid escape sequence");
        }
        read();
    }

    private Number readNumber() throws IOException {
        recorder.setLength(0);
        readAndAppendChar('-');
        int firstDigit = current;
        if (!readAndAppendDigit()) {
            throw expected("digit");
        }
        if (firstDigit != '0') {
            while (readAndAppendDigit()) {
            }
        }
        readFraction();
        readExponent();
        return Double.parseDouble(recorder.toString());
    }

    private boolean readFraction() throws IOException {
        if (!readAndAppendChar('.')) {
            return false;
        }
        if (!readAndAppendDigit()) {
            throw expected("digit");
        }
        while (readAndAppendDigit()) {
        }
        return true;
    }

    private boolean readExponent() throws IOException {
        if (!readAndAppendChar('e') && !readAndAppendChar('E')) {
            return false;
        }
        if (!readAndAppendChar('+')) {
            readAndAppendChar('-');
        }
        if (!readAndAppendDigit()) {
            throw expected("digit");
        }
        while (readAndAppendDigit()) {
        }
        return true;
    }

    private String readName() throws IOException {
        if (current != '"') {
            throw expected("name");
        }
        readString();
        return recorder.toString();
    }

    private boolean readAndAppendChar(char ch) throws IOException {
        if (current != ch) {
            return false;
        }
        recorder.append(ch);
        read();
        return true;
    }

    private boolean readChar(char ch) throws IOException {
        if (current != ch) {
            return false;
        }
        read();
        return true;
    }

    private boolean readAndAppendDigit() throws IOException {
        if (!isDigit(current)) {
            return false;
        }
        recorder.append((char) current);
        read();
        return true;
    }

    private void skipWhiteSpace() throws IOException {
        while (isWhiteSpace(current) && !endOfText()) {
            read();
        }
    }

    private void read() throws IOException {
        if (endOfText()) {
            throw error("Unexpected end of input");
        }
        column++;
        if (current == '\n') {
            line++;
            column = 0;
        }
        current = reader.read();
    }

    private boolean endOfText() {
        return current == -1;
    }

    private IOException expected(String expected) {
        if (endOfText()) {
            return error("Unexpected end of input");
        }
        return error("Expected " + expected);
    }

    private IOException error(String message) {
        return new IOException(message + " at " + line + ":" + column);
    }

    private static boolean isWhiteSpace(int ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
    }

    private static boolean isDigit(int ch) {
        return ch >= '0' && ch <= '9';
    }

    private static boolean isHexDigit(int ch) {
        return ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F';
    }

}
