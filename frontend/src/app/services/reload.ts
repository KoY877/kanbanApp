import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class Reload {
  constructor(private router: Router) { }

  /** Reload the entire page (hard reload). */
  reloadPage(): void {
    window.location.reload();
  }

  /**
   * Reload the current route without a full page reload (soft reload -
   * recommended). Navigates through a blank route first so Angular
   * re-triggers the current route's resolvers/guards.
   */
  reloadCurrentRoute(): void {
    const currentUrl = this.router.url;
    this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => {
      this.router.navigate([currentUrl]);
    });
  }

  /**
   * Navigate to a specific route, forcing a soft reload of its resolvers/guards.
   * @param route - the target route path
   */
  navigateAndReload(route: string): void {
    this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => {
      this.router.navigate([route]);
    });
  }
}
