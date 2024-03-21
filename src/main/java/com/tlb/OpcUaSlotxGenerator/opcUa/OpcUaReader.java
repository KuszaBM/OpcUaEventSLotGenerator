package com.tlb.OpcUaSlotxGenerator.opcUa;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;
import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.UaSlotBase;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.function.Supplier;

public class OpcUaReader<T> implements Supplier<T> {
    private NodeId tokenId;
    private T item;
    Logger log = LoggerFactory.getLogger(OpcUaReader.class);

    public OpcUaReader(Class<T> cls, UaSlotBase slotBase) {
        tokenId = slotBase.getTokenId();
        Constructor<?> foundCtor = null;
        for(Constructor<?> ctor : cls.getDeclaredConstructors()) {
            if (ctor.getAnnotation(OpcUaConstructor.class) == null) continue;
            if (foundCtor != null)
                throw new IllegalArgumentException("OpcUaConstructor declared twice");
            foundCtor = ctor;
        }
        if (foundCtor == null)
            throw new IllegalArgumentException(cls.getName() + " without OpcUaConstructor");
        constructor = foundCtor;
        Parameter[] params = foundCtor.getParameters();
        paramReaders = new Supplier[params.length];
        for (int idx = 0; idx < params.length; ++idx) {
            Parameter p = params[idx];
            OpcUaNode a = p.getAnnotation(OpcUaNode.class);
            if (a == null)
                throw new IllegalArgumentException("Not all constructor parameters have been annotated");
            if(a.name().isEmpty())
                throw new IllegalArgumentException("Name for all constructor parameters need to be defined");
            paramReaders[idx] = () -> {
                try {
                   // NodeId dataNode = new NodeId(slotBase.getNamespace(), "\"" + slotBase.getOpcUaName() + "\"." + "\"" + a.name() + "_" + slotBase.getSlotName() +  "\"");
                    NodeId dataNode = new NodeId(slotBase.getNamespace(), slotBase.getOpcUaName() + "\"" + a.name() + "_" + slotBase.getSlotName() +  "\"");
                    log.info("reading data: {}", dataNode);
                    return slotBase.getOpcUaClientProvider().getClient().getAddressSpace().getVariableNode(dataNode).readValue().getValue().getValue();
                } catch (UaException e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    @Override
    public T get() {
        Object[] params = new Object[paramReaders.length];
        try {
            for (int idx =0; idx < paramReaders.length; ++idx)
                params[idx] = paramReaders[idx].get();
            return (T) constructor.newInstance(params);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    private final Supplier<Object>[] paramReaders;
    Constructor<?> constructor;
}
