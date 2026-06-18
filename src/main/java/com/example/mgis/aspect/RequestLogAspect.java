package com.example.mgis.aspect;

import com.alibaba.fastjson2.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class RequestLogAspect {

    private static final Logger log = LoggerFactory.getLogger(RequestLogAspect.class);

    // 切点：所有Controller方法
    @Pointcut("execution(* com.example.mgis.controller..*.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();

        long start = System.currentTimeMillis();
        String url = request.getRequestURI();
        String method = request.getMethod();
        String ip = request.getRemoteAddr();
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // 处理参数：过滤 MP 条件构造器，避免序列化报错
        StringBuilder paramStr = new StringBuilder();
        for (Object arg : args) {
            // 判断是否是 MyBatis-Plus 查询条件包装类，直接跳过不序列化
            if (arg instanceof com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
                    || arg instanceof com.baomidou.mybatisplus.core.conditions.query.QueryWrapper) {
                paramStr.append("[MP条件构造器] ");
            } else {
                try {
                    paramStr.append(JSON.toJSONString(arg)).append(" ");
                } catch (Exception e) {
                    paramStr.append("(参数序列化失败) ");
                }
            }
        }

//        log.info("\n================= 请求开始 =================\n" +
//                        "请求地址: {} {}\n" +
//                        "请求IP: {}\n" +
//                        "类名方法: {}.{}\n" +
//                        "请求参数: {}\n" +
//                        "============================================",
//                method, url, ip, className, methodName, paramStr);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
//            log.error("\n================= 请求异常 =================\n" +
//                            "请求地址: {} {}\n" +
//                            "异常信息: {}\n" +
//                            "============================================",
//                    method, url, throwable.getMessage(), throwable);
            throw throwable;
        }

        long cost = System.currentTimeMillis() - start;
        // 响应结果正常序列化（一般是 VO/Entity，无 MP 对象）
//        log.info("\n================= 请求结束 =================\n" +
//                        "请求地址: {} {}\n" +
//                        "响应结果: {}\n" +
//                        "耗时: {}ms\n" +
//                        "============================================",
//                method, url, JSON.toJSONString(result), cost);

        return result;
    }
}