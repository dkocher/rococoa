package org.rococoa;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rococoa.cocoa.NSInvocation;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Maps to and from values in an NSInvocation.
 * 
 * @author duncan
 *
 */
public abstract class NSInvocationMapper {

    private static final String NATIVE_LONG_ENCODING = Native.LONG_SIZE == 4 ? "i" : "l"; 
    @SuppressWarnings("unused")
    private static final String NATIVE_ULONG_ENCODING = Native.LONG_SIZE == 4 ? "I" : "L"; 

    private static final Map<Class<?>, NSInvocationMapper> lookup = new HashMap<Class<?>, NSInvocationMapper>();
    private static final List<NSInvocationMapper> subtypeInstances = new ArrayList<NSInvocationMapper>();

    // ORDER OF THESE FIELDS IS IMPORTANT!
    public static final NSInvocationMapper VOID = new NSInvocationMapper("v", void.class) {
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            throw new IllegalStateException("Should not have to read void");
        }
        @Override public Memory bufferForResult(Object methodCallResult) {
            return new Memory(0);
        }
    };
    public static final NSInvocationMapper BOOLEAN = new NSInvocationMapper("c", boolean.class) {
        // Cocoa BOOL is defined as signed char
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            byte character = buffer.getByte(0);
            return character == 0 ? java.lang.Boolean.FALSE : java.lang.Boolean.TRUE;
        }
        @Override public Memory bufferForResult(Object methodCallResult) {
            Memory result = new Memory(1);
            result.setByte(0, ((Boolean) methodCallResult) ? (byte) 1 : (byte) 0);
            return result;
        }
    };
    public static final NSInvocationMapper BYTE = new NSInvocationMapper("c", byte.class) {
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            return buffer.getByte(0);
        }        
        @Override public Memory bufferForResult(Object methodCallResult) {
            Memory result = new Memory(1);
            result.setByte(0, ((Byte) methodCallResult).byteValue());
            return result;
        }
    };
    public static final NSInvocationMapper CHAR = new NSInvocationMapper("s", char.class) {
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            throw new UnsupportedOperationException("Don't yet support char, while I think what to do");
            // TODO - think what to do
        }
        @Override
        public Memory bufferForResult(Object methodCallResult) {
            throw new UnsupportedOperationException("Don't yet support char, while I think what to do");
            // TODO - think what to do
        }                
    };
    public static final NSInvocationMapper SHORT = new NSInvocationMapper("s", short.class) {
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            return buffer.getShort(0);
        }
        @Override public Memory bufferForResult(Object methodCallResult) {
            Memory result = new Memory(2);
            result.setShort(0, ((Short) methodCallResult));
            return result;
        }
    };
    public static final NSInvocationMapper INT = new NSInvocationMapper("i", int.class) {
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            return buffer.getInt(0);
        }
        @Override public Memory bufferForResult(Object methodCallResult) {
            Memory result = new Memory(4);
            result.setInt(0, ((Integer) methodCallResult));
            return result;
        }
    };
    public static final NSInvocationMapper LONG = new NSInvocationMapper("l", long.class) {
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            throw new UnsupportedOperationException("Don't yet support long, while I think what to do");
            // TODO - think what to do
        }

        @Override
        public Memory bufferForResult(Object methodCallResult) {
            throw new UnsupportedOperationException("Don't yet support long, while I think what to do");
            // TODO - think what to do
        }                
    };
    public static final NSInvocationMapper FLOAT = new NSInvocationMapper("f", float.class) {
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            return buffer.getFloat(0);
        }
        @Override public Memory bufferForResult(Object methodCallResult) {
            Memory result = new Memory(4);
            result.setFloat(0, ((Float) methodCallResult));
            return result;
        }
    };
    public static final NSInvocationMapper DOUBLE = new NSInvocationMapper("d", double.class) {
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            return buffer.getDouble(0);
        }
        @Override public Memory bufferForResult(Object methodCallResult) {
            Memory result = new Memory(8);
            result.setDouble(0, ((Double) methodCallResult));
            return result;
        }
    };
    public static final NSInvocationMapper ID = new NSInvocationMapper("@", ID.class) {
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            return new ID(buffer.getNativeLong(0));
        }
        @Override public Memory bufferForResult(Object methodCallResult) {
            Memory result = new Memory(Native.POINTER_SIZE);
            result.setNativeLong(0, ((ID) methodCallResult));
            return result;
        };
    };
    public static final NSInvocationMapper STRING = new NSInvocationMapper("@", String.class) {
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            return Foundation.toString(new ID(buffer.getNativeLong(0)));
        }
        @Override public Memory bufferForResult(Object methodCallResult) {
            Memory buffer = new Memory(Native.POINTER_SIZE);
            buffer.setNativeLong(0, Foundation.cfString((String) methodCallResult));
            return buffer;
        }
    };
    public static final NSInvocationMapper NSOBJECT = new SubtypeInvocationMapper("@", NSObject.class) {
        @SuppressWarnings("unchecked")
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            return Rococoa.wrap(new ID(buffer.getNativeLong(0)), (Class<? extends NSObject>) type);
        }
        @Override public Memory bufferForResult(Object methodCallResult) {
            Memory buffer = new Memory(Native.POINTER_SIZE);
            buffer.setNativeLong(0, ((NSObject) methodCallResult).id());
            return buffer;
        }

    };
    public static final NSInvocationMapper NATIVELONG = new SubtypeInvocationMapper(NATIVE_LONG_ENCODING, NativeLong.class) {
        @Override public Object readFrom(Memory buffer, Class<?> type) {
            return buffer.getNativeLong(0);
        }
        @Override public Memory bufferForResult(Object methodCallResult) {
            Memory result = new Memory(Native.LONG_SIZE);
            result.setNativeLong(0, ((NativeLong) methodCallResult));
            return result;
        }
    };

    protected final Class<?> type;
    protected final String typeString;

    public static NSInvocationMapper mapperForType(Class<?> type) {
        // lookup by type
        NSInvocationMapper directMatch = lookup.get(type);
        if (directMatch != null)
            return directMatch;
        // lookup by subtype in order
        for (NSInvocationMapper each : subtypeInstances) {
            if (each.type.isAssignableFrom(type))
                return each;
        }
        if (Structure.class.isAssignableFrom(type))
            return new StructureInvocationMapper(type);
                // this will add itself to lookup and be available next time
        return null;
    }
    
    protected NSInvocationMapper(String typeString, Class<?> type) {
        this.type = type;
        this.typeString = typeString;
        addToCache();
    }

    protected void addToCache() {
        lookup.put(type, this);
        if (type.isPrimitive())
            lookup.put(nonPrimitiveFor(type), this);
    }
    
    public static String stringForType(Class<?> type) {
        return mapperForType(type).typeString();
    }
    
    public String typeString() {
        return typeString;
    }
    
    public Object readArgumentFrom(NSInvocation invocation, int index, Class<?> type) {
        Memory buffer = new Memory(Native.LONG_SIZE);
        invocation.getArgument_atIndex(buffer, index);
        return readFrom(buffer, type);
    }

    protected Object readFrom(Memory buffer, Class<?> type) {
        throw new RuntimeException("Should be overriden or bypassed");
    }
    
    public abstract Memory bufferForResult(Object methodCallResult);
    
    private static Class<?> nonPrimitiveFor(Class<?> type) {
        if (type == void.class)
            return null;
        if (type == boolean.class)
            return Boolean.class;
        if (type == byte.class)
            return Byte.class;
        if (type == short.class)
            return Short.class;
        if (type == char.class)
            return Character.class;
        if (type == int.class)
            return Integer.class;
        if (type == long.class)
            return Long.class;
        if (type == float.class)
            return Float.class;
        if (type == double.class)
            return Double.class;
        throw new IllegalArgumentException("Not a primitive class " + type);
    }

