package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Exercises follow-up tasks (user story 31) through the parser, model and real JSON storage, the way a tutor would
 * type the commands.
 */
public class TasksIntegrationTest {

    private static final LocalDate DAY = LocalDate.now().plusDays(10);

    @TempDir
    public Path temporaryFolder;

    private final Model model = new ModelManager();
    private JsonAddressBookStorage addressBookStorage;
    private Logic logic;

    @BeforeEach
    public void setUp() {
        model.addPerson(ALICE);
        model.addPerson(BENSON);
        addressBookStorage = new JsonAddressBookStorage(temporaryFolder.resolve("addressbook.json"));
        logic = createLogic(model);
    }

    @Test
    public void execute_taskLifecycle_isSavedAtEveryStep() throws Exception {
        logic.execute("task add st/1 desc/Call parent due/" + LocalDate.now().plusDays(1));
        assertEquals(1, saved().getTaskList().size());

        String list = logic.execute("task list").getFeedbackToUser();
        assertTrue(list.startsWith("Tasks (1)\n1. [open] Call parent due " + LocalDate.now().plusDays(1)));

        logic.execute("task edit 1 desc/Call parent again due/-");
        assertEquals("Call parent again", saved().getTaskList().get(0).getDescription());
        assertTrue(saved().getTaskList().get(0).getDue().isEmpty());

        logic.execute("task done 1");
        assertTrue(saved().getTaskList().get(0).isDone());
        assertFalse(logic.execute("task list").getFeedbackToUser().contains("Call parent again"));
        assertTrue(logic.execute("task list all/").getFeedbackToUser().contains("[done] Call parent again"));

        logic.execute("task list all/");
        logic.execute("task delete 1 confirm");
        assertTrue(saved().getTaskList().isEmpty());
    }

    @Test
    public void execute_lessonTaskFollowsLessonAndStudentChanges() throws Exception {
        logic.execute("lesson add st/1 d/" + DAY + " t/16:00");
        logic.execute("agenda d/" + DAY);
        logic.execute("task add lesson/1 desc/Prepare mock paper");

        logic.execute("lesson move 1 t/18:00");
        logic.execute("edit 1 n/Alice Tan");

        String list = logic.execute("task list").getFeedbackToUser();
        assertTrue(list.contains("Alice Tan"), list);
        assertTrue(list.contains("lesson Math " + saved().getLessonList().get(0).getDate() + " 18:00-19:00"), list);
    }

    @Test
    public void execute_deletingStudentWithTasks_needsConfirmAndTakesTasksAlong() throws Exception {
        logic.execute("task add st/1 desc/Call parent");
        logic.execute("task add st/2 desc/Other student");

        String prompt = logic.execute("delete 1").getFeedbackToUser();
        assertTrue(prompt.contains("1 task(s)"), prompt);
        assertEquals(2, saved().getTaskList().size());

        logic.execute("delete 1 confirm");
        assertEquals(1, saved().getTaskList().size());
        assertEquals("Other student", saved().getTaskList().get(0).getDescription());
    }

    @Test
    public void execute_clear_removesTasksToo() throws Exception {
        logic.execute("task add st/1 desc/Call parent");

        logic.execute("clear confirm");

        assertTrue(saved().getTaskList().isEmpty());
    }

    @Test
    public void execute_taskStaysWithItsOwnLesson_whenTheSlotIsRebooked() throws Exception {
        logic.execute("lesson add st/1 d/" + DAY + " t/16:00");
        logic.execute("agenda d/" + DAY);
        logic.execute("task add lesson/1 desc/Prepare mock paper");
        logic.execute("lesson cancel 1 r/Unwell");

        // The same slot is booked again, for a new lesson that must not inherit the task
        logic.execute("lesson add st/1 d/" + DAY + " t/16:00");

        String list = logic.execute("task list").getFeedbackToUser();
        assertTrue(list.contains("(cancelled)"), list);
        assertFalse(list.contains("(scheduled)"), list);
    }

