import { Component } from '@angular/core';
import { CustomInputComponent } from '../shared/component/custom-input/custom-input.component';
import { DownloadFormComponent } from "./component/download-form/download-form.component";
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'app-download',
  imports: [DownloadFormComponent],
  templateUrl: './download.component.html',
  styleUrl: './download.component.scss',
})
export class DownloadComponent {

  downloadForm!: FormGroup;

  initDownloadForm(event: FormGroup) {
    this.downloadForm = event;
  }

}
