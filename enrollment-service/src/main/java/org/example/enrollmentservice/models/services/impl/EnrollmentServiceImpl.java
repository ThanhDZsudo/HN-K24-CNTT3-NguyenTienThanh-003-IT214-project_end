package org.example.enrollmentservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.enrollmentservice.exceptions.DuplicateCourseException;
import org.example.enrollmentservice.kafka.EnrollmentCreatedProducer;
import org.example.enrollmentservice.models.constants.EnrollmentStatus;
import org.example.enrollmentservice.models.dto.requests.CreateEnrollmentDetailRequest;
import org.example.enrollmentservice.models.dto.requests.CreateEnrollmentRequest;
import org.example.enrollmentservice.models.dto.responses.CourseResponse;
import org.example.enrollmentservice.models.dto.responses.EnrollmentDetailResponse;
import org.example.enrollmentservice.models.dto.responses.EnrollmentResponse;
import org.example.enrollmentservice.models.entities.Enrollment;
import org.example.enrollmentservice.models.entities.EnrollmentDetail;
import org.example.enrollmentservice.models.repositories.EnrollmentDetailRepository;
import org.example.enrollmentservice.models.repositories.EnrollmentRepository;
import org.example.enrollmentservice.models.services.EnrollmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentDetailRepository enrollmentDetailRepository;
    private final CourseGatewayService courseGatewayService;
    private final EnrollmentCreatedProducer enrollmentCreatedProducer;

    @Override
    @Transactional
    public EnrollmentResponse createEnrollment(CreateEnrollmentRequest request) {
        Set<Long> seenCourseIds = new HashSet<>();
        List<CourseResponse> courses = new ArrayList<>();
        double totalFee = 0d;

        for (CreateEnrollmentDetailRequest item : request.items()) {
            Long courseId = item.courseId();
            if (!seenCourseIds.add(courseId)) {
                throw new DuplicateCourseException();
            }

            CourseResponse course = courseGatewayService.getCourseById(courseId);
            double fee = course.courseFee() == null ? 0d : course.courseFee();
            totalFee += fee;
            courses.add(course);
        }

        Enrollment enrollment = Enrollment.builder()
                .studentName(request.studentName())
                .studentEmail(request.studentEmail())
                .totalFee(totalFee)
                .status(EnrollmentStatus.PENDING)
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        List<EnrollmentDetail> details = new ArrayList<>();
        List<EnrollmentDetailResponse> detailResponses = new ArrayList<>();

        for (int i = 0; i < courses.size(); i++) {
            CourseResponse course = courses.get(i);
            double fee = course.courseFee() == null ? 0d : course.courseFee();

            EnrollmentDetail detail = EnrollmentDetail.builder()
                    .enrollment(savedEnrollment)
                    .courseId(course.courseId())
                    .courseFee(fee)
                    .build();
            details.add(detail);

            detailResponses.add(new EnrollmentDetailResponse(null, course.courseId(), course.courseName(), fee, fee));
        }

        List<EnrollmentDetail> savedDetails = enrollmentDetailRepository.saveAll(details);

        for (int i = 0; i < savedDetails.size(); i++) {
            EnrollmentDetailResponse response = detailResponses.get(i);
            detailResponses.set(i, new EnrollmentDetailResponse(
                    savedDetails.get(i).getId(),
                    response.courseId(),
                    response.courseName(),
                    response.courseFee(),
                    response.lineTotal()
            ));
        }

        enrollmentCreatedProducer.sendEnrollmentCreated(savedEnrollment.getStudentEmail());

        return new EnrollmentResponse(
                savedEnrollment.getId(),
                savedEnrollment.getStudentName(),
                savedEnrollment.getStudentEmail(),
                savedEnrollment.getTotalFee(),
                savedEnrollment.getStatus(),
                detailResponses
        );
    }
}
