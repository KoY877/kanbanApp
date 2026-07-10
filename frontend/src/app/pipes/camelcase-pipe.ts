import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'camelcase',
})
export class CamelcasePipe implements PipeTransform {

  /**
   * Capitalize the first letter and lowercase the rest of the string.
   * @param value - the string to transform
   * @returns the camelcased string, or the original value if falsy
   */
  transform(value?: string): string | undefined {
    if (value)
      return value.charAt(0).toUpperCase()+value.slice(1).toLowerCase();
    return value
  }
}
