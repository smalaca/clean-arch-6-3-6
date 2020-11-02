package com.smalaca.taskamanager.api.rest;

import com.smalaca.taskamanager.dto.EpicDto;
import com.smalaca.taskamanager.dto.WatcherDto;
import com.smalaca.taskamanager.model.embedded.EmailAddress;
import com.smalaca.taskamanager.model.embedded.Owner;
import com.smalaca.taskamanager.model.embedded.PhoneNumber;
import com.smalaca.taskamanager.model.embedded.UserName;
import com.smalaca.taskamanager.model.embedded.Watcher;
import com.smalaca.taskamanager.model.entities.Epic;
import com.smalaca.taskamanager.model.entities.Project;
import com.smalaca.taskamanager.model.entities.User;
import com.smalaca.taskamanager.model.enums.ToDoItemStatus;
import com.smalaca.taskamanager.repository.EpicRepository;
import com.smalaca.taskamanager.repository.ProjectRepository;
import com.smalaca.taskamanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.Optional;

import static com.smalaca.taskamanager.model.enums.ToDoItemStatus.RELEASED;
import static com.smalaca.taskamanager.model.enums.ToDoItemStatus.TO_BE_DEFINED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class EpicControllerTest {
    private static final String TITLE = "Title like all the others";
    private static final String DESCRIPTION = "Something have to be done";
    private static final ToDoItemStatus STATUS = RELEASED;
    private static final String FIRST_NAME = "Nick";
    private static final String LAST_NAME = "Fury";
    private static final String EMAIL_ADDRESS = "nick.fury@shield.marvel.com";
    private static final String PHONE_PREFIX = "567";
    private static final String PHONE_NUMBER = "133131313";
    private static final String ANOTHER_FIRST_NAME = "Maria";
    private static final String ANOTHER_LAST_NAME = "Hill";
    private static final String ANOTHER_EMAIL_ADDRESS = "maria.hill@shield.com";
    private static final String ANOTHER_PHONE_PREFIX = "909";
    private static final String ANOTHER_PHONE_NUMBER = "982478438743";
    private static final long EPIC_ID = 13;
    private static final long OWNER_ID = 42;
    private static final long PROJECT_ID = 69;
    private static final long WATCHER_ID = 5;

    private final EpicRepository epicRepository = mock(EpicRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final EpicController controller = new EpicController(epicRepository, userRepository, projectRepository);
    private final ArgumentCaptor<Epic> epicCaptor = ArgumentCaptor.forClass(Epic.class);

    @Test
    void shouldNotFindEpic() {
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.empty());

        ResponseEntity<EpicDto> actual = controller.findById(EPIC_ID);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldFindEpic() {
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.of(existingEpic()));

        ResponseEntity<EpicDto> actual = controller.findById(EPIC_ID);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        EpicDto dto = actual.getBody();
        assertThat(dto.getId()).isEqualTo(EPIC_ID);
        assertThat(dto.getTitle()).isEqualTo(TITLE);
        assertThat(dto.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(dto.getStatus()).isEqualTo(STATUS.name());
        assertThat(dto.getOwnerFirstName()).isEqualTo(FIRST_NAME);
        assertThat(dto.getOwnerLastName()).isEqualTo(LAST_NAME);
        assertThat(dto.getOwnerEmailAddress()).isEqualTo(EMAIL_ADDRESS);
        assertThat(dto.getOwnerPhoneNumberPrefix()).isEqualTo(PHONE_PREFIX);
        assertThat(dto.getOwnerPhoneNumberNumber()).isEqualTo(PHONE_NUMBER);
        assertThat(dto.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(dto.getWatchers())
                .hasSize(1)
                .anySatisfy(watcherDto -> {
                    assertThat(watcherDto.getFirstName()).isEqualTo(ANOTHER_FIRST_NAME);
                    assertThat(watcherDto.getLastName()).isEqualTo(ANOTHER_LAST_NAME);
                    assertThat(watcherDto.getEmailAddress()).isEqualTo(ANOTHER_EMAIL_ADDRESS);
                    assertThat(watcherDto.getPhonePrefix()).isEqualTo(ANOTHER_PHONE_PREFIX);
                    assertThat(watcherDto.getPhoneNumber()).isEqualTo(ANOTHER_PHONE_NUMBER);
                });
    }

    private Epic existingEpic() {
        Epic epic = epicWithoutOwner();
        Owner owner = new Owner();
        owner.setFirstName(FIRST_NAME);
        owner.setLastName(LAST_NAME);
        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddress(EMAIL_ADDRESS);
        owner.setEmailAddress(emailAddress);
        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPrefix(PHONE_PREFIX);
        phoneNumber.setNumber(PHONE_NUMBER);
        owner.setPhoneNumber(phoneNumber);
        epic.setOwner(owner);
        epic.addWatcher(watcher());

        return epic;
    }

    @Test
    void shouldNotCreateInCaseOfNotExistingProject() {
        given(projectRepository.existsById(PROJECT_ID)).willReturn(false);
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project()));
        given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner()));

        ResponseEntity<Long> actual = controller.create(newEpicDto());

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.FAILED_DEPENDENCY);
    }

    @Test
    void shouldNotCreateInCaseOfNotExistingUser() {
        given(projectRepository.existsById(PROJECT_ID)).willReturn(true);
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project()));
        given(userRepository.findById(OWNER_ID)).willReturn(Optional.empty());

        ResponseEntity<Long> actual = controller.create(newEpicDto());

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.FAILED_DEPENDENCY);
    }

    @Test
    void shouldCreateProject() {
        given(projectRepository.existsById(PROJECT_ID)).willReturn(true);
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project()));
        given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner()));
        given(epicRepository.save(any())).willReturn(epicWithId());

        ResponseEntity<Long> actual = controller.create(newEpicDto());

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isEqualTo(EPIC_ID);
        then(epicRepository).should().save(epicCaptor.capture());
        Epic epic = epicCaptor.getValue();
        assertThat(epic.getTitle()).isEqualTo(TITLE);
        assertThat(epic.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(epic.getStatus()).isEqualTo(STATUS);
        assertThat(epic.getOwner().getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(epic.getOwner().getLastName()).isEqualTo(LAST_NAME);
        assertThat(epic.getOwner().getEmailAddress().getEmailAddress()).isEqualTo(EMAIL_ADDRESS);
        assertThat(epic.getOwner().getPhoneNumber().getPrefix()).isEqualTo(PHONE_PREFIX);
        assertThat(epic.getOwner().getPhoneNumber().getNumber()).isEqualTo(PHONE_NUMBER);
        assertThat(epic.getProject().getId()).isEqualTo(PROJECT_ID);
    }

    private EpicDto newEpicDto() {
        EpicDto dto = new EpicDto();
        dto.setTitle(TITLE);
        dto.setDescription(DESCRIPTION);
        dto.setStatus(STATUS.name());
        dto.setOwnerId(OWNER_ID);
        dto.setProjectId(PROJECT_ID);

        return dto;
    }

    @Test
    void shouldNotUpdateNotExistingEpic() {
        given(epicRepository.existsById(EPIC_ID)).willReturn(false);

        ResponseEntity<Void> actual = controller.update(EPIC_ID, updateEpicDto());

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotUpdateWithNonExistingUser() {
        given(epicRepository.existsById(EPIC_ID)).willReturn(true);
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.of(epicWithoutOwner()));
        given(userRepository.existsById(OWNER_ID)).willReturn(false);

        ResponseEntity<Void> actual = controller.update(EPIC_ID, updateEpicDto());

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.FAILED_DEPENDENCY);
    }

    @Test
    void shouldUpdateExistingEpicWithOwner() {
        given(epicRepository.existsById(EPIC_ID)).willReturn(true);
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.of(epic()));

        ResponseEntity<Void> actual = controller.update(EPIC_ID, updateEpicDto());

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(epicRepository).should().save(epicCaptor.capture());
        Epic epic = epicCaptor.getValue();
        assertThat(epic.getTitle()).isEqualTo(TITLE);
        assertThat(epic.getDescription()).isEqualTo("new description");
        assertThat(epic.getStatus()).isEqualTo(TO_BE_DEFINED);
        assertThat(epic.getOwner().getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(epic.getOwner().getLastName()).isEqualTo(LAST_NAME);
        assertThat(epic.getOwner().getEmailAddress().getEmailAddress()).isEqualTo("john.doe@test.com");
        assertThat(epic.getOwner().getPhoneNumber().getPrefix()).isEqualTo("9900");
        assertThat(epic.getOwner().getPhoneNumber().getNumber()).isEqualTo("8877665544");
        assertThat(epic.getProject().getId()).isEqualTo(PROJECT_ID);
    }

    @Test
    void shouldUpdateExistingEpicWithoutOwner() {
        given(epicRepository.existsById(EPIC_ID)).willReturn(true);
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.of(epicWithoutOwner()));
        given(userRepository.existsById(OWNER_ID)).willReturn(true);
        given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner()));

        ResponseEntity<Void> actual = controller.update(EPIC_ID, updateEpicDto());

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(epicRepository).should().save(epicCaptor.capture());
        Epic epic = epicCaptor.getValue();
        assertThat(epic.getTitle()).isEqualTo(TITLE);
        assertThat(epic.getDescription()).isEqualTo("new description");
        assertThat(epic.getStatus()).isEqualTo(TO_BE_DEFINED);
        assertThat(epic.getOwner().getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(epic.getOwner().getLastName()).isEqualTo(LAST_NAME);
        assertThat(epic.getOwner().getEmailAddress().getEmailAddress()).isEqualTo(EMAIL_ADDRESS);
        assertThat(epic.getOwner().getPhoneNumber().getPrefix()).isEqualTo(PHONE_PREFIX);
        assertThat(epic.getOwner().getPhoneNumber().getNumber()).isEqualTo(PHONE_NUMBER);
        assertThat(epic.getProject().getId()).isEqualTo(PROJECT_ID);
    }

    private EpicDto updateEpicDto() {
        EpicDto dto = new EpicDto();
        dto.setDescription("new description");
        dto.setStatus("TO_BE_DEFINED");
        dto.setOwnerEmailAddress("john.doe@test.com");
        dto.setOwnerPhoneNumberPrefix("9900");
        dto.setOwnerPhoneNumberNumber("8877665544");
        dto.setOwnerId(OWNER_ID);

        return dto;
    }

    private Epic epic() {
        Epic epic = epicWithoutOwner();
        Owner owner = new Owner();
        owner.setFirstName(FIRST_NAME);
        owner.setLastName(LAST_NAME);
        EmailAddress emailAddress = new EmailAddress();
        owner.setEmailAddress(emailAddress);
        PhoneNumber phoneNumber = new PhoneNumber();
        owner.setPhoneNumber(phoneNumber);
        epic.setOwner(owner);

        return epic;
    }

    private Epic epicWithoutOwner() {
        Epic epic = epicWithId();
        epic.setTitle(TITLE);
        epic.setDescription(DESCRIPTION);
        epic.setStatus(STATUS);
        epic.setProject(project());

        return epic;
    }

    @Test
    void shouldNotDeleteNotExistingEpic() {
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.empty());

        ResponseEntity<Void> actual = controller.delete(EPIC_ID);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldDeleteEpic() {
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.of(epicWithId()));

        ResponseEntity<Void> actual = controller.delete(EPIC_ID);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(epicRepository).should().delete(epicCaptor.capture());
        assertThat(epicCaptor.getValue().getId()).isEqualTo(EPIC_ID);
    }

    @Test
    void shouldNotAddWatcherToNotExistingEpic() {
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.empty());

        ResponseEntity<Void> actual = controller.addWatcher(EPIC_ID, watcherDto());

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotAddNotExistingWatcherToEpic() {
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.of(epicWithId()));
        given(userRepository.findById(WATCHER_ID)).willReturn(Optional.empty());

        ResponseEntity<Void> actual = controller.addWatcher(EPIC_ID, watcherDto());

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.FAILED_DEPENDENCY);
    }

    @Test
    void shouldAddWatcherToEpic() {
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.of(epicWithId()));
        given(userRepository.findById(WATCHER_ID)).willReturn(Optional.of(userWithId(WATCHER_ID)));

        ResponseEntity<Void> actual = controller.addWatcher(EPIC_ID, watcherDto());

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(epicRepository).should().save(epicCaptor.capture());
        assertThat(epicCaptor.getValue().getWatchers())
                .hasSize(1)
                .anySatisfy(watcher -> {
                    assertThat(watcher.getFirstName()).isEqualTo(ANOTHER_FIRST_NAME);
                    assertThat(watcher.getLastName()).isEqualTo(ANOTHER_LAST_NAME);
                    assertThat(watcher.getEmailAddress().getEmailAddress()).isEqualTo(ANOTHER_EMAIL_ADDRESS);
                    assertThat(watcher.getPhoneNumber().getPrefix()).isEqualTo(ANOTHER_PHONE_PREFIX);
                    assertThat(watcher.getPhoneNumber().getNumber()).isEqualTo(ANOTHER_PHONE_NUMBER);
                });
    }

    @Test
    void shouldNotRemoveWatcherFromNotExistingEpic() {
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.empty());

        ResponseEntity<Void> actual = controller.removeWatcher(EPIC_ID, WATCHER_ID);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    }

    @Test
    void shouldNotRemoveNotExistingWatcherFromEpic() {
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.of(epicWithWatcher()));
        given(userRepository.findById(WATCHER_ID)).willReturn(Optional.empty());

        ResponseEntity<Void> actual = controller.removeWatcher(EPIC_ID, WATCHER_ID);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.FAILED_DEPENDENCY);
    }

    @Test
    void shouldRemoveWatcherFromEpic() {
        given(epicRepository.findById(EPIC_ID)).willReturn(Optional.of(epicWithWatcher()));
        given(userRepository.findById(WATCHER_ID)).willReturn(Optional.of(userWithId(WATCHER_ID)));

        ResponseEntity<Void> actual = controller.removeWatcher(EPIC_ID, WATCHER_ID);


        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(epicRepository).should().save(epicCaptor.capture());
        assertThat(epicCaptor.getValue().getWatchers()).isEmpty();
    }

    private Epic epicWithWatcher() {
        Epic epic = epicWithId();
        epic.addWatcher(watcher());

        return epic;
    }

    private Watcher watcher() {
        Watcher watcher = new Watcher();
        watcher.setFirstName(ANOTHER_FIRST_NAME);
        watcher.setLastName(ANOTHER_LAST_NAME);
        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddress(ANOTHER_EMAIL_ADDRESS);
        watcher.setEmailAddress(emailAddress);
        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPrefix(ANOTHER_PHONE_PREFIX);
        phoneNumber.setNumber(ANOTHER_PHONE_NUMBER);
        watcher.setPhoneNumber(phoneNumber);
        return watcher;
    }

    private WatcherDto watcherDto() {
        WatcherDto dto = new WatcherDto();
        dto.setId(WATCHER_ID);
        return dto;
    }

    private User userWithId(long userId) {
        return user(userId, ANOTHER_FIRST_NAME, ANOTHER_LAST_NAME, ANOTHER_EMAIL_ADDRESS, ANOTHER_PHONE_PREFIX, ANOTHER_PHONE_NUMBER);
    }

    private Epic epicWithId() {
        Epic epic = withId(new Epic(), EPIC_ID);
        return epic;
    }

    private Project project() {
        return withId(new Project(), PROJECT_ID);
    }

    private User owner() {
        return user(OWNER_ID, FIRST_NAME, LAST_NAME, EMAIL_ADDRESS, PHONE_PREFIX, PHONE_NUMBER);
    }

    private User user(long id, String firstName, String lastName, String email, String prefix, String number) {
        User user = withId(new User(), id);
        UserName userName = new UserName();
        userName.setFirstName(firstName);
        userName.setLastName(lastName);
        user.setUserName(userName);
        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddress(email);
        user.setEmailAddress(emailAddress);
        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPrefix(prefix);
        phoneNumber.setNumber(number);
        user.setPhoneNumber(phoneNumber);
        return user;
    }

    private <T> T withId(T entity, long id) {
        try {
            Field fieldId = entity.getClass().getDeclaredField("id");
            fieldId.setAccessible(true);
            fieldId.set(entity, id);
            return entity;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}