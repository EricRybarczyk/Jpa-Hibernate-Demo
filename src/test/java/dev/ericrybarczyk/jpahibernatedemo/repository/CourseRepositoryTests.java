package dev.ericrybarczyk.jpahibernatedemo.repository;

import dev.ericrybarczyk.jpahibernatedemo.JpaHibernateDemoApplication;
import dev.ericrybarczyk.jpahibernatedemo.entity.Course;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = JpaHibernateDemoApplication.class)
class CourseRepositoryTests {

    @Autowired
    private CourseRepository repository;

    private static final long ID_1 = 10001L;
    private static final long ID_FOR_DELETE = 9999L;
    private static final long INVALID_ID = 8675309L;
    private static final String NEW_COURSE_NAME = "new course name";
    private static final String UPDATED_COURSE_NAME = "updated course name";

    @Test
    void findById_basicTestCase() throws Exception {
        Course result = repository.findById(ID_1);
        assertEquals("first course", result.getName());
    }

    @Test
    @DirtiesContext
    void deleteById_validIdValue() throws Exception {
        assertTrue(repository.deleteById(ID_FOR_DELETE));
        assertNull(repository.findById(ID_FOR_DELETE));
    }

    @Test
    void deleteById_invalidIdValue() throws Exception {
        assertFalse(repository.deleteById(INVALID_ID));
    }

    @Test
    @DirtiesContext
    void saveCourse_newCourseEntity() throws Exception {
        Course savedCourse = repository.save(new Course(NEW_COURSE_NAME));
        assertEquals(NEW_COURSE_NAME, savedCourse.getName());
    }

    @Test
    @DirtiesContext
    void saveCourse_existingCourseEntityWithModifiedValue() throws Exception {
        Course course = repository.findById(ID_1);
        course.setName(UPDATED_COURSE_NAME);

        repository.save(course);

        Course result = repository.findById(ID_1);
        assertEquals(UPDATED_COURSE_NAME, result.getName());
    }

}