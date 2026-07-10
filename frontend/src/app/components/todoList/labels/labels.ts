import { ChangeDetectorRef, Component, EventEmitter, Input, Output, SimpleChanges } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { EntityService } from '../../../services/entity-service';
import { Message } from '../../../services/message';
import { Reload } from '../../../services/reload';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { faBars } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeModule } from "@fortawesome/angular-fontawesome";

@Component({
  selector: 'app-labels',
  imports: [CommonModule, ReactiveFormsModule, FontAwesomeModule],
  templateUrl: './labels.html',
  styleUrls: ['./labels.css'],
})
export class Labels {
  readonly faBars = faBars;

  @Input() selectedLabels!: any
  @Output() sendSelectedLabel: EventEmitter<any> = new EventEmitter<any>();
  @Output() sendDeselectedLabel: EventEmitter<any> = new EventEmitter<any>();
  @Output() closeDropdownLabels: EventEmitter<void> = new EventEmitter<void>();
  private destroy$ = new Subject<void>();

  memberColumns: any | undefined[] = []
  allLabels: any | undefined[] = []
  form: any;
  isLabel: Boolean = false;
  isViewLabels: Boolean = false;

  constructor(
    private entityService: EntityService,
    private detectChange: ChangeDetectorRef,
    private message: Message,
    private reload: Reload,
    private fb: FormBuilder,
  ) {
    this.form = fb.group({
      label: [
        '',
        [Validators.minLength(3),       // minimum 3 characters
        Validators.maxLength(30),      // maximum 30 characters
        Validators.pattern(/^[a-zA-Z\s]+$/)] // letters and spaces only
      ],
      labels: this.fb.array([])
    });
  }

  /**
   * Lifecycle hook: reload the selected-labels FormArray whenever the
   * `selectedLabels` @Input changes.
   * @param changes - Angular's input change record
   */
  ngOnChanges(changes: SimpleChanges) {
    if (changes['selectedLabels'] && changes['selectedLabels'].currentValue) {
      this.loadSelectedLabels(changes['selectedLabels'].currentValue);
    }

    this.detectChange.detectChanges();
  }

  /**
   * Rebuild the `labels` FormArray and the `allLabels` display list from a
   * given set of labels, marking them all as checked.
   * @param labels - labels as strings or `{ label: string }` objects
   */
  private loadSelectedLabels(labels: any[]) {
    this.labels.clear();

    if (Array.isArray(labels) && labels.length > 0) {
      labels.forEach((label: any) => {
        this.labels.push(this.fb.group({
          label: [typeof label === 'string' ? label : label.label],
          checked: [true]
        }));
      });

      this.allLabels = labels.map((label: any) => ({
        label: typeof label === 'string' ? label : label.label,
        checked: true
      }));
    }
  }

  /** Get the currently selected labels @Input value. */
  getLabels(): string | undefined {
    return this.selectedLabels;
  }

  /**
   * Lifecycle hook: load any pre-selected labels and subscribe to the
   * label-input value changes.
   */
  ngOnInit(): void {
    if (this.selectedLabels && Array.isArray(this.selectedLabels) && this.selectedLabels.length > 0) {
      this.loadSelectedLabels(this.selectedLabels);
    }

    this.label.valueChanges.pipe(takeUntil(this.destroy$)).subscribe({
      next: (value) => {

      }
    });
  }

  /** Lifecycle hook: complete the destroy$ subject to unsubscribe all pipes. */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** The new-label text input FormArray. */
  get label(): FormArray {
    return this.form.get('label') as FormArray;
  }

  /** The FormArray backing the list of labels. */
  get labels(): FormArray {
    return this.form.get('labels') as FormArray;
  }

  /**
   * Add the value typed in the label input to both the FormArray and the
   * display list, if it is 3-30 characters and not already present.
   * @param event - the keyboard event from the label input
   */
  async addLabel(event: Event) {

    event.preventDefault()

    if (!(event instanceof KeyboardEvent)) return;

    const input = event.target as HTMLInputElement;
    const value = input.value.trim();

    if (value.length >= 3 && value.length <= 30) {
      const existsInFormArray = this.labels.value.some((l: any) => l.label === value);

      if (!existsInFormArray) {
        this.labels.push(this.fb.group({
          label: [value],
          checked: [true]
        }));
      }

      const existsInAllLabels = this.allLabels.some((l: any) => l.label === value);

      if (!existsInAllLabels) {
        this.allLabels.push({ label: value, checked: true });
      }

      this.label.reset();
    }

    // Emit the list of checked labels
    this.sendSelectedLabel.emit(this.allLabels);

    this.detectChange.detectChanges();
  }

  /**
   * Remove a label from the display list and uncheck it in the FormArray
   * (without deleting the FormArray entry).
   * @param event - the click event
   * @param index - index of the label in `allLabels`
   */
  removeLabelFromDisplay(event: any, index: number) {
    event.stopPropagation();

    const labelToRemove = this.allLabels[index];

    this.allLabels.splice(index, 1);

    const formArrayIndex = this.labels.value.findIndex((l: any) => l.label === labelToRemove.label);
    if (formArrayIndex > -1) {
      const labelFormGroup = this.labels.at(formArrayIndex);
      labelFormGroup.patchValue({ checked: false });
    }

    this.sendDeselectedLabel.emit(this.allLabels);

    this.detectChange.detectChanges();
  }

  /**
   * Remove a label entirely from the FormArray and the display list.
   * @param event - the click event
   * @param index - index of the label in the `labels` FormArray
   */
  removeLabel(event: any, index: number) {

    event.stopPropagation();

    this.labels.removeAt(index);

    this.allLabels = this.labels.value.filter((l: any) => l.checked !== false);
    this.sendDeselectedLabel.emit(this.allLabels);

    this.detectChange.detectChanges();
  }

  /** Close the labels dropdown. */
  handleCloseDropdown() {
    this.closeDropdownLabels.emit();
  }

  /** Hide the "view all labels" panel. */
  handleCloseDropdownLabels() {
    this.isViewLabels = false;
    this.detectChange.detectChanges();
  }

  /**
   * Toggle the "view all labels" panel.
   * @param event - expected to be the literal string "label"
   */
  handleOpenLabel(event: string) {
    if (event === "label") {
      this.isViewLabels = !this.isViewLabels;
      this.detectChange.detectChanges();
    }
  }

  /**
   * Check or uncheck a label from the full label list, syncing both the
   * FormArray and the display list, and notify the parent.
   * @param event - the checkbox change event
   * @param item - the label item being toggled (mutated in place for the view)
   * @param index - the label's index in the `labels` FormArray
   */
  handleSelectedLabel(event: any, item: any, index: number) {
    event.preventDefault();

    const target = event.target as HTMLInputElement;

    if (target.checked) {

      const alreadyExists = this.allLabels.some(
        (label: any) => label.label === target.value
      );

      if (!alreadyExists) {
        this.allLabels.push({ label: target.value, checked: true });
      }

      item.checked = true;

      const labelFormGroup = this.labels.at(index);
      labelFormGroup.patchValue({ checked: true });

      this.sendSelectedLabel.emit(this.allLabels);
    } else {

      this.allLabels = this.allLabels.filter(
        (label: any) => label.label !== target.value
      );

      item.checked = false;

      const labelFormGroup = this.labels.at(index);
      labelFormGroup.patchValue({ checked: false });

      this.sendDeselectedLabel.emit(this.allLabels);
    }

    this.detectChange.detectChanges();
  }
}
