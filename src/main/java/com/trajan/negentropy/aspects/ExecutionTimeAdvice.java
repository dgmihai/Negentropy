package com.trajan.negentropy.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class ExecutionTimeAdvice {
    @Pointcut("within(com.trajan.negentropy..*)")
    public void logExecutionTime() {}

    @Around("logExecutionTime()")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(
                joinPoint.getTarget().getClass()
        );

        String methodName = joinPoint.getSignature().getName();
        StopWatch stopWatch = new StopWatch(methodName);
        stopWatch.start(methodName);
        Object result = joinPoint.proceed();
        stopWatch.stop();
        log.trace(stopWatch.getTotalTimeMillis() + " ms : " + stopWatch.getId() + " running time");
        return result;
    }
}