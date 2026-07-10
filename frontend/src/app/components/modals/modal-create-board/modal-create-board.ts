import { ChangeDetectorRef, Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Board } from '../../board/board';
import { EntityService } from '../../../services/entity-service';
import { Message } from '../../../services/message';
import { AuthService } from '../../../services/authentication/auth-service';
import { lastValueFrom } from 'rxjs';
import { CommonModule } from '@angular/common';
import { CamelcasePipe } from '../../../pipes/camelcase-pipe';
import { Reload } from '../../../services/reload';

@Component({
  selector: 'app-modal-create-board',
  imports: [CommonModule, ReactiveFormsModule, CamelcasePipe, FormsModule],
  templateUrl: './modal-create-board.html',
  styleUrl: './modal-create-board.css',
})
export class ModalCreateBoard {
  @Input() modelData?: string
  @Output() closeModal: EventEmitter<void> = new EventEmitter<void>()
  @Output() openBoard: EventEmitter<boolean> = new EventEmitter<boolean>()

  isSubmitted: Boolean = true
  isDisplayModal : boolean = false
  isColumnsModal : boolean = true
  isButtonRemove : boolean = true
  isDone : boolean = false
  isDoneDeleted : boolean = false
  limitProgress : boolean = false
  isEmailCreate : boolean = false
  isExpanded: boolean = false
  defaultColumns: Array<any> = []
  submitColumns: Array<any> = []
  submitFinishedTask: Array<any> = []
  step3_columns: Array<any> = []
  step4_columns: Array<any> = []
  orderColumns: Array<any> = []
  draggedIndex: number | null = null
  columnsLength : number =  1
  selectedIndex: number | null = null;
  isModalVisible: boolean = false
  form: FormGroup;
  responsData: any;
  // Actuel Index
  currentStep = 0;
  errorMessage: string = '';

  constructor(
    private formBuilder: FormBuilder,
    private entityService : EntityService,
    private reload: Reload,
    private message: Message,
    private auth: AuthService,
    private cdr: ChangeDetectorRef,
  ) {

    this.form = formBuilder.group({
      name: ['', Validators.required],
      columns: this.formBuilder.array([]),
      added_columns: this.formBuilder.array([]),
      selectedTask: [''],
      globalOption: [''],
      email: ['', Validators.required],
      members: this.formBuilder.array([]),
    })

  }

  /** Lifecycle hook: pre-populate the form with the 4 default columns. */
  ngOnInit() {
    this.addDefaultColumns()
  }

  /** Advance the wizard to the next step (capped at step 5). */
  nextStep() {
    if (this.currentStep < 5) {
      this.currentStep++;
    }
  }

  /** Go back to the previous wizard step (floored at step 0). */
  prevStep() {
    if (this.currentStep > 0) {
      this.currentStep--;
    }
  }

  /** Quick access to the `name` form control. */
  get formControls() : FormArray{
    return this.form.get('name') as FormArray;
  }

  /** The FormArray backing the column rows (step 1). */
  get columns(): FormArray{
    return this.form.get('columns') as FormArray;
  }

  /** The FormArray backing the confirmed columns (steps 2+). */
  get added_columns(): FormArray{
    return this.form.get('added_columns') as FormArray;
  }

  /** Quick access to the `email` form control. */
  get email(): FormArray{
    return this.form.get('email') as FormArray;
  }

  /** The FormArray backing the members staged for invitation. */
  get members(): FormArray{
    return this.form.get('members') as FormArray;
  }

  /** Seed the `columns` FormArray with the 4 standard Kanban columns. */
  addDefaultColumns(): void {
    // Default Columns
    this.defaultColumns = [
      { name: 'To-do'},
      { name: 'Do-today'},
      { name: 'In progress'},
      { name: 'Done' }
    ]

    // Push default columns in FormArray
    this.defaultColumns.forEach((column) => {
      this.columns.push(this.formBuilder.group({
        columnName: [column.name], // Column name
      }),);
    });
  }

  /** Append a new empty column row to the `columns` FormArray. */
  addRow() {
    // Push empty row in FormArray
    this.columns.push(this.formBuilder.group({
      columnName: [''], // Empty Column
    }));

    // Show remove button when columns.length > 1
    const removeLastRow = this.columns.length

    // Show remove button
    if (removeLastRow > 1) {
      this.isButtonRemove = true
    }
  }

  /**
   * Remove a column row at the given index (a single row is always kept).
   * @param index - index of the row to remove in the `columns` FormArray
   */
  removeRow(index: number) {
    // Remove row if columns.length > 1
    if (this.columns.length > 1) {
      this.columns.removeAt(index );
    }

    // Remove button when columns.length = 1
    const removeLastRow = this.columns.length

    // Hide remove button
    if (removeLastRow === 1) {
      this.isButtonRemove = !this.isButtonRemove
    }
  }

