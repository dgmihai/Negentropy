package com.trajan.negentropy.model.interfaces;

public interface TimeableAncestor <T extends TimeableAncestor<T>> extends Timeable, Ancestor<T> {
}
