package seedu.address.model.task;

import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.homework.Homework;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.person.Person;

/**
 * Represents a follow-up task in TutorFlow, attached to the student, the lesson or the homework that it is about
 * (user story 31).
 * Guarantees: details are present and not null, except for the optional due date, lesson and homework, field
 * values are validated, immutable.
 *
 * <p>Every task has a permanent {@code id}, so that two tasks with the same text stay distinct. A task points at
 * its student, and at its lesson or homework, by their permanent ids. The ids survive every edit, move and
 * cancellation, so the link never needs to be rewritten. A task has at most one of a lesson and a homework item.
 */
public class Task {

    public static final int MAX_DESCRIPTION_LENGTH = 200;
    public static final String MESSAGE_DESCRIPTION_CONSTRAINTS =
            "Task descriptions must be 1-" + MAX_DESCRIPTION_LENGTH + " characters and cannot contain line breaks.";
    public static final String MESSAGE_ONE_LINK = "A task can be linked to a lesson or to homework, not both.";

    private final UUID id;
    private final String description;
    private final LocalDate due; // optional, null if the task has no due date
    private final boolean isDone;
    private final UUID studentId;
    private final UUID lessonId; // null unless the task is about a lesson
    private final UUID homeworkId; // null unless the task is about a homework item

    /**
     * Every field must be present and not null, except for {@code due}, {@code lessonId} and {@code homeworkId},
     * of which at most one of the last two may be given.
     */
    public Task(UUID id, String description, LocalDate due, boolean isDone, UUID studentId, UUID lessonId,
            UUID homeworkId) {
        requireAllNonNull(id, description, studentId);
        checkArgument(isValidDescription(description), MESSAGE_DESCRIPTION_CONSTRAINTS);
        checkArgument(lessonId == null || homeworkId == null, MESSAGE_ONE_LINK);
        this.id = id;
        this.description = description.strip();
        this.due = due;
        this.isDone = isDone;
        this.studentId = studentId;
        this.lessonId = lessonId;
        this.homeworkId = homeworkId;
    }

    /**
     * Creates a new open task about {@code student}. The {@code due} date is optional.
     */
    public static Task about(String description, LocalDate due, Person student) {
        requireAllNonNull(description, student);
        return new Task(UUID.randomUUID(), description, due, false, student.getId(), null, null);
    }

    /**
     * Creates a new open task about {@code lesson} of {@code student}. The {@code due} date is optional.
     */
    public static Task about(String description, LocalDate due, Person student, Lesson lesson) {
        requireAllNonNull(description, student, lesson);
        return new Task(UUID.randomUUID(), description, due, false, student.getId(), lesson.getId(), null);
    }

    /**
     * Creates a new open task about {@code homework}, which belongs to its student. The {@code due} date is
     * optional.
     */
    public static Task about(String description, LocalDate due, Homework homework) {
        requireAllNonNull(description, homework);
        return new Task(UUID.randomUUID(), description, due, false, homework.getStudentId(), null,
                homework.getId());
    }

    /**
     * Returns true if a given string can be the description of a task: 1-200 printable characters on one line.
     */
    public static boolean isValidDescription(String test) {
        return test != null && !test.isBlank() && test.strip().length() <= MAX_DESCRIPTION_LENGTH
                && test.codePoints().noneMatch(character -> Character.isISOControl(character)
                        || character == ' ' || character == ' ');
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Optional<LocalDate> getDue() {
        return Optional.ofNullable(due);
    }

    public boolean isDone() {
        return isDone;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public Optional<UUID> getLessonId() {
        return Optional.ofNullable(lessonId);
    }

    public Optional<UUID> getHomeworkId() {
        return Optional.ofNullable(homeworkId);
    }

    /**
     * Returns true if this task is about {@code person}.
     */
    public boolean isAbout(Person person) {
        return studentId.equals(person.getId());
    }

    /**
     * Returns true if this task is linked to {@code lesson}.
     */
    public boolean isAbout(Lesson lesson) {
        return lesson.getId().equals(lessonId);
    }

    /**
     * Returns true if this task is linked to {@code homework}.
     */
    public boolean isAbout(Homework homework) {
        return homework.getId().equals(homeworkId);
    }

    /**
     * Returns a copy of this task with the given description.
     */
    public Task withDescription(String newDescription) {
        return new Task(id, newDescription, due, isDone, studentId, lessonId, homeworkId);
    }

    /**
     * Returns a copy of this task with the given due date, which is null to remove the due date.
     */
    public Task withDue(LocalDate newDue) {
        return new Task(id, description, newDue, isDone, studentId, lessonId, homeworkId);
    }

    /**
     * Returns a completed copy of this task.
     */
    public Task markDone() {
        return new Task(id, description, due, true, studentId, lessonId, homeworkId);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Task otherTask)) {
            return false;
        }

        return id.equals(otherTask.id)
                && description.equals(otherTask.description)
                && Objects.equals(due, otherTask.due)
                && isDone == otherTask.isDone
                && studentId.equals(otherTask.studentId)
                && Objects.equals(lessonId, otherTask.lessonId)
                && Objects.equals(homeworkId, otherTask.homeworkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, due, isDone, studentId, lessonId, homeworkId);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("id", id)
                .add("description", description)
                .add("due", due)
                .add("isDone", isDone)
                .add("studentId", studentId)
                .add("lessonId", lessonId)
                .add("homeworkId", homeworkId)
                .toString();
    }
}
