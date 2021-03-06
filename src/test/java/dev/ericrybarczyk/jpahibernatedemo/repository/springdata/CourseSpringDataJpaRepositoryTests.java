package dev.ericrybarczyk.jpahibernatedemo.repository.springdata;

import com.google.common.collect.Comparators;
import dev.ericrybarczyk.jpahibernatedemo.JpaHibernateDemoApplication;
import dev.ericrybarczyk.jpahibernatedemo.entity.Course;
import dev.ericrybarczyk.jpahibernatedemo.entity.Review;
import dev.ericrybarczyk.jpahibernatedemo.entity.ReviewRating;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {JpaHibernateDemoApplication.class})
class CourseSpringDataJpaRepositoryTests {

    @Autowired
    private CourseSpringDataJpaRepository courseRepository;

    private static final long ID_1 = 10001L;
    private static final long INVALID_ID = 8675309L;
    private static final long ID_FOR_DELETE = 9999L;
    private static final long ID_COURSE_WITH_NO_REVIEWS = 10003;
    private static final String NEW_COURSE_NAME = "new course name";
    private static final String UPDATED_COURSE_NAME = "updated course name";
    private static final String EXISTING_COURSE_NAME = "second course";

    /*
        NOTE: Many of these tests are functionally equivalent to the tests in
        dev.ericrybarczyk.jpahibernatedemo.repository.CourseRepositoryTests
        because the intent here is to verify equivalent functionality
        between the two approaches to creating a Repository
     */

    @Test
    void findById_validId_resultIsReturned() throws Exception {
        Optional<Course> optionalCourse = courseRepository.findById(ID_1);
        assertTrue(optionalCourse.isPresent());
        assertEquals("first course", optionalCourse.get().getName());
    }

    @Test
    void findById_invalidId_noResultReturned() throws Exception {
        Optional<Course> optionalCourse = courseRepository.findById(INVALID_ID);
        assertTrue(optionalCourse.isEmpty());
    }

    @Test
    void findAll_returnsExpectedList() throws Exception {
        List<Course> courseList = courseRepository.findAll();
        assertEquals(4, courseList.size());
    }

    @Test
    @DirtiesContext
    void deleteById_validIdValue_deletedCourseIsNoLongerReturned() throws Exception {
        courseRepository.deleteById(ID_FOR_DELETE);
        assertTrue(courseRepository.findById(ID_FOR_DELETE).isEmpty());
    }

    @Test
    @DirtiesContext
    void saveCourse_newCourseEntity() throws Exception {
        Course savedCourse = courseRepository.save(new Course(NEW_COURSE_NAME));
        assertEquals(NEW_COURSE_NAME, savedCourse.getName());
    }
    @Test
    @DirtiesContext
    void saveCourse_existingCourseEntityWithModifiedValue() throws Exception {
        Optional<Course> optionalCourse = courseRepository.findById(ID_1);
        assertTrue(optionalCourse.isPresent());

        Course course = optionalCourse.get();
        course.setName(UPDATED_COURSE_NAME);

        courseRepository.save(course);

        Optional<Course> updatedOptionalCourse = courseRepository.findById(ID_1);
        assertTrue(updatedOptionalCourse.isPresent());
        assertEquals(UPDATED_COURSE_NAME, updatedOptionalCourse.get().getName());
    }

    @Test
    @DirtiesContext
    void createCourseWithNullName_throwsDataIntegrityViolationException() throws Exception {
        Course badCourse = new Course(null);
        assertThrows(DataIntegrityViolationException.class, () -> { courseRepository.save(badCourse); } );
    }

    @Test
    @DirtiesContext
    void updateCourse_createdDateNotModified() throws Exception {
        Optional<Course> optionalCourse = courseRepository.findById(ID_1);
        assertTrue(optionalCourse.isPresent());

        Course course = optionalCourse.get();
        LocalDateTime originalCreatedDate = LocalDateTime.from(course.getCreatedDate());

        course.setName(UPDATED_COURSE_NAME);
        courseRepository.save(course);

        Optional<Course> updatedOptionalCourse = courseRepository.findById(ID_1);
        assertTrue(updatedOptionalCourse.isPresent());
        LocalDateTime verifyCreatedDate = LocalDateTime.from(updatedOptionalCourse.get().getCreatedDate());

        assertEquals(originalCreatedDate, verifyCreatedDate);
    }

