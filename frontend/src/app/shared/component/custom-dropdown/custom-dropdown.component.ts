import { NgStyle } from '@angular/common';
import { Component, input, OnInit } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { DropdownOption } from '../../util/dropdown-option';

@Component({
  selector: 'custom-dropdown',
  imports: [NgStyle, ReactiveFormsModule],
  templateUrl: './custom-dropdown.component.html',
  styleUrl: './custom-dropdown.component.scss',
})
export class CustomDropdownComponent implements OnInit {

  control = input.required<FormControl>();
  label = input<string>('');
  asterisk = input<boolean>(false);
  width = input<string>('100%');
  options = input<DropdownOption[]>();

  ngOnInit() {
  }

}
