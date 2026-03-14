/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hhoa.kline.plugins.jdbc.types;

/**
 * @author hhoa 2022/5/9
 */
public class ClassSizeUtil {
    public static final int COLUMN_ROW_DATA_SIZE;
    public static final int ABSTRACT_BASE_COLUMN_SIZE;
    public static final int STRING_COLUMN_SIZE;
    public static final int TIMESTAMP_COLUMN_SIZE;
    public static final int BYTES_COLUMN_SIZE;
    public static final int MAP_COLUMN_SIZE;

    public static final int ARRAY_SIZE;

    public static final int INT_SIZE;
    public static final int STRING_SIZE;
    public static final int BYTE_SIZE;
    public static final int FLOAT_SIZE;
    public static final int LONG_SIZE;

    // ignore padding
    static {
        int REFERENCE = 4;
        int HEAD = 12;

        INT_SIZE = Integer.SIZE / Byte.SIZE;
        FLOAT_SIZE = Float.SIZE / Byte.SIZE;
        LONG_SIZE = Long.SIZE / Byte.SIZE;
        // ignore char array
        STRING_SIZE = HEAD + INT_SIZE * 2 + HEAD + INT_SIZE;
        BYTE_SIZE = HEAD;

        ARRAY_SIZE = HEAD + INT_SIZE;

        // ignore element size
        int ArrayListSize = HEAD + INT_SIZE * 2 + REFERENCE + HEAD + INT_SIZE;

        // ignore table size
        int HashMapSize =
                HEAD + Integer.SIZE / Byte.SIZE * 4 + Float.SIZE / Byte.SIZE + REFERENCE * 3 + HEAD;

        int HashSetSize = HEAD + HashMapSize;

        int RowKindSize = HEAD + STRING_SIZE + 1 + 4;

        COLUMN_ROW_DATA_SIZE =
                HEAD + ArrayListSize + HashMapSize + HashSetSize + RowKindSize + REFERENCE * 4;
        ABSTRACT_BASE_COLUMN_SIZE = HEAD + REFERENCE + INT_SIZE;
        STRING_COLUMN_SIZE = ABSTRACT_BASE_COLUMN_SIZE + REFERENCE * 2;
        TIMESTAMP_COLUMN_SIZE = ABSTRACT_BASE_COLUMN_SIZE + INT_SIZE;
        BYTES_COLUMN_SIZE = ABSTRACT_BASE_COLUMN_SIZE + ARRAY_SIZE + REFERENCE;
        MAP_COLUMN_SIZE = ABSTRACT_BASE_COLUMN_SIZE + HashMapSize;
    }

    public static long align(int num) {
        // The 7 comes from that the alignSize is 8 which is the number of bytes stored and sent
        // together
        return (num >> 3) << 3;
    }

    public static int getStringMemory(String str) {
        if (str == null) {
            return 0;
        }
        return str.length() * 2 + STRING_SIZE;
    }

    public static int getStringSize(String str) {
        if (str == null) {
            return 0;
        }
        return str.length();
    }
}
