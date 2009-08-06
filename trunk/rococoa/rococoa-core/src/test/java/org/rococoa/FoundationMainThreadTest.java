/*
 * Copyright 2007, 2008 Duncan McGregor
 * 
 * This file is part of Rococoa, a library to allow Java to talk to Cocoa.
 * 
 * Rococoa is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Rococoa is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Rococoa.  If not, see <http://www.gnu.org/licenses/>.
 */
 
package org.rococoa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;

import org.junit.Test;
import org.rococoa.test.RococoaTestCase;

@SuppressWarnings("nls")
public class FoundationMainThreadTest extends RococoaTestCase {
    
    private ID idNSThreadClass = Foundation.getClass("NSThread");
    private Selector isMainThreadSelector = Foundation.selector("isMainThread");
    
    private boolean nsThreadSaysIsMainThread() {
        return Foundation.send(idNSThreadClass, isMainThreadSelector, boolean.class);
    }
    
    @Test public void mainThreadChanges() {
        // Not sure that I understand this result
        assertFalse(nsThreadSaysIsMainThread());
        Thread t1 = Foundation.callOnMainThread(new Callable<Thread>() {
            public Thread call() throws Exception {
                assertTrue(nsThreadSaysIsMainThread());
                return Thread.currentThread();
            }});
        Thread t2 = Foundation.callOnMainThread(new Callable<Thread>() {
            public Thread call() throws Exception {
                assertTrue(nsThreadSaysIsMainThread());
                return Thread.currentThread();
            }});
        assertNotSame(t1, Thread.currentThread());
        assertNotSame(t1, t2);
        assertFalse(t1.equals(t2));
        assertSame(t1.getThreadGroup(), t2.getThreadGroup());
        assertNotSame(Thread.currentThread().getThreadGroup(), t1.getThreadGroup());
    }
    
    @Test public void testCallOnMainThreadFromMainThread() {
        // Weird 
        Thread mainThread = Foundation.callOnMainThread(new Callable<Thread> (){
            public Thread call() throws Exception {
                assertTrue(nsThreadSaysIsMainThread());
                
                Thread insideThread = Foundation.callOnMainThread(new Callable<Thread> (){
                    public Thread call() throws Exception {
                        assertTrue(nsThreadSaysIsMainThread());
                        return Thread.currentThread();
                    }});

                assertSame(Thread.currentThread(), insideThread);
                return insideThread;
            }});
        assertNotSame(mainThread, Thread.currentThread());
    }
    
    @Test public void isMainThread() {
        assertFalse(Foundation.isMainThread());
        assertTrue(Foundation.callOnMainThread(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return Foundation.isMainThread();
            }}));
    }
    
    @Test public void testCallOnMainThread() {
        Callable<Double> callable = new Callable<Double>() {
            public Double call() throws Exception {
                assertTrue(nsThreadSaysIsMainThread());
                ID clas = Foundation.getClass("NSNumber");
                ID aDouble = Foundation.sendReturnsID(clas, "numberWithDouble:", Math.E);
                Object[] args = {};
                return Foundation.send(aDouble, Foundation.selector("doubleValue"), double.class, args);
            }};

        assertEquals(Math.E, Foundation.callOnMainThread(callable), 0.001);
    }

    @Test
    public void testCallOnMainThreadThrows() {
        Callable<Double> callable = new Callable<Double>() {
            public Double call() throws Exception {
                throw new Error("deliberate");
            }};

        try {
            Foundation.callOnMainThread(callable);
            fail();
        } catch (Error expected) {
            assertEquals("deliberate", expected.getMessage());
        }
    }
    
    @Test public void testRunOnMainThread() {
        final Thread testThread = Thread.currentThread();
        final double[] result = new double[1];
        Runnable runnable = new Runnable() {
            public void run() {
                assertNotSame(testThread, Thread.currentThread());
                ID clas = Foundation.getClass("NSNumber");
                ID aDouble = Foundation.sendReturnsID(clas, "numberWithDouble:", Math.E);
                Object[] args = {};
                result[0] =  Foundation.send(aDouble, Foundation.selector("doubleValue"), double.class, args);
            }};
        Foundation.runOnMainThread(runnable);    
        assertEquals(Math.E, result[0], 0.001);        
    }
}
