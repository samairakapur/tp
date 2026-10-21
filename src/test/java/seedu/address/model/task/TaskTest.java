package seedu.address.model.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalLessons.ALICE_MATH;
import static seedu.address.testutil.TypicalLessons.BENSON_PHYSICS;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import seedu.address.model.homework.Homework;
import seedu.address.testutil.LessonBuilder;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.TaskBuilder;

public class TaskTest {

    @Test
    public void isValidDescription() {
        assertFalse(Task.isValidDescription(null));
        assertFalse(Task.isValidDescription(""));
        assertFalse(Task.isValidDescription("   "));
        assertFalse(Task.isValidDescription("two\nlines"));
        assertFalse(Task.isValidDescription("tab\there"));
        assertFalse(Task.isValidDescription("a".repeat(Task.MAX_DESCRIPTION_LENGTH + 1)));

        assertTrue(Task.isValidDescription("a"));
        assertTrue(Task.isValidDescription("a".repeat(Task.MAX_DESCRIPTION_LENGTH)));
        assertTrue(Task.isValidDescription("  padded  ")); // trimmed when stored
        assertTrue(Task.isValidDescription("Call parent re: S$ fees (50%) - ok?"));
    }

    @Test
    public void constructor_invalidArguments_throws() {
        UUID id = UUID.randomUUID();
        UUID studentId = ALICE.getId();
        assertThrows(NullPointerException.class, () -> new Task(null, "x", null, false, studentId, null, null));
        assertThrows(NullPointerException.class, () -> new Task(id, null, null, false, studentId, null, null));
        assertThrows(NullPointerException.class, () -> new Task(id, "x", null, false, null, null, null));
        assertThrows(IllegalArgumentException.class, Task.MESSAGE_DESCRIPTION_CONSTRAINTS, ()
                -> new Task(id, " ", null, false, studentId, null, null));
        assertThrows(IllegalArgumentException.class, Task.MESSAGE_ONE_LINK, ()
                -> new Task(id, "x", null, false, studentId, UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    public void about_assignsDistinctIds() {
        Task first = Task.about("Same text", null, ALICE);
        Task second = Task.about("Same text", null, ALICE);

        assertNotEquals(first.getId(), second.getId());
        assertNotEquals(first, second); // two tasks with the same text are allowed and stay distinct
        assertFalse(first.isDone());
    }

    @Test
    public void about_linksByPermanentIds() {
        Homework homework = Homework.assign(BENSON.getId(), "Chapter 3", LocalDate.of(2026, 12, 1));

        Task lessonTask = Task.about("Prep", null, ALICE, ALICE_MATH);
        assertEquals(ALICE.getId(), lessonTask.getStudentId());
        assertEquals(ALICE_MATH.getId(), lessonTask.getLessonId().get());
        assertTrue(lessonTask.getHomeworkId().isEmpty());

        Task homeworkTask = Task.about("Mark", null, homework);
        assertEquals(BENSON.getId(), homeworkTask.getStudentId());
        assertEquals(homework.getId(), homeworkTask.getHomeworkId().get());
        assertTrue(homeworkTask.getLessonId().isEmpty());
    }

    @Test
    public void descriptionIsStripped() {
        assertEquals("padded", new TaskBuilder().withDescription("  padded  ").build().getDescription());
    }

    @Test
    public void copies_changeOnlyTheirOwnField() {
        LocalDate due = LocalDate.of(2026, 12, 1);
        Task task = new TaskBuilder().withDue(due).build();

        assertEquals("New", task.withDescription("New").getDescription());
        assertEquals(due, task.withDescription("New").getDue().get());
        assertTrue(task.withDue(null).getDue().isEmpty());
        assertTrue(task.markDone().isDone());
        assertEquals(task.getId(), task.markDone().getId());
        assertEquals(due, task.markDone().getDue().get());
    }

    @Test
    public void isAbout_matchesByIdNotByDetails() {
        Task studentTask = new TaskBuilder().withStudent(ALICE).build();
        Task lessonTask = new TaskBuilder().withLesson(ALICE, ALICE_MATH).build();

        assertTrue(studentTask.isAbout(ALICE));
        assertFalse(studentTask.isAbout(BENSON));
        assertFalse(studentTask.isAbout(ALICE_MATH)); // a task about a student is not about their lessons
        assertTrue(lessonTask.isAbout(ALICE_MATH));
        assertTrue(lessonTask.isAbout(ALICE)); // but a task about a lesson is also about the student
        assertFalse(lessonTask.isAbout(BENSON_PHYSICS));
        // the same details but another lesson (for example a rebooked slot) is another lesson
        assertFalse(lessonTask.isAbout(new LessonBuilder(ALICE_MATH).build()));
        // the same lesson after it is cancelled keeps its id, so the task stays with it
        assertTrue(lessonTask.isAbout(ALICE_MATH.cancel("Cancelled")));
        // and a student who is renamed keeps theirs
        assertTrue(studentTask.isAbout(new PersonBuilder(ALICE).withName("Alice Tan").build().withId(ALICE.getId())));
    }

    @Test
    public void isAbout_homework() {
        Homework homework = Homework.assign(ALICE.getId(), "Chapter 3", LocalDate.of(2026, 12, 1));
        Homework other = Homework.assign(ALICE.getId(), "Chapter 3", LocalDate.of(2026, 12, 1));
        Task task = new TaskBuilder().withHomework(homework).build();

        assertTrue(task.isAbout(homework));
        assertTrue(task.isAbout(ALICE));
        assertFalse(task.isAbout(other)); // the same text is another assignment
    }

    @Test
    public void equalsAndHashCode() {
        Task task = new TaskBuilder().build();

        assertEquals(task, new TaskBuilder().build());
        assertEquals(task.hashCode(), new TaskBuilder().build().hashCode());
        assertEquals(task, task);
        assertNotEquals(task, null);
        assertNotEquals(task, "task");
        assertNotEquals(task, new TaskBuilder().withId(UUID.randomUUID()).build());
        assertNotEquals(task, new TaskBuilder().withDescription("Other").build());
        assertNotEquals(task, new TaskBuilder().withDue(LocalDate.of(2026, 1, 1)).build());
        assertNotEquals(task, new TaskBuilder().done().build());
        assertNotEquals(task, new TaskBuilder().withStudent(BENSON).build());
        assertNotEquals(task, new TaskBuilder().withLesson(ALICE, ALICE_MATH).build());
        assertNotEquals(task, new TaskBuilder().withHomework(
                Homework.assign(ALICE.getId(), "Chapter 3", LocalDate.of(2026, 12, 1))).build());
    }

    @Test
    public void toStringMethod() {
        Task task = new TaskBuilder().build();
        String expected = Task.class.getCanonicalName() + "{id=" + TaskBuilder.DEFAULT_ID
                + ", description=Call parent, due=null, isDone=false, studentId=" + ALICE.getId()
                + ", lessonId=null, homeworkId=null}";
        assertEquals(expected, task.toString());
    }
}