  /**
   * Record the index of the column row being dragged.
   * @param index - the dragged row's index
   */
  onDragStart(index: any): void {
    this.draggedIndex = index;
  }

  /** Clear the dragged-row index once dragging ends. */
  onDragEnd(): void {
    this.draggedIndex = null;
  }

  /**
   * Reorder the column rows by moving the dragged row to the drop target.
   * @param event - the native drag-drop event
   * @param targetIndex - index the row was dropped onto
   */
  onDrop(event: DragEvent, targetIndex: any): void {
    event.preventDefault();

    if(this.draggedIndex !== null && this.draggedIndex !== targetIndex) {
      const draggedRow = this.columns.at(this.draggedIndex);
      this.columns.removeAt(this.draggedIndex);
      this.columns.insert(targetIndex, draggedRow);
    }

    this.draggedIndex = null;
  }

  /**
   * Allow a drop by preventing the default dragover behavior.
   * @param event - the native dragover event
   */
  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  /**
   * Validate the board name and, if it doesn't already exist, advance to
   * step 2 (columns).
   * @param event - the form submit event
   */
  async handleNextStepPage_1(event?: Event){
    event?.preventDefault()
    this.isSubmitted = true;

    // Check the value name
    // if name is empty
    if (this.form.value.name === "")
    {
      // Show alert
      alert("Name is required");
    } else {

      // Verify if name exists in DB
      await this.verifyIfNameExistsInDB()

    }
  }

  /**
   * Check whether a board with the same name already exists for this user
   * and, if not, advance the wizard to step 2.
   */
  async verifyIfNameExistsInDB (){
    // Get all board data
    let allBoadData = await lastValueFrom(this.entityService.getData("boards/all"));

    // Filter name
    let name = allBoadData.filter((item: any) => item?.name === this.form.value.name)

    // If name not exists
    if (name.length === 0) {
      // Go to Step 2
      this.nextStep()
      // Reset isSubmitted
      this.isSubmitted = false
      // Detect changes to update the view
      this.cdr.detectChanges();
    } else {
      // Show alert
      alert("Board name already exists !");
    }
  }

  /**
   * Commit the entered column names into `added_columns`, set the initial
   * task/global-option selection, and advance to step 3.
   */
  handleNextStepPage_2() {
    // Get submited columns data
    this.submitColumns = this.form.value.columns;

    // Delete array columns data
    this.added_columns.clear()

    // Save Columns Data in the Array added_columns
    this.submitColumns.forEach((item: any)  => {
      // Toggle isDone boolean
      if (item.columnName === "Done" ){
        // Toggle isDone boolean
        this.isDone = !this.isDone
      } else {
        // Toggle isDoneDeleted boolean
        this.isDoneDeleted = !this.isDoneDeleted
      }

      // Push submited data in array columns and added_columns
      this.pushColumnsDataInArray(item)

    })

    this.setInitialSelection();

    // Next step
    this.nextStep();
  }

  /**
   * Push a submitted column into `added_columns`, defaulting
   * `limitWorkInProgress` to 3 for the "In progress" column.
   * @param item - the submitted column data (`columnName`)
   */
  pushColumnsDataInArray (item : any ){
    // Push submited data in array columns
    if(item.columnName === "In progress"){
      // Push with default limitWorkInProgress value
      this.added_columns.push(this.formBuilder.group({
        columnName: [item.columnName],
        limitWorkInProgress: [3],
      }))

    } else {
      // Push without limitWorkInProgress value
      this.added_columns.push(this.formBuilder.group({
        columnName: [item.columnName],
        limitWorkInProgress: [],
      }))
    }
  }

  /**
   * Return from step 2 to step 1, restoring the `columns` FormArray from
   * `added_columns` (falling back to `submitColumns`).
   */
  handlePrevStep() {
    // Restore the columns from added_columns
    this.columns.clear();

    // Data source: prefer added_columns, fall back to submitColumns
    const columnsData = this.added_columns.length > 0
      ? this.added_columns.value
      : this.submitColumns;

    if (columnsData && columnsData.length > 0) {
      // Repopulate the columns FormArray
      columnsData.forEach((item: any) => {
        this.columns.push(this.formBuilder.group({
          columnName: [item.columnName || '']
        }));
      });
    }

    // Reset flags
    this.isDone = false;
    this.isDoneDeleted = false;

    this.setInitialSelection();

    // Previous step
    this.prevStep();
  }

