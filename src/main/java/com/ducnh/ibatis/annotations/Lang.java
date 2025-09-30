package com.ducnh.ibatis.annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.ducnh.ibatis.scripting.LanguageDriver;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Lang {

	Class<? extends LanguageDriver> value();
}
