/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 FuseSource Corporation, a Progress Software company. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").
 * You may not use this file except in compliance with the License. You can obtain
 * a copy of the License at http://www.opensource.org/licenses/CDDL-1.0.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at resources/META-INF/LICENSE.txt.
 *
 */

package org.fusesource.fabric.eca.expression;


import java.util.Collections;
import java.util.List;

import org.apache.camel.Exchange;
import org.fusesource.fabric.eca.eventcache.CacheItem;
import org.fusesource.fabric.eca.util.ParsingUtil;

/**
 * A filter performing a comparison of two objects
 *
 * @version $Revision: 1.2 $
 */
public abstract class LogicExpression extends BinaryExpression {
    private long threshold = 0l;

    /**
     * @param left
     * @param right
     */
    public LogicExpression(String threshold, Expression left, Expression right) {
        super(left, right);
        this.threshold = ParsingUtil.getTimeAsMilliseconds(threshold);
    }

    public static Expression createOR(String threshold, Expression lvalue, Expression rvalue) {
        return new LogicExpression(threshold, lvalue, rvalue) {

            public List<CacheItem<Exchange>> getMatching() throws Exception {

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

            public List<CacheItem<Exchange>> getMatching() throws Exception {

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

            public List<CacheItem<Exchange>> getMatching() throws Exception {

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

            public List<CacheItem<Exchange>> getMatching() throws Exception {

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

            public List<CacheItem<Exchange>> getMatching() throws Exception {

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
