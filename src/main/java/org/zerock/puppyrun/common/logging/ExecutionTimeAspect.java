package org.zerock.puppyrun.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

    @Around("@annotation(org.zerock.puppyrun.common.logging.LogExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startedAt = System.nanoTime();
        Object result = joinPoint.proceed();
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        log.info(
                "Method completed. class={}, method={}, durationMs={}",
                signature.getDeclaringType().getSimpleName(),
                signature.getName(),
                durationMs
        );
        return result;
    }
}
