import { ChangeDetectorRef, Component, ElementRef, EventEmitter, Input, input, Output, ViewChild } from '@angular/core';
import { FormArray, FormBuilder, FormControl, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-description',
  imports: [],
  templateUrl: './description.html',
  styleUrl: './description.css',
})
export class Description {

  @ViewChild('textArea') textArea!: ElementRef;
  @Input() initialDescription: string = '';
  @Output() descriptionChange = new EventEmitter<string>();
  private destroy$ = new Subject<void>();

  descriptions?: string;
  form?: any;
  selectedText?: string;

  constructor (
    private formBuilder : FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {
    this.form = this.formBuilder.group({
      description: [
        '',
        [Validators.minLength(3),       // minimum 3 characters
        Validators.pattern(/^[a-zA-Z\s]+$/)] // letters and spaces only
      ]
    });
  }

  /**
   * Read the text currently selected by the user in the document.
   * @returns the selected text, or an empty string if none
   */
  getSelectedText(): string {
    const selection = window.getSelection();
    return selection ? selection.toString() : '';
  }

  /**
   * Wrap the currently selected text in markdown markers (bold/italic) and
   * insert it back into the textarea.
   * @param command - 'bold' or 'italic'
   */
  format(command: string) {
    this.selectedText = this.getSelectedText();

    if (!this.selectedText) {
      return;
    }

    let formattedText = '';

    switch(command) {
      case 'bold':
        formattedText = `**${this.selectedText}**`;

        break;
      case 'italic':
        formattedText = `__${this.selectedText}__`;
        break;

      default:
        return;

    }

    // Replace the selection with the markdown-formatted text
    document.execCommand('insertText', false, formattedText);

    // Update the FormControl
    const content = this.textArea.nativeElement.textContent;
    this.description.setValue(content, { emitEvent: true });

  }

  /**
   * Lifecycle hook: pre-fill the textarea with the initial description once
   * the view is ready.
   */
  ngAfterViewInit(): void {
    if (this.initialDescription && this.textArea) {
      this.textArea.nativeElement.textContent = this.initialDescription;
      this.description.setValue(this.initialDescription);
      this.cdr.detectChanges();
    }
  }

  /** The description text FormControl. */
  get description(): FormControl{
    return this.form.get('description') as FormControl;
  }

  ngOnInit(): void {

    this.cdr.detectChanges();

  }

  /** Lifecycle hook: complete the destroy$ subject to unsubscribe all pipes. */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Convert the textarea's markdown markers to HTML and push the result
   * back into both the DOM and the FormControl.
   */
  showContent(): void {

    const content = this.textArea.nativeElement.textContent;

    // Turn **bold** and __italic__ markers into HTML tags
    const formattedContent = content
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>') // bold
      .replace(/__(.*?)__/g, '<em>$1</em>'); // italic

    document.execCommand(formattedContent, false, '');

    // Update the FormControl with the formatted and displayed content
    const contentAfterFormat = this.textArea.nativeElement.innerHTML;
    this.description.setValue(contentAfterFormat, { emitEvent: true });

  }

  isPreviewMode: boolean = false;
  originalContent: string = '';

  /**
   * Toggle between edit mode (raw markdown, editable) and preview mode
   * (rendered HTML, read-only).
   */
  togglePreview(): void {
    if (!this.isPreviewMode) {
      // Switch to preview mode
      this.originalContent = this.textArea.nativeElement.textContent || '';

      const htmlContent = this.originalContent
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/__(.*?)__/g, '<em>$1</em>');

      this.textArea.nativeElement.innerHTML = htmlContent;
      this.textArea.nativeElement.setAttribute('contenteditable', 'false');
      this.isPreviewMode = true;
    } else {
      // Switch back to edit mode
      this.textArea.nativeElement.textContent = this.originalContent;
      this.textArea.nativeElement.setAttribute('contenteditable', 'true');
      this.isPreviewMode = false;
    }
  }

  /**
   * Force pasted clipboard content to be inserted as plain text (no rich
   * formatting), to keep the markdown model consistent.
   * @param event - the paste event
   */
  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const text = event.clipboardData?.getData('text/plain') || '';
    document.execCommand('insertText', false, text);
  }

  /**
   * Sync the description FormControl and emit descriptionChange whenever
   * the textarea content changes (ignored while in preview mode).
   * @param event - the contenteditable input event
   */
  onContentChange(event: Event): void {
    if (this.isPreviewMode) {
      return;
    }

    const target = event.target as HTMLElement;
    const content = target.textContent || '';

    this.description.setValue(content, { emitEvent: true });
    this.descriptionChange.emit(content);

  }
}
