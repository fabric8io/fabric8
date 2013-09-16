/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.eca.expression;

import java.util.Collections;
import java.util.List;

import org.apache.camel.Exchange;
import org.fusesource.eca.eventcache.CacheItem;
import org.fusesource.eca.util.ParsingUtil;

/**
 * A filter performing a comparison of two objects
 */
public abstract class LogicExpression extends BinaryExpression {
    private long threshold = 0l;

    public LogicExpression(String threshold, Expression left, Expression right) {
        super(left, right);
        this.threshold = ParsingUtil.getTimeAsMilliseconds(threshold);
    }

    public static Expression createOR(String threshold, Expression lvalue, Expression rvalue) {
        return new LogicExpression(threshold, lvalue, rvalue) {

            public List<CacheItem<Exchange>> getMatching() {
                List<CacheItem<Exchange>> result = left != null ? left.getMatching() : null;
                List<CacheItem<Exchange>> rv = right != null ? right.getMatching() : null;
                if (result != null) {
                    if (rv != null) {
                        result.addAll(rv);
                    }
                } else {
                    result = rv;
                }
                if (result != null) {
                    Collections.sort(result);
                }
                return result;
            }

            public boolean isMatch() {
                if (left != null && left.isMatch()) {
                    return true;
                }
                if (right != null && right.isMatch()) {
                    return true;
                }
                return false;
            }

            public String getExpressionSymbol() {
                return "OR";
            }
        };
    }

    public static Expression createAND(String threshold, Expression lvalue, Expression rvalue) {
        return new LogicExpression(threshold, lvalue, rvalue) {

            public List<CacheItem<Exchange>> getMatching() {
                List<CacheItem<Exchange>> lv = left.getMatching();
                List<CacheItem<Exchange>> rv = right.getMatching();
                if (lv != null && rv != null) {
                    lv.addAll(rv);
                    Collections.sort(lv);
                    return lv;
                }
                return null;
            }

            public boolean isMatch() {
                if (left != null && left.isMatch()) {
                    return right != null ? right.isMatch() : false;
                }
                return false;
            }

            public String getExpressionSymbol() {
                return "AND";
            }
        };
    }

    public static Expression createBEFORE(String threshold, Expression lvalue, Expression rvalue) {
        return new LogicExpression(threshold, lvalue, rvalue) {

            public List<CacheItem<Exchange>> getMatching() {
                List<CacheItem<Exchange>> lv = left != null ? left.getMatching() : null;
                List<CacheItem<Exchange>> rv = right != null ? right.getMatching() : null;
                if (lv != null) {
                    if (rv == null) {
                        return lv;
                    }
                    return null;
                }
                return null;
            }

            public boolean isMatch() {
                boolean lv = left != null ? left.isMatch() : false;
                boolean rv = right != null ? right.isMatch() : false;
                if (lv && !rv) {
                    return true;
                }
                return false;
            }

            public String getExpressionSymbol() {
                return "BEFORE";
            }
        };
    }

    public static Expression createAFTER(String threshold, Expression lvalue, Expression rvalue) {
        return new LogicExpression(threshold, lvalue, rvalue) {

            public List<CacheItem<Exchange>> getMatching() {
                List<CacheItem<Exchange>> lv = left != null ? left.getMatching() : null;
                List<CacheItem<Exchange>> rv = right != null ? right.getMatching() : null;
                if (rv != null) {
                    if (lv == null) {
                        return rv;
                    }
                    return null;
                }
                return null;
            }

            public boolean isMatch() {
                boolean lv = left != null ? left.isMatch() : false;
                boolean rv = right != null ? right.isMatch() : false;
                if (!lv && rv) {
                    return true;
                }
                return false;
            }

            public String getExpressionSymbol() {
                return "AFTER";
            }
        };
    }

    public static Expression createNOT(String threshold, Expression lvalue, Expression rvalue) {
        return new LogicExpression(threshold, lvalue, rvalue) {

            public List<CacheItem<Exchange>> getMatching() {
                List<CacheItem<Exchange>> result = left != null ? left.getMatching() : null;
                List<CacheItem<Exchange>> rv = right != null ? right.getMatching() : null;
                if (result != null) {
                    if (rv != null) {
                        result.addAll(rv);
                    }
                } else {
                    result = rv;
                }
                if (result != null) {
                    Collections.sort(result);
                }
                return result;
            }

            public boolean isMatch() {
                if (left != null) {
                    if (right != null) {
                        return !right.isMatch();
                    }
                } else if (right != null) {
                    return !right.isMatch();
                }
                return true;
            }

            public String getExpressionSymbol() {
                return "NOT";
            }
        };
    }

}
