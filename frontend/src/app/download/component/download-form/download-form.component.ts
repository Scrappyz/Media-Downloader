import { Component, inject, OnInit, output } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { DownloadService } from '../../service/download.service';
import { CustomDropdownComponent } from "../../../shared/component/custom-dropdown/custom-dropdown.component";
import { DropdownOption } from '../../../shared/util/dropdown-option';
import { CustomInputComponent } from "../../../shared/component/custom-input/custom-input.component";

@Component({
  selector: 'download-form',
  imports: [CustomDropdownComponent, CustomInputComponent],
  templateUrl: './download-form.component.html',
  styleUrl: './download-form.component.scss',
})
export class DownloadFormComponent implements OnInit {

  private downloadService = inject(DownloadService);

  formReady = output<FormGroup>();

  typeOptions: DropdownOption[] = [
    {
      label: 'Video',
      value: 'VIDEO'
    },
    {
      label: 'Video Only',
      value: 'VIDEO_ONLY'
    },
    {
      label: 'Audio Only',
      value: 'AUDIO_ONLY'
    }
  ];

  // Initialize OnInit for shorter code
  videoFormatOptions!: DropdownOption[];
  audioFormatOptions!: DropdownOption[];
  videoQualityOptions!: DropdownOption[];
  audioQualityOptions!: DropdownOption[];

  metadataOptions: DropdownOption[] = [
    {
      label: 'Yes',
      value: true
    },
    {
      label: 'No',
      value: false
    }
  ]

  form = new FormGroup({
    type: new FormControl('VIDEO'),
    url: new FormControl(),
    video: new FormGroup({
      format: new FormControl('default'),
      quality: new FormControl('best')
    }),
    audio: new FormGroup({
      format: new FormControl('default'),
      quality: new FormControl('best')
    }),
    metadata: new FormControl(true),
    outputName: new FormControl('')
  });

  ngOnInit() {
    const videoFormatOptions = ['Default', 'mp4', 'mkv'];
    const audioFormatOptions = ['Default', 'mp3', 'm4a', 'flac'];
    const videoQualityOptions = ['Best', '2160p', '1440p', '1080p', '720p', '480p', '360p', '240p', '144p', 'Worst'];
    const audioQualityOptions = ['Best', '320kbps', '256kbps', '192kbps', '128kbps', 'Worst'];

    this.videoFormatOptions = videoFormatOptions.map((val): DropdownOption => {
      return {
        label: val,
        value: val.toLowerCase()
      }
    });

    this.audioFormatOptions = audioFormatOptions.map((val): DropdownOption => {
      return {
        label: val,
        value: val.toLowerCase()
      }
    });

    this.videoQualityOptions = videoQualityOptions.map((val): DropdownOption => {
      return {
        label: val,
        value: val.toLowerCase()
      }
    });

    this.audioQualityOptions = audioQualityOptions.map((val): DropdownOption => {
      return {
        label: val,
        value: val.toLowerCase()
      }
    });

    this.formReady.emit(this.form);
  }

  async onPaste() {
    const text = await navigator.clipboard.readText();
    this.form.controls.url.setValue(text);
  }

}
