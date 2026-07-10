import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'dropManager',
})
export class DropManagerPipe implements PipeTransform {

  /**
   * Capitalize the first letter of the string, leaving the rest unchanged.
   * @param value - the string to transform
   * @returns the capitalized string, or an empty string if falsy
   */
  transform(value: string): string {
    if (!value) return '';
    return value.charAt(0).toUpperCase() + value.slice(1);
  }

}
