import { NgStyle } from '@angular/common';
import { Component, computed, input, Input, output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'custom-input',
  imports: [NgStyle, ReactiveFormsModule],
  templateUrl: './custom-input.component.html',
  styleUrl: './custom-input.component.scss',
})
export class CustomInputComponent {

  control = input.required<FormControl>();
  label = input<string>('');
  placeholder = input<string>('');
  asterisk = input<boolean>(false);
  width = input<string>('100%');
  rightBtn = input<InnerButton>();

  rightBtnPressed = output<any>();

  totalInputWidth = computed<string>(() => {
    const wp = this.rightBtn()?.widthPercentage;

    if(!wp) {
      return '100%';
    }

    const newWidth = (100 - wp);
    const newWidthStr = newWidth + '%';
    // console.log("Total Input Width:", newWidthStr);
    // console.log("Total Button Width:", wp);
    return newWidthStr;
  });

  onRightBtnPress(): void {
    this.rightBtnPressed.emit(this.control().value);
  }

}

interface InnerButton {
  label: string,
  widthPercentage: number,
  color?: string,
  backgroundColor?: string
}
