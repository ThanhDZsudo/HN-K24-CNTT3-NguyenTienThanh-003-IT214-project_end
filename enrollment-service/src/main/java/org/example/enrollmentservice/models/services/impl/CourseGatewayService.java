package org.example.enrollmentservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.enrollmentservice.clients.CourseClient;
import org.example.enrollmentservice.exceptions.CourseNotFoundException;
import org.example.enrollmentservice.exceptions.CourseServiceException;
import org.example.enrollmentservice.models.dto.responses.CourseResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseGatewayService {

    private final CourseClient courseClient;

    @CircuitBreaker(name = "courseService", fallbackMethod = "fallbackGetCourseById")
    public CourseResponse getCourseById(Long courseId) {
        try {
            return courseClient.getCourseById(courseId);
        } catch (FeignException.NotFound e) {
            throw new CourseNotFoundException(courseId);
        } catch (FeignException e) {
            throw new CourseServiceException("Unable to fetch course with id: " + courseId, e);
        }
    }

    public CourseResponse fallbackGetCourseById(Long courseId, Throwable throwable) {
        if (throwable instanceof CourseNotFoundException) {
            throw (CourseNotFoundException) throwable;
        }

        if (throwable instanceof CourseServiceException) {
            throw (CourseServiceException) throwable;
        }

        throw new CourseServiceException("Course service unavailable for course id: " + courseId, throwable);
    }
}
