package seedu.address.model;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.homework.Homework;
import seedu.address.model.lesson.Lesson;
import seedu.address.model.lesson.UniqueLessonList;
import seedu.address.model.person.Person;
import seedu.address.model.person.UniquePersonList;
import seedu.address.model.task.Task;

/**
 * Wraps all data at the address-book level.
 * Duplicates are not allowed (by .isSamePerson comparison for persons and .isSameLesson comparison for lessons).
 * A lesson belongs to a person, so deleting or editing a person also updates that person's lessons.
 * A task is about a student, and possibly one of their lessons or homework items, by permanent id. Deleting the
 * student, the lesson or the homework item deletes the tasks about it, and editing them leaves the links valid.
 * Tasks may have the same text, but never the same id.
 */
public class AddressBook implements ReadOnlyAddressBook {

    private static final Logger logger = LogsCenter.getLogger(AddressBook.class);

    private final UniquePersonList persons = new UniquePersonList();
    private final UniqueLessonList lessons = new UniqueLessonList();
    private final ObservableList<Homework> homework = FXCollections.observableArrayList();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final ObservableList<Task> unmodifiableTasks = FXCollections.unmodifiableObservableList(tasks);

    public AddressBook() {}

    /**
     * Creates an AddressBook using the Persons, Lessons, Homework and Tasks in the {@code toBeCopied}
     */
    public AddressBook(ReadOnlyAddressBook toBeCopied) {
        this();
        resetData(toBeCopied);
    }

    //// list overwrite operations

    /**
     * Replaces the contents of the person list with {@code persons}.
     * {@code persons} must not contain duplicate persons.
     */
    public void setPersons(List<Person> persons) {
        this.persons.setPersons(persons);
    }

    /**
     * Replaces the contents of the lesson list with {@code lessons}.
     * {@code lessons} must not contain duplicate lessons.
     */
    public void setLessons(List<Lesson> lessons) {
        this.lessons.setLessons(lessons);
    }

    /**
     * Replaces the contents of the task list with {@code tasks}.
     * {@code tasks} must not contain two tasks with the same id, or a task about a student, lesson or homework
     * item that is not in the address book, so the persons, lessons and homework must be set first.
     */
    public void setTasks(List<Task> tasks) {
        requireNonNull(tasks);
        Set<UUID> ids = new HashSet<>();
        for (Task task : tasks) {
            if (!ids.add(task.getId())) {
                throw new IllegalArgumentException("Two tasks cannot have the same id.");
            }
            validateTaskLinks(task);
        }
        this.tasks.setAll(tasks);
    }

    /**
     * Resets the existing data of this {@code AddressBook} with {@code newData}.
     */
    public void resetData(ReadOnlyAddressBook newData) {
        requireNonNull(newData);

        setPersons(newData.getPersonList());
        setLessons(newData.getLessonList());
        homework.setAll(newData.getHomeworkList());
        setTasks(newData.getTaskList());
    }

    //// person-level operations

