# Email Rate-Limiter Assignment

> #### *"Project requested for a hiring process."*

## Summary

This project uses many of the technologies that would be expected in an
actual production scenario, such as:

- Logging library (Logback)
- DB migration manager (Flyway)
- DI framework (Koin)
- ORM (Hibernate)
- Database connection pooling (HikariCP)

The RateLimiter is configured by a 'Kotlin DSL' builder or a local JSON file.

## Rate-limiter behaviors:

| Rule Type         | Description                                                          | Example                                                                                                                                                                                                              |
|-------------------|----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **No limit**      | For security-related topics.                                         |                                                                                                                                                                                                                      |
| **Never send**    | To serve as a security layer for invalid/deactivated topics.         |                                                                                                                                                                                                                      |
| **Regular limit** | Rules that evaluate the topic's history independent of other topics. | A topic of `NEWS` with a rule of `2 every 7.days` would work as one would expect.                                                                                                                                    |
| **Shared limit**  | Rules that can depend on multiple topics, sharing a limit.           | If it's necessary to limit the total of emails sent in a day, this type of rule can be used: `[NEWS, MARKETING] -> 5 every 1.days` would limit to 5 the total number of 'news' and 'marketing' emails sent in a day. |

***Any number of rules can be applied to any given topic.**

## Implementation Approach

The guiding principle used here was to minimize DB queries, which resulted in requiring just the one query.

- Find maximum span/time-window defined by rules affecting the topic of the email being sent;
- Collect emails sent in this time-window ordered by `sentAt`, most recent first;
- Iterate through the specific time-windows defined by the rules, mapping it to all emails that fit the window;
- Count the number of emails of each topic in each of these windows\
  resulting in a structure like `[timeWindow][topic] -> count`;
- Validate the rules.

## How to run

- Build the project with Gradle: `./gradlew build`
- Boot up the MySQL container with the 'Dependencies' Run Configuration in IntelliJ\
  or regularly start the container with `docker-compose up -d` in the `dependencies/` folder.
- Apply the DB migration with `./gradlew flywayMigrate`
- As this project is an isolated RateLimiter, no real email intents are available.\
  Therefore, the only behavior available is through prepared tests.\
  To run them, use `./gradlew test`
