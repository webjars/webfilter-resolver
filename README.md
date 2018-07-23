[![Build Status](https://travis-ci.org/webjars/webfilter-resolver.svg?branch=master)](https://travis-ci.org/webjars/webfilter-resolver)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.webjars/webfilter-resolver/badge.svg)](https://search.maven.org/#artifactdetails%7Corg.webjars%7Cwebfilter-resolver%7C0.1.0%7Cjar)

# webjars-webfilter-resolver
Webfilter which enables automatic resolving of webjars without versions.

Webjars automatically places the webjar dependency onto the classpath which allows referencing the
asset files simply by including the file like `webjars/bootstrap/4.1.0/css/bootstrap.min.css`.
However you have to include the version (_4.1.0_ in the previous snippet).

For Spring Boot, you can simply place the dependency `webjars-locator` in your `pom.xml` which will
automatically resolve the versions.

This webfilter does the same for Java EE (or other web containers which implement the Servlet 3.1 API).

# Features
This webfilter supports two response serve methods (behaviors).

| Response Serve Method | Explanation |
| ---- | ----------- |
| REDIRECT | Requests to `webjars/foo/bar.js` will be resolved by returning a redirection header. The request will automatically be redirected to `webjars/foo/1.0.0/bar.js`. Therefore, the files are resolved by two requests which happens automatically in the browser.|
| WRITE_BYTE_RESPONSE | **Default**. The content of `webjars/foo/1.0.0/bar.js` will immediately be written to the output stream. No redirection takes place.

The mode _WRITE_BYTE_RESPONSE_ is used by default. If you wish to override this behaviour, you can do so by
supplying the configuration in the `web.xml` file. Here is an example:
```
    <filter-mapping>
        <filter-name>webjarFilter</filter-name>
        <url-pattern>/webjars/*</url-pattern>
    </filter-mapping>

    <filter>
        <filter-name>webjarFilter</filter-name>
        <filter-class>WebJarFilter</filter-class>
        <init-param>
            <param-name>responseServeMethod</param-name>
            <param-value>REDIRECT</param-value>
        </init-param>
    </filter>
```
If an invalid parameter value is used, the response serve method will fall back to _WRITE_BYTE_RESPONSE_ and
a warning log entry will be written to the server log.

## Usage
To use this webfilter, two dependencies are necessary. First, the actual webfilter-resolver from this repository.
Additionally, it expects a webjars-locator to be provided.
The reason for that is that for specific application servers, a special webjar-locator is necessary.
If this webfilter would include one of these locators by default, you would have to specifically
exclude the locator from the dependency, if you are using a different application server than e.g. Wildfly.

## Example for Wildfly
Following dependencies are required for the webfilter to work on a Wildfly 
server (tested with Wildfly 12 in Java EE 7 mode):
```
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>webfilter-resolver</artifactId>
    <version>0.1.0</version>
</dependency>
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>webjars-locator-jboss-vfs</artifactId>
    <version>0.1.0</version><!-- The fact that the versions are equal is a coincidence :-) -->
</dependency>
```
