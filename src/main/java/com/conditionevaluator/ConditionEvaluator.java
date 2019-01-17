package com.conditionevaluator;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

public class ConditionEvaluator {

    public static boolean evaluateCondition(String conditionExpression){
        Iterator<Token> tokens = Tokenize(conditionExpression);
        IteratorCurrentPointer iteratorCurrentPointer = new IteratorCurrentPointer(tokens.next());
        boolean result = evaluateDisjunction(tokens, iteratorCurrentPointer);

        if (iteratorCurrentPointer.getCurrent().kind !=  TokenKind.ENDOFINPUT){
            throw new IllegalArgumentException("Unexpected input past end of expression");
        }

        return result;
    }

    private static boolean evaluateDisjunction(Iterator<Token> tokens, IteratorCurrentPointer iteratorCurrentPointer){
        boolean lhs = evaluateConjunction(tokens, iteratorCurrentPointer);

        while (iteratorCurrentPointer.getCurrent().kind == TokenKind.OR){
            iteratorCurrentPointer.setCurrent(tokens.next());
            boolean rhs = evaluateNegation(tokens, iteratorCurrentPointer);

            lhs |= rhs;
        }
        return lhs;
    }

    private static boolean evaluateConjunction(Iterator<Token> tokens, IteratorCurrentPointer iteratorCurrentPointer){
        boolean lhs = evaluateNegation(tokens, iteratorCurrentPointer);

        while (iteratorCurrentPointer.getCurrent().kind == TokenKind.AND){
            iteratorCurrentPointer.setCurrent(tokens.next());
            boolean rhs = evaluateNegation(tokens, iteratorCurrentPointer);
            lhs &= rhs;
        }
        return lhs;
    }

    private static boolean evaluateNegation(Iterator<Token> tokens, IteratorCurrentPointer iteratorCurrentPointer){
        boolean negated = false;
        while (iteratorCurrentPointer.getCurrent().text == "!"){
            negated = !negated;
            iteratorCurrentPointer.setCurrent(tokens.next());
        }
        boolean result = evaluateEquality(tokens, iteratorCurrentPointer);
        return result ^ negated;
    }

    private static boolean evaluateEquality(Iterator<Token> tokens, IteratorCurrentPointer iteratorCurrentPointer){
        Boolean result = false;
        Token current = iteratorCurrentPointer.getCurrent();
        if (current.kind == TokenKind.BAREWORD || current.kind == TokenKind.STRING){

            String lhs = evaluateString(tokens, iteratorCurrentPointer);
            Token operation = iteratorCurrentPointer.getCurrent();
            iteratorCurrentPointer.setCurrent(tokens.next());
            String rhs = evaluateString(tokens, iteratorCurrentPointer);

            switch (operation.kind){
                case EQUAL:
                    result = lhs.equalsIgnoreCase(rhs);
                    break;
                case NOTEQUAL:
                    result = !lhs.equalsIgnoreCase(rhs);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid comparision operator " + operation.text);

            }
        } else if (current.kind == TokenKind.OPENPARENTHESIS){
            result = parseParenthesizedExpression(tokens, iteratorCurrentPointer);
        } if (current.kind == TokenKind.FLOAT || current.kind == TokenKind.INTEGER){
            float lhs = evaluateInteger(tokens, iteratorCurrentPointer);
            Token operation = iteratorCurrentPointer.getCurrent();
            iteratorCurrentPointer.setCurrent(tokens.next());
            float rhs = evaluateInteger(tokens, iteratorCurrentPointer);

            switch (operation.kind){
                case GREATERTHAN:
                    result = lhs > rhs;
                    break;
                case LESSTHAN:
                    result = lhs < rhs;
                    break;
                case GREATERTHANANDEQUAL:
                    result = lhs >= rhs;
                    break;
                case LESSTHANANDEQUAL:
                    result = rhs <= rhs;
                    break;
                case EQUAL:
                    result = lhs == rhs;
                    break;
                case NOTEQUAL:
                    result = lhs != rhs;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid comparision operator " + operation.text);
            }
        }

        return result;
    }

    private static boolean parseParenthesizedExpression(Iterator<Token> tokens, IteratorCurrentPointer iteratorCurrentPointer){
        iteratorCurrentPointer.setCurrent(tokens.next());
        boolean result  = evaluateDisjunction(tokens, iteratorCurrentPointer);
        if (iteratorCurrentPointer.getCurrent().kind != TokenKind.CLOSEPARENTHESIS) {
            throw new IllegalArgumentException("Missing closing parenthesis");
        }
        iteratorCurrentPointer.setCurrent(tokens.next());
        return result;
    }

