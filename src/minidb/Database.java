package minidb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * STEP 3: DATABASE (parser + executor + storage in one place, to keep it easy to follow)
 *
 * Flow:   SQL text --> Lexer (tokens) --> parse + execute here --> Table --> saved to file
 *
 * Supported commands:
 *   CREATE TABLE t (id INT PRIMARY KEY, name TEXT);
 *   INSERT INTO t VALUES (1, 'Ankit');
 *   SELECT * | col1, col2 | COUNT(*) FROM t [WHERE ...] [ORDER BY col [DESC]] [LIMIT n];
 *   UPDATE t SET col = value [, col = value] [WHERE ...];
 *   DELETE FROM t [WHERE ...];
 *   DROP TABLE t;
 *   SHOW TABLES;
 */
public class Database {

    private final Map<String, Table> tables = new LinkedHashMap<>();   // table name -> Table
    private final Path folder;                                         // folder where data files are saved

    // Parser state: the list of tokens and the position of the token we are currently on
    private List<String> t;
    private int p;

    private boolean usedIndex;   // only for display: did the query use the index?

    public Database(String folderName) {
        this.folder = Paths.get(folderName);
        loadAll();
    }

    // ============================================================
    //  MAIN ENTRY: run one SQL command
    // ============================================================
    public String execute(String sql) {
        t = Lexer.tokenize(sql);
        p = 0;
        if (t.isEmpty()) {
            return "";
        }

        String command = next().toUpperCase();
        String result;
        switch (command) {
            case "CREATE": result = doCreate(); break;
            case "INSERT": result = doInsert(); break;
            case "SELECT": result = doSelect(); break;
            case "UPDATE": result = doUpdate(); break;
            case "DELETE": result = doDelete(); break;
            case "DROP":   result = doDrop();   break;
            case "SHOW":   result = doShow();   break;
            default: throw new RuntimeException("Unknown command: " + command);
        }

        accept(";");
        if (p < t.size()) {
            throw new RuntimeException("Unexpected extra input at the end of the query: " + peek());
        }
        return result;
    }

    // ============================================================
    //  COMMANDS
    // ============================================================

    // CREATE TABLE users (id INT PRIMARY KEY, name TEXT, age INT);
    private String doCreate() {
        expect("TABLE");
        String name = next();
        if (tables.containsKey(name.toLowerCase())) {
            throw new RuntimeException("Table '" + name + "' already exists");
        }
        expect("(");
        Table table = new Table(name);
        do {
            String col = next();
            String type = next().toUpperCase();
            if (!type.equals("INT") && !type.equals("TEXT")) {
                throw new RuntimeException("Only INT and TEXT types are supported, not '" + type + "'");
            }
            boolean isPk = false;
            if (accept("PRIMARY")) {
                expect("KEY");
                isPk = true;
            }
            table.addColumn(col, type, isPk);
        } while (accept(","));
        expect(")");

        tables.put(name.toLowerCase(), table);
        save(table);
        return "Table '" + name + "' created.";
    }

    // INSERT INTO users VALUES (1, 'Ankit', 22);
    private String doInsert() {
        expect("INTO");
        Table table = getTable(next());
        expect("VALUES");
        expect("(");
        List<Object> values = new ArrayList<>();
        do {
            values.add(parseValue(next()));
        } while (accept(","));
        expect(")");

        table.insert(values.toArray());
        save(table);
        return "1 row inserted.";
    }

    // SELECT name, age FROM users WHERE age > 20 ORDER BY age DESC LIMIT 5;
    private String doSelect() {
        // 1) What to show: *, COUNT(*), or a list of columns
        boolean all = false, count = false;
        List<String> wanted = new ArrayList<>();
        if (accept("*")) {
            all = true;
        } else if (accept("COUNT")) {
            expect("(");
            expect("*");
            expect(")");
            count = true;
        } else {
            do {
                wanted.add(next());
            } while (accept(","));
        }

        // 2) From which table
        expect("FROM");
        Table table = getTable(next());

        // 3) Optional parts: WHERE, ORDER BY, LIMIT
        Where where = accept("WHERE") ? parseWhere() : null;

        String orderCol = null;
        boolean desc = false;
        if (accept("ORDER")) {
            expect("BY");
            orderCol = next();
            if (accept("DESC")) {
                desc = true;
            } else {
                accept("ASC");
            }
        }

        int limit = -1;
        if (accept("LIMIT")) {
            limit = parseInt(next());
        }

        // 4) Find the matching rows
        usedIndex = false;
        List<Object[]> found = findRows(table, where);

        if (count) {
            return "COUNT(*) = " + found.size() + indexNote();
        }

        if (orderCol != null) {
            int oi = table.colIndex(orderCol);
            found.sort((a, b) -> compareValues(a[oi], b[oi]));
            if (desc) {
                Collections.reverse(found);
            }
        }
        if (limit >= 0 && found.size() > limit) {
            found = found.subList(0, limit);
        }

        // 5) Positions of the columns we need to show
        List<Integer> positions = new ArrayList<>();
        if (all) {
            for (int i = 0; i < table.columns.size(); i++) {
                positions.add(i);
            }
        } else {
            for (String col : wanted) {
                positions.add(table.colIndex(col));
            }
        }

        // 6) Format the output
        StringBuilder out = new StringBuilder();
        List<String> header = new ArrayList<>();
        for (int pos : positions) {
            header.add(table.columns.get(pos));
        }
        String headerLine = String.join(" | ", header);
        out.append(headerLine).append("\n");
        out.append("-".repeat(Math.max(headerLine.length(), 10))).append("\n");
        for (Object[] row : found) {
            List<String> cells = new ArrayList<>();
            for (int pos : positions) {
                cells.add(String.valueOf(row[pos]));
            }
            out.append(String.join(" | ", cells)).append("\n");
        }
        out.append("(").append(found.size()).append(" rows)").append(indexNote());
        return out.toString();
    }

