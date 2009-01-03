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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Structure;

/**
 * Very special case InvocationHandler that invokes the correct message dispatch
 * function for different return types.
 * 
 * Either objc_msgSend or objc_msgSend_stret should be called, depending on the
 * return type. The latter is usually for struct by value, but the former is
 * used for small structs on Intel! Oh and the call has to be mangled in all
 * cases as the result is returned on the stack, but is different sizes
 * depending on its type. Luckily jna and libffi take care of the details -
 * provided they know what the return type is.
 * 
 * This InvocationHandler is passed the return type as the first arg to the method call that it
 * intercepts, it uses it to determine which function to call, and removes it before
 * calling invoking.
 * 
 * @see http://www.cocoabuilder.com/archive/message/cocoa/2006/6/25/166236
 * and
 * @see http://developer.apple.com/documentation/developertools/Conceptual/LowLevelABI/LowLevelABI.pdf
 * 
 * Note also that there is a objc_msgSend_fret that is used supposed to be for 
 * floating point return types, but that I haven't (yet) had to use. 
 * 
 * @author duncan
 * 
 */
class MsgSendHandler implements InvocationHandler {

    private final String OPTION_INVOKING_METHOD = "invoking-method";
    	// TODO - use JNA string when made public
    
    private final static int stretCutoff = 9;
    
    private final static Method OBJC_MSGSEND;
    private final static Method OBJC_MSGSEND_STRET;        
    static {
        try {
            OBJC_MSGSEND = MsgSendLibrary.class.getDeclaredMethod("objc_msgSend", 
                ID.class, Selector.class, Object[].class);
            OBJC_MSGSEND_STRET = MsgSendLibrary.class.getDeclaredMethod("objc_msgSend_stret", 
                    ID.class, Selector.class, Object[].class);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    private final Pair<Method, Function> objc_msgSend_stret_Pair;
    private final Pair<Method, Function> objc_msgSend_Pair;

    public MsgSendHandler(Function objc_msgSend_Function, Function objc_msgSend_stret_Function) {
        this.objc_msgSend_Pair = new Pair<Method, Function>(OBJC_MSGSEND, objc_msgSend_Function);
        this.objc_msgSend_stret_Pair = new Pair<Method, Function>(OBJC_MSGSEND_STRET, objc_msgSend_stret_Function);
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> returnTypeForThisCall = (Class<?>) args[0];
        Object[] argsWithoutReturnType = removeReturnTypeFrom(args);
        
        Map<String, Object> options = new HashMap<String, Object>(1);    
        options.put(Library.OPTION_TYPE_MAPPER, new RococoaTypeMapper());
        
        Pair<Method, Function> invocation = invocationFor(returnTypeForThisCall); 
        options.put(OPTION_INVOKING_METHOD, invocation.first);
        return invocation.second.invoke(returnTypeForThisCall, argsWithoutReturnType, options);
    }
    
    private Object[] removeReturnTypeFrom(Object[] args) {
        Object[] result = new Object[args.length - 1];
        System.arraycopy(args, 1, result, 0, args.length - 2);
        return result;
    }
    
    private Pair<Method, Function> invocationFor(Class<?> returnTypeForThisCall) {
        boolean isStruct = Structure.class.isAssignableFrom(returnTypeForThisCall);
        boolean isStructByValue = isStruct && Structure.ByValue.class.isAssignableFrom(returnTypeForThisCall);
        if (!isStructByValue)
            return objc_msgSend_Pair;
        try {
            Structure prototype = (Structure) returnTypeForThisCall.newInstance();
            return prototype.size() < stretCutoff ?
                    objc_msgSend_Pair : objc_msgSend_stret_Pair;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
}
