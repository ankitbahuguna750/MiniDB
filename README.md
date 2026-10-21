# MiniDB — A Database Management System Built from Scratch in Java

MiniDB is a learning project built in Java to explore the fundamentals of database management systems (DBMS). The project focuses on understanding how a database processes commands, manages data, and provides a foundation for a simple database engine.

The project also explores a JavaFX-based graphical user interface for interacting with the database.

## Features

- **Java-Based Implementation:** Built using Java and object-oriented programming.
- **Database Operations:** Designed to support basic data management operations.
- **SQL-Like Commands:** A command interface for interacting with the database, depending on the implemented command set.
- **Data Storage:** Explore how database data can be stored and retrieved.
- **JavaFX GUI:** A desktop interface for making the database easier to use.
- **Testing:** Supports testing implemented database operations and verifying results.

## Technologies Used

- Java 21
- JavaFX 21
- IntelliJ IDEA Community Edition
- Object-Oriented Programming (OOP)
- Database Management System Fundamentals

## Project Architecture

```text
MiniDB
│
├── User Interface
│   ├── Command Input
│   └── JavaFX Desktop UI
│
├── Command Processing
│   └── Parse and Validate Commands
│
├── Database Operations
│   ├── Insert / Create
│   ├── Read / Query
│   ├── Update
│   └── Delete
│
└── Data Storage
    └── Store and Retrieve Data
```

*Note: This is a conceptual architecture. Adjust the components and supported operations to match the actual implementation.*

## Project Structure

```text
MiniDB/
├── src/
│   └── minidb/
│       ├── MiniDBJavaFX.java
│       └── ... other database classes
│
├── README.md
└── ... other project files
```

The exact class structure depends on the current version of the project.

## Requirements

Before running MiniDB, install:

1. JDK 21
2. IntelliJ IDEA Community Edition
3. JavaFX SDK 21, if required by the graphical interface

Download JavaFX:

https://gluonhq.com/products/javafx/

## Setup and Installation


### 1. Configure the JDK

In IntelliJ IDEA, open:

`File → Project Structure → Project`

Set the Project SDK to JDK 21.

### 2. Configure JavaFX

If the project uses JavaFX, download JavaFX SDK 21 and extract it.

Add its `lib` folder through:

`File → Project Structure → Libraries → + → Java`

### 3. Configure VM Options

For the JavaFX run configuration, add:

```text
--module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls
```

Change the path if JavaFX is installed elsewhere.

### 4. Run the Application

Open `MiniDBJavaFX.java` in IntelliJ IDEA.

Right-click the file and select:

`Run 'MiniDBJavaFX.main()'`

If your project uses a different main class, run that class instead.

## How MiniDB Works

A database system typically performs these operations:

1. **Accept Input:** Receives a database command or user action.
2. **Process Commands:** Checks the command and determines the requested operation.
3. **Execute Operations:** Reads, inserts, updates, or deletes data according to the supported features.
4. **Manage Storage:** Stores and retrieves data using the project's implemented storage mechanism.
5. **Display Results:** Shows query results or error messages through the command line or GUI.

The exact workflow depends on the database engine implemented in this project.

## Example Database Operations

The following are conceptual examples of common database operations. Use only commands supported by your actual MiniDB implementation.

```sql
CREATE TABLE students (
    id INT,
    name VARCHAR(50)
);

INSERT INTO students VALUES (1, 'Ankit');

SELECT * FROM students;
```

Expected result, if these SQL features are implemented:

| id | name |
|---:|---|
| 1 | Ankit |

**Important:** These examples illustrate SQL concepts. They do not guarantee that the current MiniDB supports SQL syntax, tables, or data types.

## Learning Objectives

This project helps explore:

- Database management system fundamentals
- Java classes and object-oriented design
- Command processing and validation
- Data storage and retrieval
- CRUD operations
- GUI development using JavaFX
- Testing database functionality
- Error handling and debugging

## Future Improvements

- Add or improve SQL command support.
- Implement tables, rows, and column definitions.
- Improve data persistence.
- Add a query editor and results table in JavaFX.
- Display clear error messages for invalid commands.
- Add automated tests for database operations.
- Explore indexes and query optimization.
- Add transaction support as an advanced extension.

## Author

**Ankit Bahuguna**

Java | Database Fundamentals | JavaFX | Software Development

## License

This project is intended for learning and educational purposes. Add an appropriate open-source license before distributing it publicly.
