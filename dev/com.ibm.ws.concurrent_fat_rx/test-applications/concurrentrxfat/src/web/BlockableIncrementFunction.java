/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Simple incrementing function that can block for a period of time when it runs.
 * Latches are provided to the constructor for the test case which uses this function
 * to know when it has started and control when it is allowed to complete.
 */
public class BlockableIncrementFunction implements Function<Integer, Integer> {
    /**
     * This latch, if supplied, is counted down when the function begins.
     */
    private final CountDownLatch beginLatch;

    /**
     * This latch, if supplied, is awaited before the function returns a value.
     */
    private final CountDownLatch continueLatch;

    /**
     * String that helps track which test case or part of a test case created this instance.
     */
    private final String testIdentifier;

    /**
     * Thread upon which the function is running.
     */
    volatile Thread executionThread;

    /**
     * Constructor for BlockableIncrementFunction
     *
     * @param testIdentifier string that helps track which test case or part of a test case created this instance.
     * @param beginLatch if not null, this latch is counted down when the function begins.
     * @param continueLatch if not null, this latch is awaited before the function returns a value.
     */
    public BlockableIncrementFunction(String testIdentifier, CountDownLatch beginLatch, CountDownLatch continueLatch) {
        this.beginLatch = beginLatch;
        this.continueLatch = continueLatch;
        this.testIdentifier = testIdentifier;
    }

    @Override
    public Integer apply(Integer t) {
        executionThread = Thread.currentThread();
        System.out.println("BlockableIncrementFunction > apply " + t + " for " + testIdentifier);
        if (beginLatch != null)
            beginLatch.countDown();
        try {
            if (continueLatch != null && !continueLatch.await(ConcurrentRxTestServlet.TIMEOUT_NS * 3, TimeUnit.NANOSECONDS))
                throw new TimeoutException();

            System.out.println("BlockableIncrementFunction < apply: " + (++t));
            return t;
        } catch (InterruptedException | TimeoutException x) {
            System.out.println("BlockableIncrementFunction < apply: " + x);
            throw new CompletionException(x);
        } finally {
            executionThread = null;
        }
    }
}
