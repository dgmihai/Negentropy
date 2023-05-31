package com.trajan.negentropy.aspects;

import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TestAspect {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

//    //What kind of method calls I would intercept
//    //execution(* PACKAGE.*.*(..))
//    //Weaving & Weaver
//    @Before("execution(* com.trajan.negentropy.server.*.*(..))")
//    public void before(JoinPoint joinPoint) {
//        //Advice
//        logger.info(" TEST ");
//        logger.info(" TEST2 {}", joinPoint);
//    }
}