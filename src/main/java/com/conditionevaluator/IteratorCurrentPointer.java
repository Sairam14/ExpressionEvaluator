package com.conditionevaluator;


public class IteratorCurrentPointer {
    private Token current;

    public IteratorCurrentPointer(Token token){
        this.current = token;
    }

    public Token getCurrent(){
        return current;
    }

    public void setCurrent(Token token){
        current = token;
    }
}
