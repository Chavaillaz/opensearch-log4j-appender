# OpenSearch Appender

![Quality Gate](https://github.com/chavaillaz/opensearch-log4j-appender/actions/workflows/sonarcloud.yml/badge.svg)
![Dependency Check](https://github.com/chavaillaz/opensearch-log4j-appender/actions/workflows/snyk.yml/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.chavaillaz/opensearch-log4j-appender/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.chavaillaz/opensearch-log4j-appender)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This appender allows you to send log events directly from Log4j to an OpenSearch cluster. The delivery of logs is
done asynchronously and therefore will not block the execution of applications using it.

| Appender version | Log4j version | OpenSearch version | Java version | Documentation |
|------------------|---------------|--------------------|--------------|---------------|
| 1.0.0            | 2.24.1        | 2.x                | 17+          | See below     |
| 1.0.1            | 2.24.3        | 2.x                | 17+          | See below     |
| 1.1.0            | 2.24.3        | 3.x                | 21+          | See below     |

See [the compatibility details from OpenSearch](https://github.com/opensearch-project/opensearch-java/blob/main/COMPATIBILITY.md)
for more details.

## Installation

The dependency is available in maven central (see badge and table above for the version):

```xml
<dependency>
    <groupId>com.chavaillaz</groupId>
    <artifactId>opensearch-log4j-appender</artifactId>
</dependency>
```

You then have to configure log4j in order to include this appender (see configuration section below).

## Configuration

In the Log4j configuration file, add a new appender `OpensearchAppender` using package
`com.chavaillaz.appender.log4j.opensearch` with the following properties:

| Appender property | Environment / System variable | Default value               | Description                                                                                                                             |
|-------------------|-------------------------------|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| Application       | APP                           | `unknown`                   | The name of the application generating the logs.                                                                                        |
| Host              | HOST                          | Machine host name           | The name of the host on which the application is running.                                                                               |
| Environment       | ENV                           | `local`                     | The name of the environment in which the application is running.                                                                        |
| Converter         | CONVERTER                     | `[...].DefaultLogConverter` | The path of the class used to convert logging events into key/value documents to be stored.                                             |
| Index             | INDEX                         | `ha`                        | The name of the OpenSearch index to which the documents are sent.                                                                       |
| IndexSuffix       | INDEX_SUFFIX                  | -                           | The suffix added to the index name (using current date) in a format pattern suitable for `DateTimeFormatter`.                           |
| Url               | OPENSEARCH_URL                | -                           | The address of OpenSearch in the format `scheme://host:port`.                                                                           |
| User              | OPENSEARCH_USER               | -                           | The username to use as credentials to access OpenSearch.                                                                                |
| Password          | OPENSEARCH_PASSWORD           | -                           | The password to use as credentials to access OpenSearch.                                                                                |
| ApiKey            | OPENSEARCH_API_KEY            | -                           | The API key (already encoded) to use as credentials to access OpenSearch.                                                               |
| FlushThreshold    | -                             | `100`                       | The threshold number of messages triggering the transmission of documents to the server.                                                |
| FlushInterval     | -                             | `5000`                      | The time (ms) between two automatic flushes, which are triggering the transmission of logs, even if not reaching the defined threshold. |

Note that `Url` is the only mandatory configuration, except if you need to overwrite the default value of another ones.

## XML file example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="info">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        <OpensearchAppender name="OpenSearch">
            <PatternLayout pattern="%msg"/>
            <Application>myApplication</Application>
            <Environment>local</Environment>
            <Converter>com.chavaillaz.appender.log4j.DefaultLogConverter</Converter>
            <Index>ha</Index>
            <IndexSuffix>-yyyy.MM</IndexSuffix>
            <Url>http://localhost:9200</Url>
            <User>admin</User>
            <Password>admin</Password>
            <FlushThreshold>100</FlushThreshold>
            <FlushInterval>5000</FlushInterval>
        </OpensearchAppender>
    </Appenders>
    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="OpenSearch"/>
        </Root>
        <Logger name="com.chavaillaz.appender" additivity="false">
            <AppenderRef ref="Console"/>
        </Logger>
    </Loggers>
</Configuration>
```

## Contributing

If you have a feature request or found a bug, you can:

- Write an issue
- Create a pull request

If you want to contribute then

- Please write tests covering all your changes
- Ensure you didn't break the build by running `mvn test`
- Fork the repo and create a pull request

## License

This project is under Apache 2.0 License.