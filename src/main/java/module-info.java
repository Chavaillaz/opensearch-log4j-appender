/**
 * The Log4j2 appender shipping logs to OpenSearch, a plugin Log4j2 builds by reflection, which its package is
 * opened to.
 * <p>
 * The OpenSearch client declares no module name: it is required by the name the JDK derives from its jar,
 * {@code opensearch.java}, which the jar keeps as long as it is not renamed. The warning of javac against a
 * transitive dependency on it, and on the HttpClient 5 whose connection manager the API exposes, is suppressed,
 * the API of the appender exposing the client as it is.
 */
@SuppressWarnings("requires-transitive-automatic")
module com.chavaillaz.appender.log4j.opensearch {

    requires transitive com.chavaillaz.appender;
    requires transitive opensearch.java;
    requires transitive org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires tools.jackson.databind;
    requires static lombok;

    // Declared on behalf of the client, which as an automatic module declares none of what it needs, so that the
    // modules it loads classes from are resolved along with it on the module path
    requires jakarta.json;
    requires org.apache.commons.logging;

    exports com.chavaillaz.appender.log4j.opensearch;
    opens com.chavaillaz.appender.log4j.opensearch to org.apache.logging.log4j.core;

}
