package com.kanban.kanbanapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.kanban.kanbanapp.Data_Transfer_Object.TaskCreateRequest;
import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.KanbanColumn;
import com.kanban.kanbanapp.Model.Task;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.exception.WipLimitExceededException;
import com.kanban.kanbanapp.repository.KanbanColumnRepository;
import com.kanban.kanbanapp.repository.MemberRepository;
import com.kanban.kanbanapp.repository.TaskRepository;

/**
 * Unit tests for {@link TaskService}, focused on both correct behavior and
 * the object-level authorization checks that scope every operation to the
 * board owned by the authenticated user.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final String OWNER_ID = "owner-1";
    private static final String OTHER_USER_ID = "intruder-1";

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private KanbanColumnRepository kanbanColumnRepository;

    @Mock
    private MemberRepository memberRepository;

    private TaskService taskService;

    private Board board;
    private KanbanColumn column;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, kanbanColumnRepository, memberRepository);

        User owner = new User();
        owner.setId(OWNER_ID);

        board = new Board();
        board.setId("board-1");
        board.setUser(owner);

        column = new KanbanColumn();
        column.setId("column-1");
        column.setBoard(board);
    }

    private TaskCreateRequest requestFor(String columnId) {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setName("Write tests");
        request.setColumnId(columnId);
        return request;
    }

    // --- createTask ---

    @Test
    void createTask_savesTask_whenUserOwnsTargetColumnBoard() {
        when(kanbanColumnRepository.findByIdAndBoard_User_Id(column.getId(), OWNER_ID))
            .thenReturn(Optional.of(column));
        when(taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(column.getId()))
            .thenReturn(List.of());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task saved = taskService.createTask(requestFor(column.getId()), OWNER_ID);

        assertThat(saved.getName()).isEqualTo("Write tests");
        assertThat(saved.getColumn()).isEqualTo(column);
    }

    @Test
    void createTask_throwsAccessDenied_whenUserDoesNotOwnTargetColumnBoard() {
        when(kanbanColumnRepository.findByIdAndBoard_User_Id(column.getId(), OTHER_USER_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(requestFor(column.getId()), OTHER_USER_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_throwsWipLimitExceeded_whenColumnAtLimit() {
        column.setLimitWorkInProgress(1);
        when(kanbanColumnRepository.findByIdAndBoard_User_Id(column.getId(), OWNER_ID))
            .thenReturn(Optional.of(column));
        when(taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(column.getId()))
            .thenReturn(List.of(new Task()));

        assertThatThrownBy(() -> taskService.createTask(requestFor(column.getId()), OWNER_ID))
            .isInstanceOf(WipLimitExceededException.class);

        verify(taskRepository, never()).save(any());
    }

    // --- getTaskById ---

    @Test
    void getTaskById_returnsTask_whenOwnedByUser() {
        Task task = new Task();
        task.setId("task-1");
        task.setColumn(column);
        when(taskRepository.findByIdAndColumn_Board_User_Id(task.getId(), OWNER_ID))
            .thenReturn(Optional.of(task));

        Task result = taskService.getTaskById(task.getId(), OWNER_ID);

        assertThat(result).isEqualTo(task);
    }

    @Test
    void getTaskById_throwsAccessDenied_whenNotOwnedByUser() {
        when(taskRepository.findByIdAndColumn_Board_User_Id("task-1", OTHER_USER_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById("task-1", OTHER_USER_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    // --- getTasksByColumn ---

    @Test
    void getTasksByColumn_returnsTasks_whenColumnOwnedByUser() {
        Task task = new Task();
        task.setId("task-1");
        task.setColumn(column);
        when(kanbanColumnRepository.findByIdAndBoard_User_Id(column.getId(), OWNER_ID))
            .thenReturn(Optional.of(column));
        when(taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(column.getId()))
            .thenReturn(List.of(task));

        List<Task> result = taskService.getTasksByColumn(column.getId(), OWNER_ID);

        assertThat(result).containsExactly(task);
    }

    @Test
    void getTasksByColumn_throwsAccessDenied_whenColumnNotOwnedByUser() {
        when(kanbanColumnRepository.findByIdAndBoard_User_Id(column.getId(), OTHER_USER_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTasksByColumn(column.getId(), OTHER_USER_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).findAllByColumn_IdOrderByTaskOrderAsc(any());
    }

    // --- deleteTask ---

    @Test
    void deleteTask_deletesTaskAndReordersColumn_whenOwnedByUser() {
        Task task = new Task();
        task.setId("task-1");
        task.setColumn(column);
        when(taskRepository.findByIdAndColumn_Board_User_Id(task.getId(), OWNER_ID))
            .thenReturn(Optional.of(task));
        when(taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(column.getId()))
            .thenReturn(List.of());

        taskService.deleteTask(task.getId(), OWNER_ID);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_throwsAccessDenied_whenNotOwnedByUser() {
        when(taskRepository.findByIdAndColumn_Board_User_Id("task-1", OTHER_USER_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask("task-1", OTHER_USER_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).delete(any());
    }

    // --- updateTask ---

    @Test
    void updateTask_updatesTask_whenUserOwnsTaskAndTargetColumn() {
        Task task = new Task();
        task.setId("task-1");
        task.setColumn(column);
        when(taskRepository.findByIdAndColumn_Board_User_Id(task.getId(), OWNER_ID))
            .thenReturn(Optional.of(task));
        when(kanbanColumnRepository.findByIdAndBoard_User_Id(column.getId(), OWNER_ID))
            .thenReturn(Optional.of(column));
        when(taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(column.getId()))
            .thenReturn(List.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.updateTask(task.getId(), requestFor(column.getId()), OWNER_ID);

        assertThat(result.getName()).isEqualTo("Write tests");
    }

    @Test
    void updateTask_throwsAccessDenied_whenTaskNotOwnedByUser() {
        when(taskRepository.findByIdAndColumn_Board_User_Id("task-1", OTHER_USER_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask("task-1", requestFor(column.getId()), OTHER_USER_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(kanbanColumnRepository, never()).findByIdAndBoard_User_Id(any(), any());
    }

    @Test
    void updateTask_throwsAccessDenied_whenTargetColumnNotOwnedByUser() {
        Task task = new Task();
        task.setId("task-1");
        task.setColumn(column);
        when(taskRepository.findByIdAndColumn_Board_User_Id(task.getId(), OWNER_ID))
            .thenReturn(Optional.of(task));

        String otherUsersColumnId = "column-owned-by-someone-else";
        when(kanbanColumnRepository.findByIdAndBoard_User_Id(otherUsersColumnId, OWNER_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(task.getId(), requestFor(otherUsersColumnId), OWNER_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).save(any());
    }
}
