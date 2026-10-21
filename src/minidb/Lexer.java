package minidb;

import java.util.ArrayList;
import java.util.List;

/**
 * STEP 1: LEXER
 * Breaks SQL text into small pieces (tokens), just like the lexer of a compiler.
 *
 * Example:  SELECT name FROM users WHERE age > 20;
 * Tokens:   [SELECT] [name] [FROM] [users] [WHERE] [age] [>] [20] [;]
 *
 * Note: A text value (like 'Ankit') is stored with a leading ' in the token,
 *       so we can later tell it is a string and not a number or keyword.
 *       'Ankit'  ->  "'Ankit"
 */
public class Lexer {

    public static List<String> tokenize(String sql) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        int len = sql.length();

        while (i < len) {
            char c = sql.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;                                   // ignore spaces

            } else if (Character.isLetter(c) || c == '_') {
                int start = i;                         // word: SELECT, users, name ...
                while (i < len && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) {
                    i++;
                }
                tokens.add(sql.substring(start, i));

            } else if (Character.isDigit(c)
                    || (c == '-' && i + 1 < len && Character.isDigit(sql.charAt(i + 1)))) {
                int start = i;                         // number: 10, 250, -5
                i++;
                while (i < len && Character.isDigit(sql.charAt(i))) {
                    i++;
                }
                tokens.add(sql.substring(start, i));

            } else if (c == '\'') {
                int start = ++i;                       // string: 'Ankit'
                while (i < len && sql.charAt(i) != '\'') {
                    i++;
                }
                if (i >= len) {
                    throw new RuntimeException("Unterminated string (closing ' is missing)");
                }
                tokens.add("'" + sql.substring(start, i));
                i++;                                   // skip the closing '

            } else if (i + 1 < len && isTwoCharOperator(sql.substring(i, i + 2))) {
                tokens.add(sql.substring(i, i + 2));   // !=  >=  <=
                i += 2;

            } else if ("(),;*=<>".indexOf(c) >= 0) {
                tokens.add(String.valueOf(c));         // single-character symbol
                i++;

            } else {
                throw new RuntimeException("Unexpected character: " + c);
            }
        }
        return tokens;
    }

    private static boolean isTwoCharOperator(String s) {
        return s.equals("!=") || s.equals(">=") || s.equals("<=");
    }
}