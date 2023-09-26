package com.trajan.negentropy.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Aspect
@Slf4j
public class BenchmarkAspect {

    @Around("@within(benchmark)")
    public Object logExecutionTime(ProceedingJoinPoint pjp, Benchmark benchmark) throws Throwable {
        // Get the method name
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String callerName = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String returnType = signature.getReturnType().getSimpleName();
        Stream<String> argTypes = Arrays.stream(signature.getParameterTypes()).map(Class::getSimpleName);

        String argTypesString = argTypes.collect(Collectors.joining(", "));
        // Measure time before method execution
        long startTime = System.currentTimeMillis();
        log.trace("Benchmarking: {}: {} {}({})", callerName, returnType, methodName, argTypesString);

        Object result = pjp.proceed(); // Proceed with the actual method execution

        // Measure time after method execution
        long endTime = System.currentTimeMillis();

        // Calculate execution time
        long duration = endTime - startTime;

        // Convert threshold from ms to ns
        long millisFloor = benchmark.millisFloor();

        // Only log if the duration exceeds the threshold or if threshold is set to the default (-1)
        if ((benchmark.millisFloor() == -1 || duration >= millisFloor) && duration > 0) {
            // Use NumberFormat with US locale for comma-separated formatting
            String formattedDuration = NumberFormat.getNumberInstance(Locale.US).format(duration);
            if (duration > 300) {
                log.warn(">>> EXCESSIVE DURATION FOR {}: {} {}({}) : {} ms <<<", callerName, returnType, methodName, argTypesString, formattedDuration);
            } else {
                log.debug("{}: {} {}({}) : {} ms", callerName, returnType, methodName, argTypesString, formattedDuration);
            }
        }

        return result;
    }
}
