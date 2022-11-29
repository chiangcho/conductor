/*
 * Copyright 2022 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.tasks.http;

public class BooleanUtils {

    public static boolean toBoolean(final Boolean bool) {
        return bool != null && bool.booleanValue();
    }

    public static boolean toBoolean(final int value) {
        return value != 0;
    }

    public static Boolean toBooleanObject(final String str) {
        // Previously used equalsIgnoreCase, which was fast for interned 'true'.
        // Non interned 'true' matched 15 times slower.
        //
        // Optimisation provides same performance as before for interned 'true'.
        // Similar performance for null, 'false', and other strings not length 2/3/4.
        // 'true'/'TRUE' match 4 times slower, 'tRUE'/'True' 7 times slower.
        if (str == "true") {
            return Boolean.TRUE;
        }
        if (str == null) {
            return null;
        }
        switch (str.length()) {
            case 1:
                {
                    final char ch0 = str.charAt(0);
                    if (ch0 == 'y' || ch0 == 'Y' || ch0 == 't' || ch0 == 'T') {
                        return Boolean.TRUE;
                    }
                    if (ch0 == 'n' || ch0 == 'N' || ch0 == 'f' || ch0 == 'F') {
                        return Boolean.FALSE;
                    }
                    break;
                }
            case 2:
                {
                    final char ch0 = str.charAt(0);
                    final char ch1 = str.charAt(1);
                    if ((ch0 == 'o' || ch0 == 'O') && (ch1 == 'n' || ch1 == 'N')) {
                        return Boolean.TRUE;
                    }
                    if ((ch0 == 'n' || ch0 == 'N') && (ch1 == 'o' || ch1 == 'O')) {
                        return Boolean.FALSE;
                    }
                    break;
                }
            case 3:
                {
                    final char ch0 = str.charAt(0);
                    final char ch1 = str.charAt(1);
                    final char ch2 = str.charAt(2);
                    if ((ch0 == 'y' || ch0 == 'Y')
                            && (ch1 == 'e' || ch1 == 'E')
                            && (ch2 == 's' || ch2 == 'S')) {
                        return Boolean.TRUE;
                    }
                    if ((ch0 == 'o' || ch0 == 'O')
                            && (ch1 == 'f' || ch1 == 'F')
                            && (ch2 == 'f' || ch2 == 'F')) {
                        return Boolean.FALSE;
                    }
                    break;
                }
            case 4:
                {
                    final char ch0 = str.charAt(0);
                    final char ch1 = str.charAt(1);
                    final char ch2 = str.charAt(2);
                    final char ch3 = str.charAt(3);
                    if ((ch0 == 't' || ch0 == 'T')
                            && (ch1 == 'r' || ch1 == 'R')
                            && (ch2 == 'u' || ch2 == 'U')
                            && (ch3 == 'e' || ch3 == 'E')) {
                        return Boolean.TRUE;
                    }
                    break;
                }
            case 5:
                {
                    final char ch0 = str.charAt(0);
                    final char ch1 = str.charAt(1);
                    final char ch2 = str.charAt(2);
                    final char ch3 = str.charAt(3);
                    final char ch4 = str.charAt(4);
                    if ((ch0 == 'f' || ch0 == 'F')
                            && (ch1 == 'a' || ch1 == 'A')
                            && (ch2 == 'l' || ch2 == 'L')
                            && (ch3 == 's' || ch3 == 'S')
                            && (ch4 == 'e' || ch4 == 'E')) {
                        return Boolean.FALSE;
                    }
                    break;
                }
            default:
                break;
        }

        return null;
    }

    public static boolean toBoolean(final String str) {
        return toBooleanObject(str) == Boolean.TRUE;
    }
}
