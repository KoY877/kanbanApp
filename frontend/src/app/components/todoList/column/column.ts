import { ChangeDetectorRef, Component, ElementRef, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Message } from '../../../services/message';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { AddTask } from "../add-task/add-task";
import { Column } from '../../../models/column.model';
import { faPlus} from '@fortawesome/free-solid-svg-icons';
import { FaIconComponent } from "@fortawesome/angular-fontawesome";
import { EntityService } from '../../../services/entity-service';
import { CharacterPipe } from '../../../pipes/character-pipe';
import { catchError, forkJoin, of } from 'rxjs';
import { CdkDragDrop, CdkDrag, CdkDropList, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { FormatTextPipe } from '../../../pipes/format-text-pipe';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-column',
  imports: [CommonModule, ReactiveFormsModule, AddTask, FaIconComponent, CharacterPipe, FormatTextPipe, CdkDropList, CdkDrag],
  templateUrl: './column.html',
  styleUrl: './column.css',
})
export class List implements OnChanges {
  readonly faPlus = faPlus

  @Input() clikedBoard!: any;

  isOpenTaskModal?: boolean = false
  isEditTask: boolean = false
  selectedTask: any = null
  column : Column[] = []
  selectedColumnId: string = ''
  taskLoadError = '';
  taskMoveError = '';
  isMovingTask = false;

  constructor(
    private message: Message,
    private elementRef: ElementRef,
    private entityService: EntityService,
    private cdr: ChangeDetectorRef,
  ){}

  ngOnInit(): void {}

  /**
   * Lifecycle hook: when the clicked board changes, load its columns and
   * fetch the tasks for each of them.
   * @param changes - Angular's input change record
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['clikedBoard'] && this.clikedBoard?.[0]?.columns) {
      this.column = this.clikedBoard[0].columns;
      this.loadTasksForColumns();
    }
  }

  /**
   * Fetch the tasks of every column in parallel and attach them to their
   * respective column objects.
   */
  private loadTasksForColumns(): void {
    this.taskLoadError = '';
    if (!this.column.length) return;

    // Initialize tasks as an empty array for each column
    this.column.forEach(col => {
      if (!col.tasks) col.tasks = [];
    });

    const columns = this.column;
    const requests = columns.map(col =>
      col.id ? this.entityService.getData<any>(`tasks/column/${col.id}`).pipe(
        catchError(error => {
          if (this.column === columns) {
            this.taskLoadError = `Impossible de charger les tÃ¢ches de Â« ${col.columnName} Â» (HTTP ${error.status ?? '?'}).`;
          }
          console.error('Error loading tasks for column:', col.id, error);
          return of(col.tasks ?? []);
        })
      ) : of([])
    );

    forkJoin(requests).subscribe({
      next: (taskArrays: any[]) => {
        if (this.column !== columns) return;
        taskArrays.forEach((tasks, i) => {
          this.column[i] = { ...this.column[i], tasks: tasks || [] };
        });
        this.cdr.detectChanges();
      },
      error: (error) => {
        const status = error instanceof HttpErrorResponse ? error.status : undefined;
        this.taskLoadError = status === 0
          ? 'Impossible de joindre le serveur pour charger les tÃ¢ches. VÃ©rifiez que le backend est dÃ©marrÃ©.'
          : `Impossible de charger les tÃ¢ches${status ? ` (HTTP ${status})` : ''}. Consultez la rÃ©ponse de la requÃªte dans lâ€™onglet RÃ©seau du navigateur.`;
        console.error('Error loading column tasks:', error);
        this.cdr.detectChanges();
      },
    });
  }

  /** The board's columns, or an empty array if none are loaded yet. */
  get filteredTasks() {
    if (this.clikedBoard && this.clikedBoard[0] && this.clikedBoard[0].columns) {
      return this.column;
    }
    return [];
  }

  /**
   * Open the add-task modal for a given column, closing any open menu
   * dropdowns and registering a one-shot outside-click listener to close it.
   * Does nothing if the column has already reached its WIP limit.
   * @param columnId - the column to add the task to
   */
  handleAddTask(columnId: string) {
    const col = this.column.find(c => c.id === columnId);
    const limit = col?.limitWorkInProgress;
    if (limit !== null && limit !== undefined && (col?.tasks?.length ?? 0) >= limit) {
      alert(`Column "${col?.columnName}" has reached its WIP limit of ${limit}`);
      return;
    }

    this.message.messageBooleanCloseAll(true);
    this.selectedColumnId = columnId;
    this.isOpenTaskModal = true
    this.isEditTask = false

    // Register a click listener to detect clicks outside the modal
    const clickListener = (clickEvent: any) => {
      if (!this.elementRef.nativeElement.contains(clickEvent.target)) {
        this.isOpenTaskModal = false;
        this.isEditTask = false;
        document.removeEventListener('click', clickListener);
      }
    };

    document.addEventListener('click', clickListener);
  }

