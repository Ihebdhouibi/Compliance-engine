import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterModule, SidebarComponent, TopbarComponent],
  template: `
    <div class="shell">
      <app-sidebar #sb></app-sidebar>
      <div class="shell__main"
           [class.main--collapsed]="sb.collapsed()"
           [class.main--mobile-open]="sb.mobileOpen()">
        <app-topbar></app-topbar>
        <main class="shell__content">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styleUrl: './shell.component.scss'
})
export class ShellComponent {}