  /** Toggle the expanded/collapsed state of a truncated text block. */
  toggleText() {
    this.isExpanded = !this.isExpanded
  }

  /**
   * Set the default "selected task column" and "global option" based on
   * whether a "Done" column exists among `added_columns`.
   */
  setInitialSelection(): void {
    // Find the last added_columns index that has a columnName value
    const lastWithValueIndex = this.added_columns.controls
      .map((control, index) => ({ control, index }))
      .filter(({ control }) => control.get('columnName')?.value)
      .map(({ index }) => index)
      .pop();

    // Find the index of the "Done" column in added_columns
    const doneColumnIndex = this.added_columns.controls
      .map((control, index) => ({ control, index }))
      .find(({ control }) => control.get('columnName')?.value === 'Done')?.index;

    // If a "Done" column exists, default to it; otherwise use the last column
    if (doneColumnIndex !== undefined) {
      this.form.get('selectedTask')?.setValue('Done');
    } else if (lastWithValueIndex !== undefined) {
      const initialTask = this.added_columns.at(lastWithValueIndex).get('columnName')?.value;
      this.form.get('selectedTask')?.setValue(initialTask);
    }

    // Global option: offer to add a "Done" column automatically if none exists
    if (doneColumnIndex === undefined) {
      this.form.get('globalOption')?.setValue('Add a Done column for me');
    } else {
      this.form.get('globalOption')?.setValue('finished task in ');
    }
  }

  /**
   * Whether a given column index is the last one carrying a value.
   * @param index - the column index to check
   */
  isLastWithValue(index: number): boolean {
    const lastWithValueIndex = this.added_columns.controls
      .map((control, idx) => ({ control, idx }))
      .filter(({ control }) => control.get('columnName')?.value)
      .map(({ idx }) => idx)
      .pop();
    return index === lastWithValueIndex;
  }

  /**
   * Whether a role/task radio should be disabled because another one is
   * already selected.
   * @param type - the radio group type ('task' or 'global')
   */
  isRadioDisabled(type: 'task' | 'global'): boolean {
    if (type === 'task') {
      return this.selectedIndex !== null ;
    }

    return this.selectedIndex !== null;

  }

  /**
   * Move to the WIP-limit step: reorder `added_columns` so "Done" is last,
   * optionally auto-add a "Done" column, then advance to step 4.
   */
  handleLimitProgress() {
    this.limitProgress = true;

    // Save the data BEFORE clearing
    this.step3_columns = this.form.value.added_columns;

    // Remove the "Done" object
    const value = this.step3_columns.find(item => item.columnName === "Done")
    this.step3_columns = this.step3_columns.filter(item => item.columnName !== "Done")

    // Push the "Done" object at the end, if it exists
    if (value) {
      this.step3_columns.push(value)
    }

    // Clear column data BEFORE repopulating
    this.columns.clear()
    this.added_columns.clear()

    // Repopulate with the saved data (a single push per item, no duplication)
    this.step3_columns.forEach((item) => {
      if (item.columnName === "Done") {
        this.isDone = true
      } else {
        this.isDoneDeleted = true
      }

      this.added_columns.push(this.formBuilder.group({
        columnName: [item.columnName],
        limitWorkInProgress: [item.limitWorkInProgress]
      }))
    })

    // If the "Add a Done column" option is checked
    this.handleAddColumn(value);

    this.nextStep()
  }

  /**
   * Auto-append a "Done" column when the user opted in and no "Done"
   * column already exists.
   * @param value - the existing "Done" column data, if any
   */
  handleAddColumn(value: String) {
    if (!value && (this.form.value.globalOption === 'Add a Done column for me')) {
      this.added_columns.push(this.formBuilder.group({
        columnName: ['Done'],
        limitWorkInProgress: []
      }))
    }
  }

  /**
   * Return from the WIP-limit step to step 2, keeping the current edits
   * staged in `step3_columns` for reuse.
   */
  handlePrevTaskStep() {
    this.step3_columns = this.form.value.added_columns;
    this.prevStep();
  }

  /**
   * Return from step 3 to step 2, restoring `columns` from `step3_columns`.
   */
  handlePrevChoiceStep() {
    this.limitProgress = false;

    // Restore columns from step3_columns so we can go back to step 1
    if (this.step3_columns && this.step3_columns.length > 0) {
      this.columns.clear();
      this.step3_columns.forEach((item: any) => {
        this.columns.push(this.formBuilder.group({
          columnName: [item.columnName || '']
        }));
      });
    }

    this.setInitialSelection();

    this.prevStep();
  }

  /** Reset to step 3 and immediately advance to step 4 (invite members). */
  handleInviteMember0(){
    this.limitProgress = false;
    this.currentStep = 3;
    this.nextStep()
  }