    // UPDATE users SET age = 23, name = 'Ankit B' WHERE id = 1;
    private String doUpdate() {
        Table table = getTable(next());
        expect("SET");
        List<String> cols = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        do {
            cols.add(next());
            expect("=");
            values.add(parseValue(next()));
        } while (accept(","));
        Where where = accept("WHERE") ? parseWhere() : null;

        usedIndex = false;
        Set<Object[]> targets = identitySet(findRows(table, where));

        // Build new rows (without touching the old ones), then replace everything at once
        List<Object[]> newRows = new ArrayList<>();
        int changed = 0;
        for (Object[] row : table.rows) {
            if (targets.contains(row)) {
                Object[] copy = row.clone();
                for (int i = 0; i < cols.size(); i++) {
                    copy[table.colIndex(cols.get(i))] = values.get(i);
                }
                table.checkTypes(copy);
                newRows.add(copy);
                changed++;
            } else {
                newRows.add(row);
            }
        }
        table.replaceAllRows(newRows);
        save(table);
        return changed + " row(s) updated." + indexNote();
    }

    // DELETE FROM users WHERE id = 2;
    private String doDelete() {
        expect("FROM");
        Table table = getTable(next());
        Where where = accept("WHERE") ? parseWhere() : null;

        usedIndex = false;
        Set<Object[]> targets = identitySet(findRows(table, where));

        List<Object[]> kept = new ArrayList<>();
        for (Object[] row : table.rows) {
            if (!targets.contains(row)) {
                kept.add(row);
            }
        }
        int removed = table.rows.size() - kept.size();
        table.replaceAllRows(kept);
        save(table);
        return removed + " row(s) deleted." + indexNote();
    }

    // DROP TABLE users;
    private String doDrop() {
        expect("TABLE");
        String name = next();
        Table table = getTable(name);
        tables.remove(name.toLowerCase());
        try {
            Files.deleteIfExists(fileOf(table));
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file: " + e.getMessage());
        }
        return "Table '" + name + "' dropped.";
    }

    // SHOW TABLES;
    private String doShow() {
        expect("TABLES");
        if (tables.isEmpty()) {
            return "(no tables)";
        }
        StringBuilder out = new StringBuilder();
        for (Table table : tables.values()) {
            out.append(table.name).append("  (");
            for (int i = 0; i < table.columns.size(); i++) {
                if (i > 0) out.append(", ");
                out.append(table.columns.get(i)).append(" ").append(table.types.get(i));
                if (i == table.pkIndex) out.append(" PRIMARY KEY");
            }
            out.append(")  - ").append(table.rows.size()).append(" rows\n");
        }
        return out.toString().trim();
    }

    // ============================================================
    //  WHERE: parsing and checking conditions
    // ============================================================

    private static class Condition {
        String col;      // age
        String op;       // >
        Object value;    // 20
    }

    private static class Where {
        List<Condition> conds = new ArrayList<>();
        List<String> joins = new ArrayList<>();   // "AND" / "OR" (between the conditions)
    }

    // WHERE age > 20 AND city = 'Delhi'
    private Where parseWhere() {
        Where w = new Where();
        while (true) {
            Condition c = new Condition();
            c.col = next();
            c.op = next();
            if (!List.of("=", "!=", ">", "<", ">=", "<=").contains(c.op)) {
                throw new RuntimeException("Invalid operator: " + c.op);
            }
            c.value = parseValue(next());
            w.conds.add(c);

            if (peekIs("AND") || peekIs("OR")) {
                w.joins.add(next().toUpperCase());
            } else {
                break;
            }
        }
        return w;
    }

    /** Returns the rows that match the WHERE clause. Always returns a new (mutable) list. */
    private List<Object[]> findRows(Table table, Where w) {
        List<Object[]> out = new ArrayList<>();

        if (w == null) {
            out.addAll(table.rows);
            return out;
        }

        // INDEX SHORTCUT: for a single "primary_key = value" condition we don't need to scan the whole table
        if (w.conds.size() == 1 && table.pkIndex != -1) {
            Condition c = w.conds.get(0);
            if (c.op.equals("=") && table.columns.get(table.pkIndex).equalsIgnoreCase(c.col)) {
                usedIndex = true;
                Object[] row = table.pkMap.get(c.value);
                if (row != null) {
                    out.add(row);
                }
                return out;
            }
        }

        // Otherwise do a FULL SCAN: check every row
        for (Object[] row : table.rows) {
            if (matches(table, row, w)) {
                out.add(row);
            }
        }
        return out;
    }

