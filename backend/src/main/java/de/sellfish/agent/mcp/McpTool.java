package de.sellfish.agent.mcp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stub-Annotation für MCP-Tool-Methoden.
 * Ersetzt, sobald Spring AI 1.0.4+ die nativen Annotationen liefert.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {
    String description() default "";
}