static abstract class SubtypeInvocationMapper extends NSInvocationMapper {

    public SubtypeInvocationMapper(String typeString, Class<?> type) {
        super(typeString, type);
    }

    @Override
    protected void addToCache() {
        subtypeInstances.add(this);
    }
}

static class StructureInvocationMapper extends NSInvocationMapper {

    @SuppressWarnings("unchecked")
    public StructureInvocationMapper(Class<?> type) {
        super(encodeStruct((Class<? extends Structure>) type), type);
    }

    private static String encodeStruct(Class<? extends Structure> clas) {
        StringBuilder result = new StringBuilder();
        if (!(Structure.ByValue.class.isAssignableFrom(clas)))
            result.append('^'); // pointer to
            
        result.append('{').append(clas.getSimpleName()).append('=');
        for (Field f : collectStructFields(clas, new ArrayList<Field>())) {
            result.append(stringForType(f.getType()));
        }
        return result.append('}').toString();
    }

    @SuppressWarnings("unchecked")
    private static List<Field> collectStructFields(Class<? extends Structure> clas, List<Field> list) {
        if (clas == Structure.class)
            return list;
        for (Field f : clas.getDeclaredFields()) {
            list.add(f);
        }
        return collectStructFields((Class<? extends Structure>) clas.getSuperclass(), list);
    }
    
