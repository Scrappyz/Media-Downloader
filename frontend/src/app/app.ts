import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DownloadComponent } from './download/download.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, DownloadComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('frontend');
}
