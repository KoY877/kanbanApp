import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'color',
})
export class ColorPipe implements PipeTransform {

  /**
   * Generate a random hex color code.
   * @returns a color string in the form "#RRGGBB"
   */
  private generateRandomColor(): string {
    const letters = '0123456789ABCDEF';
    let color = '#';

    for (let i = 0; i < 6; i++) {
      color += letters[Math.floor(Math.random() * 16)];
    }

    return color;
  }

  /**
   * Return a new random hex color on each call. Takes no input value.
   * @returns a random color string in the form "#RRGGBB"
   */
  transform(): string {
    return this.generateRandomColor();
  }
}
