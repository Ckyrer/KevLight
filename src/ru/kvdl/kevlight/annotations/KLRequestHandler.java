package ru.kvdl.kevlight.annotations;

import ru.kvdl.kevlight.utils.KLParam;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface KLRequestHandler {
    public String request();
    public KLParam[] args() default {};
    public String requestType() default "GET";
    public boolean startsWith() default false;
}