    private static String evaluateString(Iterator<Token> tokens, IteratorCurrentPointer iteratorCurrentPointer){
        String result = null;
        String text = iteratorCurrentPointer.getCurrent().text;
        switch (iteratorCurrentPointer.getCurrent().kind){
            case BAREWORD:
                result = text;
                break;
            case STRING:
                result = text.substring(1, text.length() - 1);
                break;

            default:
                throw new IllegalArgumentException("Expected string or bareword, found " + text);
        }
        iteratorCurrentPointer.setCurrent(tokens.next());
        return result;
    }

    private static float evaluateInteger(Iterator<Token> tokens, IteratorCurrentPointer iteratorCurrentPointer){
        float result;
        float value = iteratorCurrentPointer.getCurrent().floatValue;
        switch (iteratorCurrentPointer.getCurrent().kind){
            case INTEGER:
            case FLOAT:
                result = value;
                break;
            default:
                throw new IllegalArgumentException("Expected float or integer represented as float, found " + value);
        }
        iteratorCurrentPointer.setCurrent(tokens.next());
        return result;
    }

    private static Iterator<Token> Tokenize(String input){
        Integer i = 0;
        LinkedList<Token> tokens = new LinkedList<>();

        Pattern pattern;
        Token token;

        while (i < input.length()){
            if (Character.isWhitespace(input.charAt(i))){
                i += 1;
                continue;
            }

            pattern = Pattern.compile("^\'[^\']*'");
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.STRING);
            if (token != null){
                tokens.add(token);
                i += token.text.length();
                continue;
            }

            pattern = Pattern.compile("^\\=\\=");
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.EQUAL);
            if (token != null){
                tokens.add(token);
                i += token.text.length();
                continue;
            }

            pattern = Pattern.compile("^\\!\\=");
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.NOTEQUAL);
            if (token != null){
                tokens.add(token);
                i += token.text.length();
                continue;
            }

            pattern = Pattern.compile("^and", Pattern.CASE_INSENSITIVE);
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.AND);
            if (token != null){
                tokens.add(token);
                i += token.text.length();
                continue;
            }

            pattern = Pattern.compile("^or", Pattern.CASE_INSENSITIVE);
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.OR);
            if (token != null){
                tokens.add(token);
                i += token.text.length();
                continue;
            }

            pattern = Pattern.compile("^[+-]?([0-9]*[.])?[0-9]+", Pattern.CASE_INSENSITIVE);
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.FLOAT);
            if (token != null){
                tokens.add(token);
                i += token.text.length();
                continue;
            }

            pattern = Pattern.compile("^\\d+", Pattern.CASE_INSENSITIVE);
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.INTEGER);
            if (token != null){
                tokens.add(token);
                i += token.text.length();
                continue;
            }

            pattern = Pattern.compile("^\\w+", Pattern.CASE_INSENSITIVE);
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.BAREWORD);
            if (token != null){
                tokens.add(token);
                i += token.text.length();
                continue;
            }

            if (Character.isLetterOrDigit(input.charAt(i))){
                tokens.add(getToken(TokenKind.SPECIALCHARACTER, input.substring(i)));
                i += 1;
                continue;
            }

            pattern = Pattern.compile("^\\(");
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.OPENPARENTHESIS);
            if (token != null){
                tokens.add(token);
                i += 1;
                continue;
            }

            pattern = Pattern.compile("^\\)");
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.CLOSEPARENTHESIS);
            if (token != null){
                tokens.add(token);
                i += 1;
                continue;
            }

            pattern = Pattern.compile("^\\>\\=");
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.GREATERTHANANDEQUAL);
            if (token != null){
                tokens.add(token);
                i += token.text.length();
                continue;
            }

            pattern = Pattern.compile("^\\<\\=");
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.LESSTHANANDEQUAL);
            if (token != null){
                tokens.add(token);
                i += token.text.length();
                continue;
            }

            pattern = Pattern.compile("^<");
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.LESSTHAN);
            if (token != null){
                tokens.add(token);
                i += 1;
                continue;
            }

            pattern = Pattern.compile("^>");
            token = getToken(pattern.matcher(input.substring(i)), TokenKind.GREATERTHAN);
            if (token != null){
                tokens.add(token);
                i += 1;
                continue;
            }

            throw new IllegalArgumentException("Alphanumeric character is not part of a word. This should ideally never occur.");

        }
        tokens.add(getToken(TokenKind.ENDOFINPUT, "end of input"));
        return tokens.iterator();
    }


    private static Token getToken(Matcher matcher, TokenKind tokenKind){
        while (matcher.find()){
            return getToken(tokenKind, matcher.group());
        }
        return null;
    }

    private static Token getToken(TokenKind tokenKind, String text){
        Token token = new Token();
        token.kind = tokenKind;
        token.text = text;
        return token;
    }
}
