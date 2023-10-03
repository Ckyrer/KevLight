package ru.kvdl.kevlight.annotations;

import ru.kvdl.kevlight.utls.KLParam;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface KLCmdRequestHandler {
    KLParam[] args() default {};
    String command();
}
