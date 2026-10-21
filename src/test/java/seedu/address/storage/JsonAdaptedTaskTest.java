package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.storage.JsonAdaptedTask.INVALID_ID_MESSAGE_FORMAT;
import static seedu.address.storage.JsonAdaptedTask.MISSING_FIELD_MESSAGE_FORMAT;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalLessons.ALICE_MATH;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.homework.Homework;
import seedu.address.model.task.Task;
import seedu.address.testutil.TaskBuilder;

public class JsonAdaptedTaskTest {

    private static final String ID = TaskBuilder.DEFAULT_ID.toString();
    private static final String STUDENT_ID = ALICE.getId().toString();

    private static JsonAdaptedTask task(String id, String description, String due, Boolean done, String studentId,
            String lessonId, String homeworkId) {
        return new JsonAdaptedTask(id, description, due, done, studentId, lessonId, homeworkId);
    }

    @Test
    public void toModelType_studentTask_roundTrip() throws Exception {
        Task task = new TaskBuilder().withDue(LocalDate.of(2026, 12, 1)).done().build();
        assertEquals(task, new JsonAdaptedTask(task).toModelType());
    }

    @Test
    public void toModelType_lessonTask_roundTrip() throws Exception {
        Task task = new TaskBuilder().withLesson(ALICE, ALICE_MATH).build();
        assertEquals(task, new JsonAdaptedTask(task).toModelType());
    }

    @Test
    public void toModelType_homeworkTask_roundTrip() throws Exception {
        Homework homework = Homework.assign(ALICE.getId(), "Chapter 3", LocalDate.of(2026, 12, 1));
        Task task = new TaskBuilder().withHomework(homework).build();
        assertEquals(task, new JsonAdaptedTask(task).toModelType());
    }

    @Test
    public void toModelType_missingDoneAndDue_meansOpenWithoutDueDate() throws Exception {
        Task task = task(ID, "Call", null, null, STUDENT_ID, null, null).toModelType();

        assertEquals(false, task.isDone());
        assertTrue(task.getDue().isEmpty());
    }

    @Test
    public void toModelType_missingRequiredFields_throwsIllegalValueException() {
        assertThrows(IllegalValueException.class, String.format(MISSING_FIELD_MESSAGE_FORMAT, "id"),
                task(null, "Call", null, false, STUDENT_ID, null, null)::toModelType);
        assertThrows(IllegalValueException.class, String.format(MISSING_FIELD_MESSAGE_FORMAT, "description"),
                task(ID, null, null, false, STUDENT_ID, null, null)::toModelType);
        assertThrows(IllegalValueException.class, String.format(MISSING_FIELD_MESSAGE_FORMAT, "studentId"),
                task(ID, "Call", null, false, null, null, null)::toModelType);
    }

    @Test
    public void toModelType_invalidValues_throwsIllegalValueException() {
        assertThrows(IllegalValueException.class, Task.MESSAGE_DESCRIPTION_CONSTRAINTS,
                task(ID, " ", null, false, STUDENT_ID, null, null)::toModelType);
        assertThrows(IllegalValueException.class, Task.MESSAGE_DESCRIPTION_CONSTRAINTS,
                task(ID, "a".repeat(201), null, false, STUDENT_ID, null, null)::toModelType);
        assertThrows(IllegalValueException.class, JsonAdaptedTask.MESSAGE_INVALID_DUE,
                task(ID, "Call", "01/12/2026", false, STUDENT_ID, null, null)::toModelType);
        assertThrows(IllegalValueException.class, JsonAdaptedTask.MESSAGE_INVALID_DUE,
                task(ID, "Call", "2026-02-30", false, STUDENT_ID, null, null)::toModelType);
    }

    @Test
    public void toModelType_invalidIds_throwsIllegalValueException() {
        assertThrows(IllegalValueException.class, String.format(INVALID_ID_MESSAGE_FORMAT, "id"),
                task("not-an-id", "Call", null, false, STUDENT_ID, null, null)::toModelType);
        assertThrows(IllegalValueException.class, String.format(INVALID_ID_MESSAGE_FORMAT, "studentId"),
                task(ID, "Call", null, false, " ", null, null)::toModelType);
        assertThrows(IllegalValueException.class, String.format(INVALID_ID_MESSAGE_FORMAT, "lessonId"),
                task(ID, "Call", null, false, STUDENT_ID, "Math 2026-12-01", null)::toModelType);
        assertThrows(IllegalValueException.class, String.format(INVALID_ID_MESSAGE_FORMAT, "homeworkId"),
                task(ID, "Call", null, false, STUDENT_ID, null, "")::toModelType);
    }

    @Test
    public void toModelType_lessonAndHomework_throwsIllegalValueException() {
        String other = UUID.randomUUID().toString();
        assertThrows(IllegalValueException.class, Task.MESSAGE_ONE_LINK,
                task(ID, "Call", null, false, STUDENT_ID, other, other)::toModelType);
    }
}
