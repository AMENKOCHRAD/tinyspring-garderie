package com.tinyspring.garderie.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Before("execution(* com.tinyspring.garderie.service.impl..*(..))")
    public void logMethodEntry(JoinPoint joinPoint) {
        log.info("AOP Before - entering {}.{}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
    }

    @AfterReturning(
            pointcut = "execution(* com.tinyspring.garderie.service.impl..*(..))",
            returning = "result"
    )
    public void logMethodSuccess(JoinPoint joinPoint, Object result) {
        String resultType = result == null ? "void/null" : result.getClass().getSimpleName();

        log.info("AOP AfterReturning - {} completed with result type {}",
                joinPoint.getSignature().getName(),
                resultType);
    }

    @AfterThrowing(
            pointcut = "execution(* com.tinyspring.garderie.service.impl..*(..))",
            throwing = "exception"
    )
    public void logMethodException(JoinPoint joinPoint, Throwable exception) {
        log.warn("AOP AfterThrowing - {} failed: {}",
                joinPoint.getSignature().getName(),
                exception.getMessage());
    }
}
