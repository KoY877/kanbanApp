import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-alert',
  imports: [],
  templateUrl: './alert.html',
  styleUrl: './alert.css',
})
export class Alert {
  @Output() cancel: EventEmitter<void> = new EventEmitter<void>();
  @Output() discard: EventEmitter<void> = new EventEmitter<void>();

  /** Emit a cancel event, keeping the current unsaved state. */
  handleCancel() {
    this.cancel.emit();
  }

  /** Emit a discard event, dropping the current unsaved state. */
  handleDiscard(){
    this.discard.emit()
  }
}
