package dev.ericrybarczyk.jpahibernatedemo.repository;

import dev.ericrybarczyk.jpahibernatedemo.entity.Course;
import dev.ericrybarczyk.jpahibernatedemo.entity.Passport;
import dev.ericrybarczyk.jpahibernatedemo.entity.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import java.util.List;

@Repository
@Transactional
public class StudentRepository {

    private EntityManager entityManager;
    private Logger logger;

    public StudentRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    public Student findById(Long id) {
        try {
            return entityManager.find(Student.class, id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Student save(Student student) {
        if (student.getId() == null) {
            entityManager.persist(student);
        } else {
            entityManager.merge(student);
        }
        return student;
    }

    public Student save(Student student, Passport passport) {
        // Passport must be persisted first in order to support the relationship
        if (passport.getId() == null) {
            entityManager.persist(passport);
        } else {
            entityManager.merge(passport);
        }
        student.setPassport(passport);
        if (student.getId() == null) {
            entityManager.persist(student);
        } else {
            entityManager.merge(student);
        }
        return student;
    }

    public Student saveEnrollment(Student student, Course course) {
        student.addCourse(course);
        course.addStudent(student);
        entityManager.persist(student);
        entityManager.persist(course);
        return student;
    }

    public boolean deleteById(Long id) {
        Student student = findById(id);
        if (student == null) {
            return false;
        }
        entityManager.remove(student);
        return true;
    }


    // JPQL Queries added for learning purposes

    public List<Student> findStudentsWithValueWithinPassportNumber() {
        return entityManager.createQuery("Select s from Student s where s.passport.passportNumber like '%1234%'", Student.class).getResultList();
    }

    /*
        Other JPQL criteria & function examples for use in queries:
        BETWEEN 100 and 1000
        IS NULL
        upper, lower, trim, length
     */


}
