/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.utils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A helper class to pretty print a table of values to the console
 */
public class TablePrinter {
    private List<Column> columns = new ArrayList<>();
    private Map<String, Column> columnMap = new HashMap<>();
    private String columnSeparator = "  ";
    private List<String[]> rows = new ArrayList<>();

    /**
     * Defines the columns of the table
     */
    public void columns(String... headers) {
        for (String header : headers) {
            // force lazy creation
            column(header);
        }
    }

    /**
     * Looks up the column using the header name and returns the column object so it
     * can be configured
     */
    public Column column(String header) {
        synchronized (columns) {
            Column answer = columnMap.get(header);
            if (answer == null) {
                answer = new Column(header);
                columns.add(answer);
                columnMap.put(header, answer);
            }
            return answer;
        }
    }

    /**
     * Looks up the column using its index; lazily creating one if required
     */
    public Column column(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        synchronized (columns) {
            Column answer = null;
            if (index < columns.size()) {
                answer = columns.get(index);
            }
            if (answer == null) {
                answer = new Column("");
                columns.add(answer);
            }
            return answer;
        }
    }

    /**
     * Adds a new row of values
     */
    public void row(String... values) {
        int columnIndex = 0;
        for (String value : values) {
            Column column = column(columnIndex++);
            if (value != null) {
                int length = value.length();
                column.ensureWidth(length);
            }
        }
        rows.add(values);
    }

    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        boolean first = true;
        for (Column column : columns) {
            if (first) {
                first = false;
            } else {
                out.print(columnSeparator);
            }
            column.printHeader(out);
        }
        out.println();
        for (String[] row : rows) {
            first = true;

            int i = 0;
            for (Column column : columns) {
                if (first) {
                    first = false;
                } else {
                    out.print(columnSeparator);
                }
                String value = null;
                if (i < row.length) {
                    value = row[i];
                }
                if (value == null) {
                    value = "";
                }
                i++;
                column.printValue(out, value);
            }
            out.println();
        }
    }

    public static class Column {
        private final String headerText;
        private String header;
        private String headerFlags = "-";
        private String rowFlags = "-";
        private int width = 1;

        public Column(String header) {
            this.header = header;
            this.headerText = "[" + header + "]";
            ensureWidth(headerText.length());
        }

        public String getHeader() {
            return header;
        }

        public String getHeaderFlags() {
            return headerFlags;
        }

        public void setHeaderFlags(String headerFlags) {
            this.headerFlags = headerFlags;
        }

        public String getRowFlags() {
            return rowFlags;
        }

        public void setRowFlags(String rowFlags) {
            this.rowFlags = rowFlags;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        /**
         * Ensures the column is wide enough for the given value
         */
        public void ensureWidth(int length) {
            if (length > width) {
                width = length;
            }

        }

        /**
         * Prints the header value
         */
        public void printHeader(PrintStream out) {
            String formatText = "%" + headerFlags + width + "s";
            out.printf(formatText, headerText);
        }

        /**
         * Prints a value
         */
        public void printValue(PrintStream out, String value) {
            String formatText = "%" + rowFlags + width + "s";
            out.printf(formatText, value);
        }
    }
}
