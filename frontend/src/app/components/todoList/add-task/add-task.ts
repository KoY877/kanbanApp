import { ChangeDetectorRef, Component, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { Message } from '../../../services/message';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { Description } from "../description/description";
import { CommonModule } from '@angular/common';
import { CharacterPipe } from '../../../pipes/character-pipe';
import { Members } from "../members/members";
import { Labels } from "../labels/labels";
import { CamelcasePipe } from '../../../pipes/camelcase-pipe';
import { faClock, faList, faPlus, faTag, faUsers } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { EntityService } from '../../../services/entity-service';

@Component({
  selector: 'app-add-task',
  imports: [CommonModule, ReactiveFormsModule, Description, CharacterPipe, Members, Labels, CamelcasePipe, FontAwesomeModule],
  templateUrl: './add-task.html',
  styleUrl: './add-task.css',
})
export class AddTask {
  readonly faUsers = faUsers;
  readonly faListAlt = faList;
  readonly faClock = faClock;
  readonly faPlus = faPlus;
  readonly faTag = faTag;

  @Input() currentBoard!: any;
  @Input() isTaskOpen: boolean = false;
  @Input() isEditTask: boolean = false;
  @Input() taskToEdit: any = null;
  @Output() taskUpdated = new EventEmitter<any>();
  @Input() columnId!: string;
  @Output() closeTaskModal: EventEmitter<void> = new EventEmitter<void>();
  @Output() closeTimeModal: EventEmitter<void> = new EventEmitter<void>();
  @Output() clickOutside = new EventEmitter<void>();
  @Output() taskCreated = new EventEmitter<any>();

  private destroy$ = new Subject<void>();

  selectedItem: any = { name: 'Yellow', colorChoice: '#f5cc00', bgColor: '#ffffe0' };
  myModal: any
  defaultColors: Array<any> = []
  members: any[] = []
  selectedMembers: any[] = []
  selectedMember?: any | undefined[] = []
  selectedLabel?: any | undefined[] = []
  selectedLabels: any[] = []
  time: number | undefined;
  isMembers: boolean = false;
  isLabels: boolean = false;
  isEstimateTime: boolean = false;
  isTimeValue: boolean = false;
  isDescription: boolean = false;

  isMemberSelected: Boolean = false
  isLabelSelected: Boolean = false
  form: FormGroup

  constructor(
    private fb: FormBuilder,
    private message: Message,
    private elementRef: ElementRef,
    private cdr: ChangeDetectorRef,
    private entityService: EntityService,
  ) {
    this.form = fb.group({
      name: ['', [Validators.required, Validators.minLength(1)]],
      colors: this.fb.array([]),
      members: this.fb.array([]),
      labels: this.fb.array([]),
      description: [''],
      date: [''],
      time: [''],
    });
  }

  /**
   * Lifecycle hook: populate the color choices and subscribe to the
   * members-dropdown open/close events.
   */
  ngOnInit(): void {
    this.handleColorArray();

    this.message.openMembersDropdown$.pipe(takeUntil(this.destroy$)).subscribe((msg: boolean) => {
      this.isMembers = msg;
      this.cdr.detectChanges();
    });
  }

  /**
   * Lifecycle hook: when editing an existing task, pre-fill the form,
   * selected members/labels, color and time from `taskToEdit`.
   */
  ngAfterViewInit(): void {
    // Pre-fill after the view has initialized
    if (this.isEditTask && this.taskToEdit) {
      this.form.patchValue({
        name: this.taskToEdit.name,
        description: this.taskToEdit.description || '',
      });

      // Pre-fill members, marking them as 'checked: true'
      if (this.taskToEdit.members?.length) {
        this.selectedMembers = this.taskToEdit.members.map((m: any) => ({
          ...m,
          checked: true
        }));
        this.isMemberSelected = true;
      }

      // Pre-fill labels
      if (this.taskToEdit.labels?.length) {
        this.selectedLabels = this.taskToEdit.labels.map((label: string | any) => ({
          label: typeof label === 'string' ? label : label.label || label,
          checked: true
        }));
        this.isLabelSelected = true;
      }

      // Pre-fill the color
      if (this.taskToEdit.colors?.[0]) {
        const color = this.defaultColors.find(c => c.colorChoice === this.taskToEdit.colors[0]);
        if (color) {
          this.selectedItem = color;
        }
      }

      // Handle the time estimate
      if (this.taskToEdit.time) {
        const timeValue = this.taskToEdit.time.toString().split(':')[0];
        this.time = parseInt(timeValue);
        this.form.patchValue({ time: this.time });
        this.isTimeValue = true;
      }

      this.cdr.detectChanges();
    }
  }

  /** Lifecycle hook: complete the destroy$ subject to unsubscribe all pipes. */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** The FormArray of available color choices. */
  get colors(): FormArray {
    return this.form?.get('colors') as FormArray;
  }

  /** The FormArray of available label choices. */
  get labels(): FormArray {
    return this.form?.get('labels') as FormArray;
  }

  /**
   * When a click occurs outside this component while the task modal is
   * open, close any open sub-panel (members/labels/description/time) first,
   * or emit clickOutside if none is open.
   * @param event - the document click event
   */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {

      // Check if the task modal is open
      if (this.isTaskOpen === true) {

        if (this.isMembers === true || this.isLabels === true || this.isDescription === true || this.isEstimateTime === true) {
          this.isMembers = false;
          this.isLabels = false;
          this.isDescription = false;
          this.isEstimateTime = false;
          this.cdr.detectChanges();
        } else {
          this.clickOutside.emit();
        }

      }
    }
  }

  /**
   * Update the selected color swatch.
   * @param data - a form control change event carrying the new color value
   */
  selectItem(data: any) {
    this.selectedItem = data.value;
  }

  /** Populate the default color palette and its backing FormArray. */
  handleColorArray() {
    this.defaultColors = [
      { name: 'Yellow', colorChoice: '#f5cc00', bgColor: '#ffffe0' },
      { name: 'Red', colorChoice: '#ff858f', bgColor: '#ffccd0' },
      { name: 'Orange', colorChoice: '#faa200', bgColor: '#ffeac2' },
      { name: 'Blue', colorChoice: '#70b0ff', bgColor: '#cce3ff' }
    ];

    this.defaultColors.forEach((color) => {
      this.colors.push(this.fb.group({
        name: [color.name],
        colorChoice: [color.colorChoice],
        bgColor: [color.bgColor]
      }),);
    });

    return this.colors
  }

  /**
   * Toggle one of the sub-panels (member/label/description/time), closing
   * the others.
   * @param event - which panel to toggle: 'member' | 'label' | 'description' | 'time'
   * @returns the current members list (for the 'member' case) or void
   */
  handleDropdown(event: string) {

    // for Members
if (event === 'member') {
  if (this.isMembers === false) {
    this.isMembers = true;
    this.isLabels = false;
    this.isDescription = false;
    this.isEstimateTime = false;

    // Load the board's members
    this.members = this.currentBoard[0].members;

    // In edit mode, mark the already-selected members as checked
    if (this.isEditTask && this.selectedMembers.length > 0) {
      this.members = this.members.map((member: any) => ({
        ...member,
        checked: this.selectedMembers.some((sm: any) =>
          (sm.id || sm) === (member.id || member)
        )
      }));
    }
  } else {
    this.isMembers = false;
  }
  this.cdr.detectChanges();
}

    // for Labels
    if (event === 'label') {
      if (this.isLabels === false) {
        this.isLabels = true
        this.isMembers = false
        this.isDescription = false
        this.isEstimateTime = false
      } else {
        this.isLabels = false
      }
    }

    if (event === 'description') {
      if (this.isDescription === false) {
        this.isDescription = true
        this.isMembers = false
        this.isLabels = false
        this.isEstimateTime = false
      } else {
        this.isDescription = false
      }
    }

    if (event === 'time') {

      if (this.isEstimateTime === false) {
        this.isEstimateTime = true
        this.isMembers = false
        this.isLabels = false
        this.isDescription = false
      } else {
        this.isEstimateTime = false
      }
    }

    this.cdr.detectChanges();
    return this.members
  }

  /**
   * Receive the list of newly checked members and merge them into the
   * selected-members list.
   * @param event - array of member objects that were checked
   * @returns the updated selected members list
   */
  handleSelectedMember(event: any) {
    this.selectedMember = event

    if (this.selectedMember.length > 0) {
      this.isMemberSelected = true
    }

    this.selectedMember?.forEach((element: any) => {
      this.selectedMembers = this.selectedMembers?.filter((item: any) => item !== element);

      if (this.selectedMembers) {
        this.selectedMembers.push(element);
      } else {
        // If members2 is null/undefined, initialize it with an array containing the element
        this.selectedMembers = [element];
      }

    })

    return this.selectedMembers;
  }

  /**
   * Remove a deselected member from the selected-members list.
   * @param event - the deselected member's email
   * @returns the updated selected members list
   */
  handleDeselectedMember(event: any) {
    // event carries the deselected member's email
    this.selectedMembers = this.selectedMembers?.filter((item: any) => {
      const itemEmail = item?.memberEmail || item;
      return itemEmail !== event;
    });

    // Hide the section if no member remains selected
    if (this.selectedMembers.length === 0) {
      this.isMemberSelected = false;
    }

    this.cdr.detectChanges();

    return this.selectedMembers;
  }

  /**
   * Receive the list of currently checked labels.
   * @param event - array of checked label objects
   * @returns the updated selected labels list
   */
  handleSelectedLabel(event: any) {

    this.selectedLabel = event

    if (this.selectedLabel && this.selectedLabel.length > 0) {
      this.isLabelSelected = true
      this.selectedLabels = event
    } else {
      this.isLabelSelected = false
      this.selectedLabels = []
    }

    return this.selectedLabels
  }

  /**
   * Update the selected labels after a label is deselected.
   * @param event - the updated array of selected labels
   * @returns the updated selected labels list
   */
  handleDeselectedLabel(event: any) {
    this.selectedLabels = event || []

    if (this.selectedLabels.length === 0) {
      this.isLabelSelected = false
    }

    this.cdr.detectChanges();

    return this.selectedLabels
  }

  /**
   * Sync the description form control from the nested Description component.
   * @param value - the new description text
   */
  handleDescriptionChange(value: string) {
    this.form.get('description')?.setValue(value, { emitEvent: false });
  }

  /** Close the add/edit task modal. */
  handleCloseModal() {
    this.closeTaskModal.emit()
  }

  /** Read the time-estimate form control and flag whether it has a value. */
  handleAddTime() {
    this.time = this.form.get('time')?.value;

    if (this.time && this.time !== undefined) {
      this.isTimeValue = true;
    } else {
      this.isTimeValue = false;
    }
  }

  /**
   * Build the task payload from the form and either update the task being
   * edited or create a new one.
   */
  handleSubmitAddTask() {
    if (this.form.get('name')?.invalid) return;

    const taskData = {
      name: this.form.get('name')?.value,
      description: this.form.get('description')?.value || null,
      members: this.selectedMembers.map((m: any) => m?.id || m),
      labels: this.selectedLabels.map((l: any) => l.label),
      colors: this.selectedItem?.colorChoice ? [this.selectedItem.colorChoice] : [],
      time: this.time ? `${String(this.time).padStart(2, '0')}:00:00` : null,
      columnId: this.columnId,
    };

    if (this.isEditTask && this.taskToEdit) {
      // Edit mode
      this.entityService.updateData('tasks', { ...taskData, id: this.taskToEdit.id })
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (task) => {
            this.taskUpdated.emit(task);
            this.form.reset();
          },
          error: (err) => console.error('Error updating task:', err),
        });
    } else {
      // Creation mode
      this.entityService.addData<any>('tasks', taskData)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (task) => {
            this.taskCreated.emit(task);
            this.form.reset();
          },
          error: (err) => console.error('Error creating task:', err),
        });
    }
  }

}