  /** Close the add/edit task modal. */
  handleCloseTaskModal(){
    this.isOpenTaskModal = false
    this.isEditTask = false
  }

  /**
   * Append a newly created task to its column's local task list.
   * @param newTask - the task returned by the backend
   */
  handleTaskCreated(newTask: any) {

    const col = this.column.find(col => col.id === this.selectedColumnId);
    if (col) {
      if (!col.tasks) col.tasks = [];
      col.tasks = [...col.tasks, newTask];
    }
    this.isOpenTaskModal = false;
  }

 /**
  * Handle a drag-and-drop task move, either reordering within a column or
  * transferring it to another column (respecting its WIP limit), with an
  * optimistic update rolled back on API failure.
  * @param event - the CDK drag-drop event
  */
 drop(event: CdkDragDrop<any[], any[]>) {
  if (this.isMovingTask) return;
  const source = event.previousContainer.data;
  const destination = event.container.data;
  const task = source[event.previousIndex];
  const target = this.column.find(col => col.tasks === destination);
  if (!task?.id || !target?.id) return;
  if (source !== destination && target.limitWorkInProgress != null &&
      destination.length >= target.limitWorkInProgress) return;

  const sourceSnapshot = [...source];
  const destinationSnapshot = [...destination];
  if (source === destination) {
    moveItemInArray(destination, event.previousIndex, event.currentIndex);
  } else {
    transferArrayItem(source, destination, event.previousIndex, event.currentIndex);
  }
  this.isMovingTask = true;
  this.taskMoveError = '';
  this.entityService.putData<any>('tasks', `${task.id}/move`, {
    columnId: target.id,
    position: event.currentIndex,
  }).subscribe({
    next: updated => {
      Object.assign(task, updated);
      source.forEach((item, index) => item.taskOrder = index);
      destination.forEach((item, index) => item.taskOrder = index);
      this.isMovingTask = false;
      this.cdr.detectChanges();
    },
    error: error => {
      source.splice(0, source.length, ...sourceSnapshot);
      if (source !== destination) destination.splice(0, destination.length, ...destinationSnapshot);
      this.taskMoveError = 'Le déplacement n’a pas été enregistré. La tâche a été remise à sa place.';
      this.isMovingTask = false;
      console.error('Error moving task:', error);
      this.cdr.detectChanges();
    },
  });
 }
  /** CDK drop-list connection ids, one per column. */
  get connectedDropLists(): string[] {
    return this.column.map((_, i) => 'drop-list-' + i);
  }

  /**
   * Pick a color from a fixed palette for a column header, based on its index.
   * @param num - the column index
   * @returns a hex color string
   */
  generateRandomColor(num: number): string {
    const colors = [
      '#5595E8', '#C9A700', '#A5889E', '#4CAF50', '#E57373',
      '#64B5F6', '#FFB74D', '#81C784', '#BA68C8', '#4DB6AC'
    ];
    return colors[num % colors.length];
  }

  /**
   * Open the task modal in edit mode for the given task.
   * @param task - the task to edit
   */
  editTask(task: any) {
    this.message.messageBooleanCloseAll(true);
    this.isEditTask = true;
    this.selectedTask = task;
    this.selectedColumnId = task.columnId;
    this.isOpenTaskModal = true;
  }

  /**
   * Replace the updated task in its column's local task list and close the
   * edit modal.
   * @param updatedTask - the task returned by the backend after update
   */
  handleTaskUpdated(updatedTask: any) {
    // Find and update the task within its column
    const column = this.column.find(col => col.id === updatedTask.columnId);
    if (column && column.tasks) {
      const taskIndex = column.tasks.findIndex(t => t.id === updatedTask.id);
      if (taskIndex !== -1) {
        column.tasks[taskIndex] = updatedTask;
      }
    }
    this.isOpenTaskModal = false;
    this.isEditTask = false;
    this.selectedTask = null;
  }
}
