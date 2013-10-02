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