    /**
     * Returns true if a person with the same identity as {@code person} exists in the address book.
     */
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return persons.contains(person);
    }

    /**
     * Adds a person to the address book.
     * The person must not already exist in the address book.
     */
    public void addPerson(Person p) {
        persons.add(p);
    }

    /**
     * Replaces the given person {@code target} in the list with {@code editedPerson}.
     * {@code target} must exist in the address book.
     * The person identity of {@code editedPerson} must not be the same as another existing person in the address book.
     */
    public void setPerson(Person target, Person editedPerson) {
        requireNonNull(editedPerson);

        editedPerson = editedPerson.withId(target.getId());
        persons.setPerson(target, editedPerson);
        updateStudentOfLessons(target, editedPerson);
    }

    /**
     * Removes {@code key} from this {@code AddressBook}.
     * {@code key} must exist in the address book.
     */
    public void removePerson(Person key) {
        persons.remove(key);
        tasks.removeIf(task -> task.isAbout(key));
        homework.removeIf(item -> item.getStudentId().equals(key.getId()));
        boolean hasRemovedLessons = lessons.removeIf(lesson -> lesson.isWith(key));
        if (hasRemovedLessons) {
            logger.fine("Removed linked lessons");
        }
    }

    /**
     * Makes the lessons of {@code target} the lessons of {@code editedPerson}, so that they are not left behind
     * when the name or the phone number that identifies the person is edited.
     */
    private void updateStudentOfLessons(Person target, Person editedPerson) {
        if (target.getName().equals(editedPerson.getName()) && target.getPhone().equals(editedPerson.getPhone())) {
            return;
        }

        logger.fine("Updating linked lessons");
        List<Lesson> updatedLessons = lessons.asUnmodifiableObservableList().stream()
                .map(lesson -> lesson.isWith(target) ? lesson.withStudent(editedPerson) : lesson)
                .collect(Collectors.toList());
        lessons.setLessons(updatedLessons);
    }

    //// lesson-level operations

    /**
     * Returns true if a lesson with the same identity as {@code lesson} exists in the address book.
     */
    public boolean hasLesson(Lesson lesson) {
        requireNonNull(lesson);
        return lessons.contains(lesson);
    }

    /**
     * Adds a lesson to the address book.
     * The lesson must not already exist in the address book.
     */
    public void addLesson(Lesson lesson) {
        lessons.add(lesson);
    }

    /**
     * Replaces the given lesson {@code target} in the list with {@code editedLesson}.
     * {@code target} must exist in the address book.
     * The lesson identity of {@code editedLesson} must not be the same as another existing lesson in the address book.
     */
    public void setLesson(Lesson target, Lesson editedLesson) {
        requireNonNull(editedLesson);

        lessons.setLesson(target, editedLesson);
    }

    /**
     * Removes {@code key} from this {@code AddressBook}.
     * {@code key} must exist in the address book.
     */
    public void removeLesson(Lesson key) {
        lessons.remove(key);
        tasks.removeIf(task -> task.isAbout(key));
    }

    /**
     * Returns the lessons in the address book that overlap with {@code lesson}.
     */
    public List<Lesson> getConflictingLessons(Lesson lesson) {
        return lessons.getOverlapping(lesson);
    }

    /**
     * Sorts the persons in this {@code AddressBook} using the given {@code comparator}.
     */
    public void sortPersons(Comparator<Person> comparator) {
        persons.sort(comparator);
    }

    /** Adds a unique assignment belonging to a known student. */
    public void addHomework(Homework item) {
        validateHomework(item, null);
        homework.add(item);
    }

    /** Replaces an assignment while preserving its identity. */
    public void setHomework(Homework target, Homework replacement) {
        if (!target.getId().equals(replacement.getId()) || !homework.contains(target)) {
            throw new IllegalArgumentException("Homework identity must be preserved.");
        }
        validateHomework(replacement, target);
        homework.set(homework.indexOf(target), replacement);
    }

    /** Removes an assignment. */
    public void removeHomework(Homework target) {
        if (!homework.remove(target)) {
            throw new IllegalArgumentException("Homework no longer exists.");
        }
        tasks.removeIf(task -> task.isAbout(target));
    }

    /** Rejects duplicate identities, duplicate assignments, and dangling student links. */
    private void validateHomework(Homework item, Homework target) {
        requireNonNull(item);
        if (persons.asUnmodifiableObservableList().stream()
                .noneMatch(person -> person.getId().equals(item.getStudentId()))) {
            throw new IllegalArgumentException("Homework student does not exist.");
        }
        if (homework.stream().filter(existing -> target == null || !existing.getId().equals(target.getId()))
                .anyMatch(existing ->
                existing.getId().equals(item.getId()) || existing.isSameAssignment(item))) {
            throw new IllegalArgumentException("This student already has homework '" + item.getDescription()
                    + "' due " + item.getDueDate() + ".");
        }
    }

    //// task-level operations

    /**
     * Adds a task to the address book.
     * No task with the same id may exist, and the student, lesson or homework item it is about must exist.
     */
    public void addTask(Task task) {
        requireNonNull(task);
        if (findTask(task.getId()).isPresent()) {
            throw new IllegalArgumentException("Two tasks cannot have the same id.");
        }
        validateTaskLinks(task);
        tasks.add(task);
    }

    /**
     * Returns the task with the given {@code id}, or empty if it has been deleted.
     */
    public Optional<Task> findTask(UUID id) {
        requireNonNull(id);
        return tasks.stream().filter(task -> task.getId().equals(id)).findFirst();
    }

    /**
     * Replaces the given task {@code target} with {@code editedTask}, which must have the same id and links.
     * {@code target} must exist in the address book.
     */
    public void setTask(Task target, Task editedTask) {
        requireNonNull(editedTask);
        if (!target.getId().equals(editedTask.getId())
                || !target.getStudentId().equals(editedTask.getStudentId())
                || !target.getLessonId().equals(editedTask.getLessonId())
                || !target.getHomeworkId().equals(editedTask.getHomeworkId())) {
            throw new IllegalArgumentException("An edited task must keep its id and its links.");
        }
        int index = tasks.indexOf(target);
        if (index < 0) {
            throw new IllegalArgumentException("The task to replace does not exist.");
        }
        tasks.set(index, editedTask);
    }

    /**
     * Removes {@code key} from this {@code AddressBook}.
     * {@code key} must exist in the address book.
     */
    public void removeTask(Task key) {
        if (!tasks.remove(key)) {
            throw new IllegalArgumentException("The task to remove does not exist.");
        }
    }

    /**
     * Rejects a task whose student, lesson or homework item is missing, or belongs to another student.
     */
    private void validateTaskLinks(Task task) {
        requireNonNull(task);
        Person student = persons.asUnmodifiableObservableList().stream().filter(task::isAbout).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The student of the task does not exist."));
        if (task.getLessonId().isPresent() && lessons.asUnmodifiableObservableList().stream()
                .noneMatch(lesson -> task.isAbout(lesson) && lesson.isWith(student))) {
            throw new IllegalArgumentException("The lesson of the task does not exist for that student.");
        }
        if (task.getHomeworkId().isPresent() && homework.stream()
                .noneMatch(item -> task.isAbout(item) && item.getStudentId().equals(task.getStudentId()))) {
            throw new IllegalArgumentException("The homework of the task does not exist for that student.");
        }
    }

    @Override
    public ObservableList<Homework> getHomeworkList() {
        return FXCollections.unmodifiableObservableList(homework);
    }

    //// util methods

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("persons", persons)
                .add("lessons", lessons)
                .add("tasks", tasks)
                .toString();
    }

    @Override
    public ObservableList<Person> getPersonList() {
        return persons.asUnmodifiableObservableList();
    }

    @Override
    public ObservableList<Task> getTaskList() {
        return unmodifiableTasks;
    }

    @Override
    public ObservableList<Lesson> getLessonList() {
        return lessons.asUnmodifiableObservableList();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof AddressBook otherAddressBook)) {
            return false;
        }

        return persons.equals(otherAddressBook.persons)
                && lessons.equals(otherAddressBook.lessons)
                && homework.equals(otherAddressBook.homework)
                && tasks.equals(otherAddressBook.tasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(persons, lessons, homework, tasks);
    }
}
