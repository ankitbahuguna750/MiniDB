MiniDB
A small relational database engine written in plain Java (no external libraries).
It understands a subset of SQL, stores data on disk and uses a primary-key index for fast lookups.
Features
Custom SQL lexer + parser (hand-written)
`CREATE TABLE`, `INSERT`, `SELECT`, `UPDATE`, `DELETE`, `DROP TABLE`, `SHOW TABLES`
`WHERE` with `=  !=  >  <  >=  <=` joined by `AND` / `OR`
`ORDER BY ... [DESC]`, `LIMIT`, `COUNT(*)`
Types: `INT`, `TEXT`
`PRIMARY KEY` with duplicate check and HashMap index (O(1) lookup)
File persistence: data survives restarts (`minidb_data/` folder)
How it works
```
SQL text -> Lexer (tokens) -> Parser/Executor (Database.java) -> Table (rows + index) -> .tbl file
```
File	Role
`Lexer.java`	Breaks SQL text into tokens
`Table.java`	Columns, rows, primary key index, type checks
`Database.java`	Parses and runs commands, saves/loads files
`Main.java`	Command-line interface
Run
```
javac -d out src/minidb/*.java
java -cp out minidb.Main
```
(or just press Run on `Main.java` in IntelliJ)
Example
```
minidb> CREATE TABLE users (id INT PRIMARY KEY, name TEXT, age INT);
minidb> INSERT INTO users VALUES (1, 'Ankit', 22);
minidb> SELECT * FROM users WHERE id = 1;
id | name | age
---------------
1 | Ankit | 22
(1 rows)   [PRIMARY KEY index use hua - fast lookup]
```
Limitations / ideas for next steps
`AND` / `OR` are evaluated left to right (no precedence or brackets)
No JOIN, GROUP BY or transactions
Whole table file is rewritten on every change
Next: B-Tree index, JOIN, binary file format, write-ahead log


        // Commands
CREATE TABLE employees (id INT PRIMARY KEY, name TEXT, salary INT);

INSERT INTO employees VALUES (1, 'Ankit', 30000);

INSERT INTO employees VALUES (2, 'Rahul', 25000);

SELECT * FROM employees;

SELECT * FROM employees WHERE id = 1;

SELECT * FROM employees WHERE salary > 25000;

UPDATE employees SET salary = 35000 WHERE id = 1;

SELECT COUNT(*) FROM employees;