  /** Advance to step 4 (invite members). */
  handleInviteMember1(){
    this.limitProgress = false;
    this.nextStep()
  }

  /** Return to step 3 (WIP limits). */
  handlePrevProgressStep(){
    this.prevStep()
  }

  /**
   * Validate the entered email and stage it as a new member invite with the
   * default "Standard" role.
   * @param event - the form submit event
   */
  inviteMember(event: any) {
    event?.preventDefault()
    const emailValue = this.form.value.email;

    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

    if(emailValue && emailRegex.test(emailValue))
    {
      this.isEmailCreate = true
      this.members.push(this.formBuilder.group({
        memberEmail: [emailValue],
        role: ['Standard']
      })),
      this.email.reset()

      this.isSubmitted = false
    } else {
      this.isSubmitted = true
      alert('Please enter a correct email address')
    }

  }

  /**
   * Remove a staged member invite.
   * @param index - index of the member in the `members` FormArray
   */
  removeMember(index: number){
    if (this.members.length > 0) {
      this.members.removeAt(index );
    }
  }

  /**
   * Create the board, then persist its columns and staged member invites,
   * notifying the rest of the app and closing the modal on success.
   * @param event - the form submit event
   */
  async handleSubmit(event: any){
    event?.preventDefault()
    // Mark all form controls as touched to trigger validation messages
    this.form.markAllAsTouched()

    // Validate form
    if (!this.form.value.name || this.form.value.name.trim() === '') {
      alert('Board name is required');
      return;
    }

    try {
      const boardData = {
        name: this.form.value.name.trim(),
        description: this.form.value.description || ''
        // No userId - extracted from JWT on the backend side
      };

      console.log('Creating board:', boardData);

      // 1. Create the board
      const response: any = await lastValueFrom(
        this.entityService.addData('boards', boardData)
      );

      console.log('Board created successfully:', response);
      this.responsData = response;

      const boardId: string = response.id;

      // 2. Save KanbanColumns linked to the new board
      const columnsToSave: any[] = this.form.value.added_columns ?? [];
      for (const col of columnsToSave) {
        if (col.columnName?.trim()) {
          await lastValueFrom(
            this.entityService.addData('board/kanban-column', {
              columnName: col.columnName.trim(),
              limitWorkInProgress: col.limitWorkInProgress ?? null,
              boardId
            })
          );
        }
      }

      // 3. Save Members linked to the new board
      const membersToSave: any[] = this.form.value.members ?? [];
      const failedMembers: string[] = [];
      for (const m of membersToSave) {
        if (m.memberEmail?.trim()) {
          try {
            await lastValueFrom(
              this.entityService.addData('board/member', {
                memberEmail: m.memberEmail.trim(),
                role: m.role ?? 'Standard',
                boardId
              })
            );
          } catch (memberError: any) {
            console.warn(`Could not add member ${m.memberEmail}:`, memberError);
            failedMembers.push(m.memberEmail.trim());
          }
        }
      }

      // Notify other components about the new board
      this.message.messageAny(response);
      this.message.messageOpenBoard(true);

      // Close modal
      this.closeModal.emit();

      // Warn if some members could not be added (email not registered)
      if (failedMembers.length > 0) {
        alert(`Board created successfully!\n\nThe following email(s) could not be added as members (not registered):\n${failedMembers.join('\n')}`);
      }

    } catch (error: any) {
      console.error('Error creating board:', error);

      // Handle specific error cases
      if (error.status === 401) {
        this.errorMessage = 'Authentication required. Please sign in again.';
      } else if (error.status === 400) {
        this.errorMessage = error.error?.message || 'A board with this name already exists. Please choose a different name.';
      } else if (error.status === 403) {
        this.errorMessage = 'You do not have permission to create a board.';
      } else if (error.status === 0) {
        // Network error - board might have been created
        console.warn('Network error (status 0) - checking if board was created...');
        this.closeModal.emit();
        setTimeout(() => {
          this.reload.reloadPage();
        }, 500);
        return;
      } else {
        this.errorMessage = error.error?.message || 'An error occurred while creating the board. Please try again.';
      }

      alert(this.errorMessage);
    }
  }
  /** Close the create-board modal. */
  handleCloseModal() {
    this.closeModal.emit();
  }

  /**
   * Close the modal when the backdrop (outside the dialog) is clicked.
   * @param event - the click event
   */
  handleBackdropClick(event: MouseEvent) {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.handleCloseModal();
    }
  }

  /** Close the modal when the Escape key is pressed. */
  @HostListener('document:keydown.escape')
  handleEscapeKey() {
    this.handleCloseModal();
  }
}
