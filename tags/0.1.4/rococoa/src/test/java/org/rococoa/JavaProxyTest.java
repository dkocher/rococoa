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

import org.rococoa.cocoa.NSNotification;
import org.rococoa.cocoa.NSNotificationCenter;
import org.rococoa.cocoa.NSString;

@SuppressWarnings("nls")
public class JavaProxyTest extends NSTestCase {
    
    public static class JavaImplementor {
        public Object arg;
    
        public void nothing() {
            arg = null;
        }
        
        public ID sayHello() {
            return Foundation.cfString("Hello");
        }
        
        public ID testTakesIDReturnsID(ID id) {
            arg = id;
            return Foundation.cfString("Hello");
        }
        
        public void takesOCObject(NSString s) {
            arg = s;            
        }
        
        public void takesInt_AndInt(int a, int b) {
            arg = new Object[] {a, b};
        }
        
        public byte takesStringReturnsByte(NSString s) {
            arg = s;
            return 42;
        }
        
        public boolean takesBooleanReturnsBoolean(boolean b) {
            arg = b;
            return !b;
        }
        
        public String takesJavaStringReturnsJavaString(String s) {
            return s.toLowerCase();
        }
        
        public void notify(NSNotification notification) {
            this.arg = notification;
        }
     }

    private JavaImplementor implementor;
    private ID ocProxy;
    
    @Override
    protected void setUp() throws Exception {
        implementor = new JavaImplementor();
        ocProxy = Rococoa.wrap(implementor); // hang onto this to prevent GC issues
    }
    
    public void testRepondsToSelector() {
        // respond to selector is required for delegates
        assertEquals(0, (byte) Foundation.send(ocProxy, "respondsToSelector:", byte.class, Foundation.selector("Bo")));
        assertEquals(1, (byte) Foundation.send(ocProxy, "respondsToSelector:", byte.class, Foundation.selector("sayHello")));
    }
    
    public void testNoArgsReturnsVoid() {
        implementor.arg = "something";
        ID result = Foundation.sendReturnsID(ocProxy, "nothing");
        assertTrue(result.isNull());
        assertNull(implementor.arg);
    }

    public void testNoArgsReturnsID() {
        ID result =  Foundation.sendReturnsID(ocProxy, "sayHello");
        assertEquals("Hello", Foundation.toString(result));        
        assertNull(implementor.arg);
    }

    public void testTakesIDReturnsID() {
        ID result =  Foundation.sendReturnsID(ocProxy, "testTakesIDReturnsID:", new ID(42));
        assertEquals("Hello", Foundation.toString(result));        
        assertEquals(new ID(42), implementor.arg);
    }
    
    public void testTakesOCObject() {
        ID result =  Foundation.sendReturnsID(ocProxy, "takesOCObject:", Foundation.cfString("hello"));
        assertTrue(result.isNull());
        assertEquals("hello", ((NSString) implementor.arg).toString());
    }

    public void testTakesStringReturnsByte() {
        byte result = Foundation.send(ocProxy, "takesStringReturnsByte:", byte.class, Foundation.cfString("hello"));
        assertEquals(42, result);
        assertEquals("hello", ((NSString) implementor.arg).toString());
    }
    
    public void testTakesBooleanReturnsBoolean() {
        assertTrue(Foundation.send(ocProxy, "takesBooleanReturnsBoolean:", boolean.class, false));
        assertFalse(Foundation.send(ocProxy, "takesBooleanReturnsBoolean:", boolean.class, true));
    }
    
    public void testTakesIntAndInt() {
        ID result =  Foundation.sendReturnsID(ocProxy, "takesInt:AndInt:", 42, -1);
        assertTrue(result.isNull());
        Object[] arg = (Object[]) implementor.arg;
        assertEquals(42, arg[0]);
        assertEquals(-1, arg[1]);
    }
    
    public void testTakesJavaStringReturnsJavaString() {
        assertEquals("lower", Foundation.send(ocProxy, "takesJavaStringReturnsJavaString:", String.class, "LoWeR"));
    }
    
    public void testNotifications() {
        NSNotificationCenter notificationCentre = NSNotificationCenter.CLASS.defaultCenter();
        
        notificationCentre.addObserver_selector_name_object(
                ocProxy, 
                Foundation.selector("notify:"),
                "MyNotification",
                null);

        NSNotification notification = NSNotification.CLASS.notificationWithName_object("MyNotification", null);
        
        assertNull(implementor.arg);
        notificationCentre.postNotification(notification);             
        assertEquals(notification, implementor.arg);
    }

    
}
