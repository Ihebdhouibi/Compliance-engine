import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`
})
export class AppComponent implements OnInit {
  ngOnInit(): void {
    if (typeof document !== 'undefined' &&
        localStorage.getItem('colorMode') === 'dark') {
      document.body.classList.add('dark-mode');
    }
  }
}
