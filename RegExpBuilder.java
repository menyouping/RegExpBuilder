import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

public class RegExpBuilder {
    private StringBuilder literal;
    private Boolean ignoreCase;
    private Boolean multiLine;
    private HashSet<Character> specialCharactersInsideCharacterClass;
    private HashSet<Character> specialCharactersOutsideCharacterClass;
    private StringBuilder escapedString;
    private int min;
    private int max;
    private String of;
    private Boolean ofAny;
    private String from;
    private String notFrom;
    private String like;
    private String behind;
    private String notBehind;
    private String either;
    private Boolean reluctant;
    private Boolean capture;

    public RegExpBuilder() {
        literal = new StringBuilder();
        specialCharactersInsideCharacterClass = new HashSet<Character>(
                Arrays.asList(new Character[] { '^', '-', ']' }));
        specialCharactersOutsideCharacterClass = new HashSet<Character>(
                Arrays.asList(new Character[] { '.', '^', '$', '*', '+', '?', '(', ')', '[', '{' }));
        escapedString = new StringBuilder();
        clear();
    }

    private void clear() {
        ignoreCase = false;
        multiLine = false;
        min = -1;
        max = -1;
        of = null;
        ofAny = false;
        from = null;
        notFrom = null;
        like = null;
        behind = null;
        notBehind = null;
        either = null;
        reluctant = false;
        capture = false;
    }

    private void flushState() {
        if (of != null || ofAny || from != null || notFrom != null || like != null) {
            String captureLiteral = capture ? "" : "?:";
            String quantityLiteral = getQuantityLiteral();
            String characterLiteral = getCharacterLiteral();
            String reluctantLiteral = reluctant ? "?" : "";
            String behindLiteral = behind != null ? "(?=" + behind + ")" : "";
            String notBehindLiteral = notBehind != null ? "(?!" + notBehind + ")" : "";
            literal.append("(" + captureLiteral + "(?:" + characterLiteral + ")" + quantityLiteral + reluctantLiteral
                    + ")" + behindLiteral + notBehindLiteral);
            clear();
        }
    }

    private String getQuantityLiteral() {
        if (min != -1) {
            if (max != -1) {
                return "{" + min + "," + max + "}";
            }
            return "{" + min + ",}";
        }
        return "{0," + max + "}";
    }

    private String getCharacterLiteral() {
        if (of != null) {
            return of;
        }
        if (ofAny) {
            return ".";
        }
        if (from != null) {
            return "[" + from + "]";
        }
        if (notFrom != null) {
            return "[^" + notFrom + "]";
        }
        if (like != null) {
            return like;
        }
        return "";
    }

    public String getLiteral() {
        flushState();
        return literal.toString();
    }

    public Pattern getRegExp() {
        flushState();
        int flags = 0;
        if (ignoreCase) {
            flags = flags | Pattern.CASE_INSENSITIVE;
        }
        if (multiLine) {
            flags = flags | Pattern.MULTILINE;
        }
        return Pattern.compile(literal.toString(), flags);
    }

    public RegExpBuilder ignoreCase() {
        ignoreCase = true;
        return this;
    }

    public RegExpBuilder multiLine() {
        multiLine = true;
        return this;
    }

    public RegExpBuilder start() {
        literal.append("(?:^)");
        return this;
    }

    public RegExpBuilder end() {
        flushState();
        literal.append("(?:$)");
        return this;
    }

    public RegExpBuilder either(RegExpBuilder r) {
        return either(r.getLiteral());
    }

    public RegExpBuilder either(String s) {
        flushState();
        this.either = s;
        return this;
    }

    public RegExpBuilder or(RegExpBuilder r) {
        return or(r.getLiteral());
    }

    public RegExpBuilder or(String s) {
        String either = this.either;
        String or = s;
        literal.append("(?:(?:" + either + ")|(?:" + or + "))");
        clear();
        return this;
    }

    public RegExpBuilder exactly(int n) {
        flushState();
        min = n;
        max = n;
        return this;
    }

    public RegExpBuilder min(int n) {
        flushState();
        min = n;
        return this;
    }

