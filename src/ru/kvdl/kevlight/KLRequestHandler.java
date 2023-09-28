package ru.kvdl.kevlight;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface KLRequestHandler {
    public String request();
    public String requestType() default "GET";
    public boolean startsWith() default false;
}