    @Test
    public void execute_taskAboutStudent_survivesEditsAndArchive() throws Exception {
        logic.execute("task add st/1 desc/Call parent");

        logic.execute("edit 1 n/Alice Tan p/91234567");
        logic.execute("archive 1");

        String list = logic.execute("task list").getFeedbackToUser();
        assertTrue(list.contains("Call parent") && list.contains("Alice Tan"), list);
        assertEquals(1, saved().getTaskList().size());
    }

    @Test
    public void execute_pastDueDate_isRejectedForAddAndEdit() throws Exception {
        String addCommand = "task add st/1 desc/Call parent due/2020-01-01";
        CommandException addError = assertThrows(CommandException.class, () -> logic.execute(addCommand));
        assertEquals("Due dates must be today or later.", addError.getMessage());
        assertTrue(model.getAddressBook().getTaskList().isEmpty());

        logic.execute("task add st/1 desc/Call parent due/today");
        logic.execute("task list");
        String editCommand = "task edit 1 desc/Call parent due/2020-01-01";
        CommandException editError = assertThrows(CommandException.class, () -> logic.execute(editCommand));
        assertEquals("Due dates must be today or later.", editError.getMessage());
        assertEquals(LocalDate.now(), saved().getTaskList().get(0).getDue().get());
    }

    @Test
    public void execute_todayAndTmr_workForAddAndEdit() throws Exception {
        logic.execute("task add st/1 desc/Call parent due/today");
        logic.execute("task add st/2 desc/Email parent due/tmr");
        assertEquals(LocalDate.now(), saved().getTaskList().get(0).getDue().get());
        assertEquals(LocalDate.now().plusDays(1), saved().getTaskList().get(1).getDue().get());

        logic.execute("task list");
        logic.execute("task edit 1 desc/Call parent due/tmr");
        assertEquals(LocalDate.now().plusDays(1), saved().getTaskList().get(0).getDue().get());
    }

    @Test
    public void execute_homeworkTask_followsAndIsDeletedWithItsHomework() throws Exception {
        logic.execute("hw add st/1 desc/Chapter 3 due/" + DAY);
        logic.execute("hw list");
        logic.execute("task add hw/1 desc/Check corrections");
        assertTrue(logic.execute("task list").getFeedbackToUser().contains("| homework Chapter 3 due " + DAY),
                "the row names the homework");

        logic.execute("hw edit 1 desc/Chapter 4");
        assertTrue(logic.execute("task list").getFeedbackToUser().contains("| homework Chapter 4 due " + DAY));

        logic.execute("hw delete 1 confirm");
        assertTrue(saved().getTaskList().isEmpty());
    }

    @Test
    public void execute_deletingStudentWithOnlyTasks_namesTheTasks() throws Exception {
        logic.execute("task add st/1 desc/Call parent");
        logic.execute("task add st/1 desc/Email parent");

        String prompt = logic.execute("delete 1").getFeedbackToUser();
        assertTrue(prompt.contains("has 2 task(s)") && prompt.contains("delete 1 confirm"), prompt);

        String done = logic.execute("delete 1 confirm").getFeedbackToUser();
        assertTrue(done.contains("along with 2 task(s)."), done);
        assertTrue(saved().getTaskList().isEmpty());
    }

    @Test
    public void execute_deletingStudentWithEverything_countsEveryRecord() throws Exception {
        logic.execute("lesson add st/1 d/" + DAY + " t/16:00");
        logic.execute("hw add st/1 desc/Chapter 3 due/" + DAY);
        logic.execute("hw list");
        logic.execute("agenda d/" + DAY);
        logic.execute("task add lesson/1 desc/Prepare");
        logic.execute("task add hw/1 desc/Check");
        logic.execute("task add st/1 desc/Call");

        String prompt = logic.execute("delete 1").getFeedbackToUser();
        assertTrue(prompt.contains("This also deletes 3 task(s)."), prompt);

        String done = logic.execute("delete 1 confirm").getFeedbackToUser();
        assertTrue(done.contains("along with 1 lesson(s), 1 homework item(s) and 3 task(s)."), done);
        assertTrue(saved().getTaskList().isEmpty());
    }

    private ReadOnlyAddressBook saved() throws Exception {
        return addressBookStorage.readAddressBook().orElseThrow();
    }

    private Logic createLogic(Model targetModel) {
        return new LogicManager(targetModel, new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userprefs.json"))));
    }
}
