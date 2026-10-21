package seedu.address.testutil;

import java.time.LocalDate;
import java.util.UUID;

import seedu.address.model.homework.Homework;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.person.Person;
import seedu.address.model.task.Task;

/**
 * A utility class to help with building Task objects.
 */
public class TaskBuilder {

    public static final String DEFAULT_DESCRIPTION = "Call parent";
    public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UUID id = DEFAULT_ID;
    private String description = DEFAULT_DESCRIPTION;
    private LocalDate due;
    private boolean isDone;
    private UUID studentId = TypicalPersons.ALICE.getId();
    private UUID lessonId;
    private UUID homeworkId;

    /** Creates a {@code TaskBuilder} for an open task about Alice without a due date. */
    public TaskBuilder() {}

    /** Returns an easily recognised id, so that a test can name the tasks it adds. */
    public static UUID idOf(int number) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", number));
    }

    /** Sets the permanent id of the {@code Task} that we are building. */
    public TaskBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    /** Sets the description of the {@code Task} that we are building. */
    public TaskBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    /** Sets the due date of the {@code Task} that we are building. */
    public TaskBuilder withDue(LocalDate due) {
        this.due = due;
        return this;
    }

    /** Marks the {@code Task} that we are building as done. */
    public TaskBuilder done() {
        this.isDone = true;
        return this;
    }

    /** Makes the {@code Task} that we are building about {@code student} alone. */
    public TaskBuilder withStudent(Person student) {
        this.studentId = student.getId();
        this.lessonId = null;
        this.homeworkId = null;
        return this;
    }

    /** Makes the {@code Task} that we are building about {@code lesson}, whose student is {@code student}. */
    public TaskBuilder withLesson(Person student, Lesson lesson) {
        this.studentId = student.getId();
        this.lessonId = lesson.getId();
        this.homeworkId = null;
        return this;
    }

    /** Makes the {@code Task} that we are building about {@code homework}, which sets its student. */
    public TaskBuilder withHomework(Homework homework) {
        this.studentId = homework.getStudentId();
        this.lessonId = null;
        this.homeworkId = homework.getId();
        return this;
    }

    /** Returns the {@code Task} that we have built. */
    public Task build() {
        return new Task(id, description, due, isDone, studentId, lessonId, homeworkId);
    }
}
