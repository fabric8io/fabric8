/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.example;

import java.util.Random;

/**
 *
 */
public class StockData {

    private int price;

    public StockData() {
        // just create a random price
        price = 50 + new Random().nextInt(200);
    }

    public int getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "FUSE stock at " + price;
    }
}
