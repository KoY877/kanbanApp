import { ChangeDetectorRef, Component, signal, ViewChild } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Header } from "./components/header/header";
import { Container } from "./components/container/container";
import { Message } from './services/message';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header, Container],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('KanbanApp_Angular');
  isCloseAllDropdown = false;

   

  /**
   * Toggle the close-all-dropdowns flag, propagated as an @Input to the
   * Container component to close any open dropdown across the app.
   */
  handleCloseAllDropdowns(): void {
     this.isCloseAllDropdown = !this.isCloseAllDropdown;
  }
}