    @SuppressWarnings("unchecked")
    @Override public Object readArgumentFrom(NSInvocation invocation, int index, Class<?> type) {
        if (Structure.ByValue.class.isAssignableFrom(type))
            return readStructureByValue(invocation, index, (Class<? extends Structure>) type);
        else
            return readStructureByReference(invocation, index, (Class<? extends Structure>) type);
    }
    
    @Override public Memory bufferForResult(Object methodCallResult) {
        if (methodCallResult instanceof Structure.ByValue)
            return bufferForStructureByValue((Structure) methodCallResult);
        else
            return bufferForStructureByReference((Structure) methodCallResult);
    }
    
    private Structure readStructureByValue(NSInvocation invocation, int index, 
            Class<? extends Structure> type)
    {
        Structure result = newInstance(type);
        Memory buffer = new Memory(result.size());
        invocation.getArgument_atIndex(buffer, index);
        return copyBufferToStructure(buffer, result);
    }
    
    private Structure readStructureByReference(NSInvocation invocation, int index, 
            Class<? extends Structure> type)
    {
        Memory buffer = new Memory(Native.POINTER_SIZE);
        invocation.getArgument_atIndex(buffer, index);
        Pointer pointerToResult = buffer.getPointer(0);
        Structure result = newInstance(type);        
        return copyBufferToStructure(pointerToResult, result);
    }

    @SuppressWarnings("unchecked")
    private <T> T newInstance(Class<?> clas) {
        try {
            return (T) clas.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate " + clas,  e);
        }
    }

    private Structure copyBufferToStructure(Pointer buffer, Structure structure) {
        int byteCount = structure.size();
        memcpy(structure.getPointer(), buffer, byteCount);
        structure.read();
        return structure;
    }
    
    private Memory bufferForStructureByValue(Structure methodCallResult) {
        methodCallResult.write();
        int byteCount = methodCallResult.size();
        Memory buffer = new Memory(byteCount);
        memcpy(buffer, methodCallResult.getPointer(), byteCount);
        return buffer;
    }

    private Memory bufferForStructureByReference(Structure methodCallResult) {
        methodCallResult.write();
        Memory buffer = new Memory(Native.POINTER_SIZE);
        buffer.setPointer(0, methodCallResult.getPointer());
        return buffer;
    }

    private void memcpy(Pointer dest, Pointer src, int byteCount) {
        memcpyViaByteBuffer(dest, src, byteCount);
    }

    @SuppressWarnings("unused") // kept as naive implementation
    private void memcpyViaArray(Pointer dest, Pointer src, int byteCount) {
        byte[] structBytes = new byte[byteCount];
        src.read(0, structBytes, 0, byteCount);
        dest.write(0, structBytes, 0, byteCount);
    }

    private void memcpyViaByteBuffer(Pointer dest, Pointer src, int byteCount) {
        ByteBuffer destBuffer = dest.getByteBuffer(0, byteCount);
        ByteBuffer srcBuffer = src.getByteBuffer(0, byteCount);
        destBuffer.put(srcBuffer);
    }
}

}
