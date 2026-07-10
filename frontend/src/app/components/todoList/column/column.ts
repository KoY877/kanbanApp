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
import { forkJoin, of } from 'rxjs';
import { CdkDragDrop, CdkDrag, CdkDropList, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { FormatTextPipe } from '../../../pipes/format-text-pipe';

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
    if (!this.column.length) return;

    // Initialize tasks as an empty array for each column
    this.column.forEach(col => {
      if (!col.tasks) col.tasks = [];
    });

    const requests = this.column.map(col =>
      col.id ? this.entityService.getData<any>(`tasks/column/${col.id}`) : of([])
    );

    forkJoin(requests).subscribe({
      next: (taskArrays: any[]) => {
        taskArrays.forEach((tasks, i) => {
          this.column[i] = { ...this.column[i], tasks: tasks || [] };
        });
        this.cdr.detectChanges();
      },
      error: () => {},
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

    this.selectedColumnId = columnId;
    this.isOpenTaskModal = true
    this.isEditTask = false

    // Close all open menu dropdowns
    const dropdowns = this.elementRef.nativeElement.querySelectorAll('.menu-dropdown');
    dropdowns.forEach((dropdown: any) => {
      dropdown.classList.remove('active');
    });

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
  if (event.previousContainer === event.container) {
    moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
  } else {
    const task = event.previousContainer.data[event.previousIndex];
    const targetCol = this.column.find(col => col.tasks === event.container.data);
    const targetColumnId = targetCol?.id;

    // Check the WIP limit
    if (targetCol?.limitWorkInProgress !== null && targetCol?.limitWorkInProgress !== undefined) {
      const currentCount = event.container.data.length;
      if (currentCount >= targetCol.limitWorkInProgress) {
        return; // Limit reached -> cancel the drop
      }
    }

    if (targetColumnId && task?.id) {
      // Optimistic update
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );

      const taskPayload = {
        ...task,
        columnId: targetColumnId,
        members: (task.members || []).map((member: any) => member?.id || member),
        labels: (task.labels || []).map((label: any) => label?.name || label),
      };

      this.entityService.updateData(`tasks`, taskPayload).subscribe({
        next: () => {},
        error: (err) => {
          transferArrayItem(
            event.container.data,
            event.previousContainer.data,
            event.currentIndex,
            event.previousIndex
          );
          console.error('Error moving task:', err);
        }
      });
    }
  }
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