    public RegExpBuilder max(int n) {
        flushState();
        max = n;
        return this;
    }

    public RegExpBuilder of(String s) {
        of = escapeOutsideCharacterClass(s);
        return this;
    }

    public RegExpBuilder ofAny() {
        ofAny = true;
        return this;
    }

    public RegExpBuilder from(char... c) {
        return from(new String(c));
    }

    public RegExpBuilder from(String s) {
        from = escapeInsideCharacterClass(s);
        return this;
    }

    public RegExpBuilder notFrom(char... c) {
        return notFrom(new String(c));
    }

    public RegExpBuilder notFrom(String s) {
        notFrom = escapeInsideCharacterClass(s);
        return this;
    }

    public RegExpBuilder like(RegExpBuilder r) {
        like = r.getLiteral();
        return this;
    }

    public RegExpBuilder reluctantly() {
        reluctant = true;
        return this;
    }

    public RegExpBuilder behind(RegExpBuilder r) {
        behind = r.getLiteral();
        return this;
    }

    public RegExpBuilder notBehind(RegExpBuilder r) {
        notBehind = r.getLiteral();
        return this;
    }

    public RegExpBuilder asGroup() {
        capture = true;
        return this;
    }

    public RegExpBuilder find(String s) {
        return this.then(s);
    }

    public RegExpBuilder then(String s) {
        return this.exactly(1).of(s);
    }

    public RegExpBuilder some(String s) {
        return this.min(1).from(s);
    }

    public RegExpBuilder maybeSome(String s) {
        return this.min(0).from(s);
    }

    public RegExpBuilder maybe(String s) {
        return this.max(1).of(s);
    }

    public RegExpBuilder anything() {
        return this.min(1).ofAny();
    }

    public RegExpBuilder lineBreak() {
        return this.either("\\r\\n").or("\\r").or("\\n");
    }

    public RegExpBuilder lineBreaks() {
        return this.like(new RegExpBuilder().lineBreak());
    }

    public RegExpBuilder whitespace() {
        if (this.min == -1 && this.max == -1) {
            return this.exactly(1).of("\\s");
        }
        this.like = "\\s";
        return this;
    }

    public RegExpBuilder tab() {
        return this.exactly(1).of("\\t");
    }

    public RegExpBuilder tabs() {
        return this.like(new RegExpBuilder().tab());
    }

    public RegExpBuilder digit() {
        return this.exactly(1).of("\\d");
    }

    public RegExpBuilder digits() {
        return this.like(new RegExpBuilder().digit());
    }

    public RegExpBuilder letter() {
        this.exactly(1);
        this.from = "A-Za-z";
        return this;
    }

    public RegExpBuilder letters() {
        this.from = "A-Za-z";
        return this;
    }

    public RegExpBuilder lowerCaseLetter() {
        this.exactly(1);
        this.from = "a-z";
        return this;
    }

    public RegExpBuilder lowerCaseLetters() {
        this.from = "a-z";
        return this;
    }

    public RegExpBuilder upperCaseLetter() {
        this.exactly(1);
        this.from = "A-Z";
        return this;
    }

    public RegExpBuilder upperCaseLetters() {
        this.from = "A-Z";
        return this;
    }

    public RegExpBuilder append(RegExpBuilder r) {
        this.exactly(1);
        this.like = r.getLiteral();
        return this;
    }

    public RegExpBuilder optional(RegExpBuilder r) {
        this.max(1);
        this.like = r.getLiteral();
        return this;
    }

    private String escapeInsideCharacterClass(String s) {
        return escapeSpecialCharacters(s, specialCharactersInsideCharacterClass);
    }

    private String escapeOutsideCharacterClass(String s) {
        return escapeSpecialCharacters(s, specialCharactersOutsideCharacterClass);
    }

    private String escapeSpecialCharacters(String s, HashSet<Character> specialCharacters) {
        escapedString = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char character = s.charAt(i);
            if (specialCharacters.contains(character)) {
                escapedString.append("\\" + character);
            } else {
                escapedString.append(character);
            }
        }
        return escapedString.toString();
    }
}
