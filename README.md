# Warehouse DLQ Handler Plugin

A Spring Boot application for handling Dead Letter Queue (DLQ) messages in the warehouse system.

## Requirements

- Java 21
- Maven 3.6+
- Spring Boot 3.2.3

## Project Structure

```
src/main/java/com/warehouse/
├── config/         # Configuration classes
├── controller/     # REST controllers
├── service/        # Business logic
├── model/          # Data models
├── repository/     # Data access layer
└── exception/      # Custom exceptions
```

## Getting Started

1. Clone the repository
2. Build the project:
   ```bash
   mvn clean install
   ```
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The application will start on port 8080.

## Dependencies

- Spring Boot Web
- Spring Boot Test
- Lombok 