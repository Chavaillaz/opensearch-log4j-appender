/**
 * The Log4j2 appender shipping logs to OpenSearch, a plugin Log4j2 builds by reflection, which its package is
 * opened to.
 * <p>
 * The OpenSearch client declares no module name: it is required by the name the JDK derives from its jar,
 * {@code opensearch.java}, which the jar keeps as long as it is not renamed. The warnings of javac against
 * requiring such an automatic module, as the jars of HttpClient 5 are too, and against requiring one
 * transitively, are suppressed: these libraries offer no other module to require, and the API of the appender
 * exposes the client, and the connection manager of HttpClient 5, as they are.
 */
@SuppressWarnings({"requires-automatic", "requires-transitive-automatic"})
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
