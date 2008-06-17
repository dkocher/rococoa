package org.rococoa;

import com.sun.jna.Library;

/**
 * JNA Library for plain C calls, standard JNA marshalling applies to these
 */
public interface FoundationLibrary extends Library {
    
    void NSLog(ID pString, Object thing);
    
    ID CFStringCreateWithCString(ID allocator, String string, int encoding);
    ID CFStringCreateWithBytes(ID allocator, byte[] bytes, int byteCount, int encoding, byte isExternalRepresentation);
    String CFStringGetCStringPtr(ID string, int encoding);
    byte CFStringGetCString(ID theString, byte[] buffer, int bufferSize, int encoding);
    int CFStringGetLength(ID theString);
    
    void CFRetain(ID cfTypeRef);
    void CFRelease(ID cfTypeRef);
    int CFGetRetainCount (ID cfTypeRef);
        
    ID objc_getClass(String className);
    ID class_createInstance(ID pClass, int extraBytes);        
    Selector sel_registerName(String selectorName);
}