    // Evaluates conditions from left to right (no AND/OR precedence, kept simple on purpose)
    private boolean matches(Table table, Object[] row, Where w) {
        boolean result = check(table, row, w.conds.get(0));
        for (int i = 1; i < w.conds.size(); i++) {
            boolean next = check(table, row, w.conds.get(i));
            result = w.joins.get(i - 1).equals("AND") ? (result && next) : (result || next);
        }
        return result;
    }

    private boolean check(Table table, Object[] row, Condition c) {
        int cmp = compareValues(row[table.colIndex(c.col)], c.value);
        switch (c.op) {
            case "=":  return cmp == 0;
            case "!=": return cmp != 0;
            case ">":  return cmp > 0;
            case "<":  return cmp < 0;
            case ">=": return cmp >= 0;
            default:   return cmp <= 0;   // "<="
        }
    }

    private int compareValues(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) {
            return Integer.compare((Integer) a, (Integer) b);
        }
        if (a instanceof String && b instanceof String) {
            return ((String) a).compareTo((String) b);
        }
        throw new RuntimeException("Cannot compare a number with text");
    }

    // ============================================================
    //  FILE SAVE / LOAD  (so data survives after the program closes)
    //  File: minidb_data/<table>.tbl
    //  Line 1:  id:INT:PK,name:TEXT,age:INT      (columns)
    //  Rest:    1<TAB>Ankit<TAB>22                (one row per line)
    // ============================================================

    private Path fileOf(Table table) {
        return folder.resolve(table.name.toLowerCase() + ".tbl");
    }

    private void save(Table table) {
        try {
            Files.createDirectories(folder);
            List<String> lines = new ArrayList<>();

            List<String> header = new ArrayList<>();
            for (int i = 0; i < table.columns.size(); i++) {
                header.add(table.columns.get(i) + ":" + table.types.get(i) + (i == table.pkIndex ? ":PK" : ""));
            }
            lines.add(String.join(",", header));

            for (Object[] row : table.rows) {
                List<String> cells = new ArrayList<>();
                for (Object value : row) {
                    cells.add(String.valueOf(value).replace("\t", " ").replace("\n", " "));
                }
                lines.add(String.join("\t", cells));
            }
            Files.write(fileOf(table), lines);
        } catch (IOException e) {
            throw new RuntimeException("Could not save file: " + e.getMessage());
        }
    }

    private void loadAll() {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".tbl"))::iterator) {
                String fileName = file.getFileName().toString();
                String name = fileName.substring(0, fileName.length() - 4);
                List<String> lines = Files.readAllLines(file);
                if (lines.isEmpty()) continue;

                Table table = new Table(name);
                for (String def : lines.get(0).split(",")) {
                    String[] parts = def.split(":");
                    table.addColumn(parts[0], parts[1], parts.length > 2);
                }
                for (int i = 1; i < lines.size(); i++) {
                    String[] cells = lines.get(i).split("\t", -1);
                    Object[] row = new Object[cells.length];
                    for (int c = 0; c < cells.length; c++) {
                        row[c] = table.types.get(c).equals("INT") ? (Object) Integer.parseInt(cells[c]) : cells[c];
                    }
                    table.insert(row);
                }
                tables.put(name.toLowerCase(), table);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load data: " + e.getMessage());
        }
    }

    // ============================================================
    //  SMALL HELPERS (simple parser tools)
    // ============================================================

    private String peek() {
        return p < t.size() ? t.get(p) : "";
    }

    private String next() {
        if (p >= t.size()) {
            throw new RuntimeException("Incomplete query");
        }
        return t.get(p++);
    }

    private boolean peekIs(String word) {
        return peek().equalsIgnoreCase(word);
    }

    /** If the next token is 'word', consume it and return true; otherwise do nothing. */
    private boolean accept(String word) {
        if (peekIs(word)) {
            p++;
            return true;
        }
        return false;
    }

    /** The next token must be 'word', otherwise throw an error. */
    private void expect(String word) {
        String got = next();
        if (!got.equalsIgnoreCase(word)) {
            throw new RuntimeException("Expected '" + word + "' but found '" + got + "'");
        }
    }

    private Table getTable(String name) {
        Table table = tables.get(name.toLowerCase());
        if (table == null) {
            throw new RuntimeException("Table '" + name + "' not found");
        }
        return table;
    }

    /** Turns a token into a real value: 'abc -> "abc" (String), 25 -> 25 (Integer) */
    private Object parseValue(String token) {
        if (token.startsWith("'")) {
            return token.substring(1);
        }
        return parseInt(token);
    }

    private int parseInt(String token) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Expected a number but found '" + token + "'");
        }
    }

    private Set<Object[]> identitySet(List<Object[]> rows) {
        Set<Object[]> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(rows);
        return set;
    }

    private String indexNote() {
        return usedIndex ? "   [PRIMARY KEY index used - fast lookup]" : "";
    }
}