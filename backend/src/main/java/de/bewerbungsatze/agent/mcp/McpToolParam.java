package de.bewerbungsatze.agent.mcp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stub-Annotation für MCP-Tool-Parameter.
 * Ersetzt, sobald Spring AI 1.0.4+ die nativen Annotationen liefert.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpToolParam {
    String description() default "";
}
