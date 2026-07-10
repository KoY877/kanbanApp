import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'character',
})
export class CharacterPipe implements PipeTransform {

  /**
   * Build a 2-letter uppercase initials placeholder followed by "?".
   * @param value - the source string (e.g. a name)
   * @returns the first two characters uppercased and suffixed with "?", or
   *          the original value if falsy
   */
  transform(value?: string): string | undefined {
    if (value)
      return value.charAt(0).toUpperCase() + value.charAt(1).toUpperCase() + '?' ;
    return value
  }

}
