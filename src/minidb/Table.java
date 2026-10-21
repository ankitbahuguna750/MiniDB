package minidb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * STEP 2: TABLE
 * All the data of one table lives here:
 *   - column names and types (INT or TEXT)
 *   - rows (each row is an Object[])
 *   - an index on the PRIMARY KEY (HashMap) so a row can be found instantly by its key
 *
 * Example table "users":
 *   columns = [id, name, age]      types = [INT, TEXT, INT]
 *   rows    = [ [1,"Ankit",22], [2,"Rahul",25] ]
 *   pkMap   = { 1 -> row1, 2 -> row2 }     (if id is the PRIMARY KEY)
 */
public class Table {

    public final String name;
    public final List<String> columns = new ArrayList<>();
    public final List<String> types = new ArrayList<>();
    public int pkIndex = -1;                              // position of the primary key column (-1 = none)
    public List<Object[]> rows = new ArrayList<>();
    public Map<Object, Object[]> pkMap = new HashMap<>(); // primary key -> row

    public Table(String name) {
        this.name = name;
    }

    public void addColumn(String col, String type, boolean isPrimaryKey) {
        if (columns.stream().anyMatch(c -> c.equalsIgnoreCase(col))) {
            throw new RuntimeException("Column '" + col + "' is defined twice");
        }
        if (isPrimaryKey) {
            if (pkIndex != -1) {
                throw new RuntimeException("A table can have only one PRIMARY KEY");
            }
            pkIndex = columns.size();
        }
        columns.add(col);
        types.add(type);
    }

    /** Returns the position of a column by its name (case-insensitive). */
    public int colIndex(String col) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equalsIgnoreCase(col)) {
                return i;
            }
        }
        throw new RuntimeException("Column '" + col + "' does not exist in table '" + name + "'");
    }

    /** Checks that the number of values and their types (INT/TEXT) are correct. */
    public void checkTypes(Object[] row) {
        if (row.length != columns.size()) {
            throw new RuntimeException("Table has " + columns.size() + " columns but " + row.length + " values were given");
        }
        for (int i = 0; i < row.length; i++) {
            boolean ok = types.get(i).equals("INT") ? row[i] instanceof Integer : row[i] instanceof String;
            if (!ok) {
                throw new RuntimeException("Column '" + columns.get(i) + "' needs a " + types.get(i) + " value");
            }
        }
    }

    /** Inserts a new row. Checks first, then adds. */
    public void insert(Object[] row) {
        checkTypes(row);
        if (pkIndex != -1) {
            Object key = row[pkIndex];
            if (pkMap.containsKey(key)) {
                throw new RuntimeException("PRIMARY KEY '" + key + "' already exists");
            }
            pkMap.put(key, row);
        }
        rows.add(row);
    }

    /**
     * Replaces all rows at once (used by UPDATE and DELETE).
     * Builds the new index first; if a duplicate key is found it throws an error and the old data stays safe.
     */
    public void replaceAllRows(List<Object[]> newRows) {
        Map<Object, Object[]> newMap = new HashMap<>();
        if (pkIndex != -1) {
            for (Object[] row : newRows) {
                if (newMap.put(row[pkIndex], row) != null) {
                    throw new RuntimeException("PRIMARY KEY '" + row[pkIndex] + "' would be duplicated");
                }
            }
        }
        this.rows = newRows;
        this.pkMap = newMap;
    }
}