package minidb;

import java.util.Scanner;

/**
 * STEP 4: MAIN (command line)
 * You type SQL here. A query ends with ';' (multi-line queries work too).
 * Your data is saved in the "minidb_data" folder.
 */
public class Main {

    public static void main(String[] args) {
        Database db = new Database("minidb_data");
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== MiniDB ===  (type 'help' for commands, 'exit' to quit)");

        StringBuilder query = new StringBuilder();
        while (true) {
            System.out.print(query.length() == 0 ? "minidb> " : "     -> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            if (query.length() == 0) {
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }
                if (line.equalsIgnoreCase("help")) {
                    printHelp();
                    continue;
                }
            }

            query.append(line).append(' ');
            if (line.endsWith(";")) {
                try {
                    System.out.println(db.execute(query.toString()));
                } catch (RuntimeException e) {
                    System.out.println("Error: " + e.getMessage());
                }
                query.setLength(0);
            }
        }
        System.out.println("Bye!");
    }

    private static void printHelp() {
        System.out.println("""
                Commands (end every command with ;):
                  CREATE TABLE users (id INT PRIMARY KEY, name TEXT, age INT);
                  INSERT INTO users VALUES (1, 'Ankit', 22);
                  SELECT * FROM users;
                  SELECT name, age FROM users WHERE age > 20 AND age < 30;
                  SELECT * FROM users ORDER BY age DESC LIMIT 5;
                  SELECT COUNT(*) FROM users;
                  UPDATE users SET age = 23 WHERE id = 1;
                  DELETE FROM users WHERE id = 2;
                  DROP TABLE users;
                  SHOW TABLES;
                  exit""");
    }
}