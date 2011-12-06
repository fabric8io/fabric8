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
package org.fusesource.eca.eventcache;

import java.util.concurrent.TimeUnit;

/**
 * An abstraction over System.currentTimeMillis() - so can have Mock implementation for testing
 */
public interface EventClock {

    enum TYPE {System, Mock};

    /**
     * Get the current time in milliseconds
     *
     * @return the current time
     */
    long currentTimeMillis();

    /**
     * Set the current time.
     * <p/>
     * This will be a no-op on System time implementation
     */
    void setCurrentTime(int value, TimeUnit unit);

    /**
     * Advance the current time.
     * <p/>
     * This will be a no-op on System time implementation
     */
    void advanceClock(int value, TimeUnit unit);

    /**
     * Retreat the current time.
     * <p/>
     * This will be a no-op on System time implementation
     */
    void retreatClock(int value, TimeUnit unit);
}
