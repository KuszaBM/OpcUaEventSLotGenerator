package com.tlb.OpcUaSlotxGenerator.exceptions;

public class SlotCreationException extends Exception {
    public SlotCreationException() {
        super("Exception while crating slot");
    }
    public SlotCreationException(String message) {
        super(message);
    }
}