    @Test
    @DirtiesContext
    void updateCourse_lastUpdatedDateIsModified() throws Exception {
        Optional<Course> optionalCourse = courseRepository.findById(ID_1);
        assertTrue(optionalCourse.isPresent());

        Course course = optionalCourse.get();
        LocalDateTime originalUpdatedDate = LocalDateTime.from(course.getLastUpdatedDate());

        course.setName(UPDATED_COURSE_NAME);
        courseRepository.save(course);

        Optional<Course> updatedOptionalCourse = courseRepository.findById(ID_1);
        assertTrue(updatedOptionalCourse.isPresent());
        LocalDateTime revisedUpdatedDate = LocalDateTime.from(updatedOptionalCourse.get().getLastUpdatedDate());

        assertTrue(revisedUpdatedDate.isAfter(originalUpdatedDate));
    }

    @Test
    @Transactional
    @DirtiesContext
    void testSaveNewReview_ExistingCourse() throws Exception {
        Optional<Course> optionalCourse = courseRepository.findById(ID_COURSE_WITH_NO_REVIEWS);
        assertTrue(optionalCourse.isPresent());
        Course course = optionalCourse.get();
        assertEquals(0, course.getReviews().size());

        Review review = new Review("test review one", ReviewRating.THREE);
        review.setCourse(course);
        course.addReview(review);
        courseRepository.save(course);

        Optional<Course> optionalUpdatedCourse = courseRepository.findById(ID_COURSE_WITH_NO_REVIEWS);
        assertTrue(optionalUpdatedCourse.isPresent());
        Course updatedCourse = optionalUpdatedCourse.get();
        assertEquals(1, updatedCourse.getReviews().size());

        /*  NOTE: A critical requirement for the final assert below to pass was to add the "cascade" attribute
                  to Course.reviews @OneToMany annotation. Without this, the above updatedCourse.getReviews().size()
                  was ONE - valid assertion - however all fields on updatedCourse.getReviews().get(0) were NULL
                  because the Review was not really being persisted when saving the Course.
                  See: https://stackoverflow.com/a/60024246/798642 for more info.
         */
        assertEquals("test review one", updatedCourse.getReviews().get(0).getReviewContent());
        assertEquals(ReviewRating.THREE, updatedCourse.getReviews().get(0).getRating());
    }

    @Test
    void testSortedResults() throws Exception {
        List<Course> courseList = courseRepository.findAll(Sort.by(Sort.Direction.DESC, "name"));
        // Google, thank you for Guava!
        assertTrue(Comparators.isInOrder(courseList, (o1, o2) -> o2.getName().compareTo(o1.getName()))); // comparator for descending order
    }

    @Test
    @DirtiesContext
    void testPaginationResults() throws Exception {
        // add 11 Courses so we have 15 in total (data.sql inserts 4 Courses)
        for (int i = 0; i < 11; i++) {
            courseRepository.save(new Course(String.format("Course %s for Pagination", i)));
        }

        PageRequest pageRequest = PageRequest.of(0, 5);
        Page<Course> coursePage1 = courseRepository.findAll(pageRequest);
        assertEquals(5, coursePage1.getSize());

        Page<Course> coursePage2 = courseRepository.findAll(coursePage1.nextPageable());
        assertEquals(5, coursePage2.getSize());

        Page<Course> coursePage3 = courseRepository.findAll(coursePage2.nextPageable());
        assertEquals(5, coursePage3.getSize());
    }

    @Test
    void testFindByNameCustomJpaRepositoryMethod() throws Exception {
        List<Course> courseList = courseRepository.findByName(EXISTING_COURSE_NAME);
        assertEquals(1, courseList.size());
    }

    @Test
    void testFindByNameLikeCustomJpaRepositoryMethod() throws Exception {
        List<Course> courseList = courseRepository.findByNameLikeIgnoreCase("%FUN%");
        assertEquals(1, courseList.size());
    }

    @Test
    @Transactional
    @DirtiesContext
    void testDeleteByNameCustomJpaRepositoryMethod() throws Exception {
        List<Course> deletedCourses = courseRepository.deleteByNameLikeIgnoreCase("%delete%");
        assertEquals(1, deletedCourses.size());

        Optional<Course> optionalCourse = courseRepository.findById(ID_FOR_DELETE);
        assertTrue(optionalCourse.isEmpty());
    }

    @Test
    void testFindFunCoursesJpqlCustomJpaRepositoryMethod() throws Exception {
        List<Course> funCourses = courseRepository.findFunCoursesJpql();
        assertEquals(1, funCourses.size());
    }

    @Test
    void testFindFunCoursesNativeCustomJpaRepositoryMethod() throws Exception {
        List<Course> funCourses = courseRepository.findFunCoursesNative();
        assertEquals(1, funCourses.size());
    }

    @Test
    void testFindFunCoursesNamedCustomJpaRepositoryMethod() throws Exception {
        List<Course> funCourses = courseRepository.findFunCoursesNamed();
        assertEquals(1, funCourses.size());
    }

}