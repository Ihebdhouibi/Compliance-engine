import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DesignSettingsService, DesignSettings } from '../../services/design-settings.service';
import { environment } from '../../environments/environment';

type Section = 'design' | 'notifications';

type ColorKey =
  | 'sideNavbarColor'
  | 'topNavbarColor'
  | 'backgroundColor'
  | 'accentColor'
  | 'addButtonColor'
  | 'updateButtonColor'
  | 'deleteButtonColor';

export interface ColorField {
  key: ColorKey;
  label: string;
  hint: string;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})
export class SettingsComponent {

  private ds = inject(DesignSettingsService);

  activeSection: Section = 'design';
  isSaving      = false;
  isRemovingLogo    = false;
  isRemovingFavIcon = false;
  saveMsg   = '';
  saveError = '';

  form: Partial<DesignSettings> = {};

  logoPreview    = '';
  favIconPreview = '';
  logoFile:    File | null = null;
  favIconFile: File | null = null;

  readonly serverBase = environment.serverBaseUrl;

  readonly defaultColors: Record<ColorKey, string> = {
    sideNavbarColor:   '#071528',
    topNavbarColor:    '#071528',
    backgroundColor:   '#020912',
    accentColor:       '#00d4ed',
    addButtonColor:    '#00d4ed',
    updateButtonColor: '#00bcd4',
    deleteButtonColor: '#ff4d6a',
  };

  readonly colorFields: ColorField[] = [
    { key: 'sideNavbarColor',   label: 'Sidebar Background',  hint: 'Color of the left navigation panel' },
    { key: 'topNavbarColor',    label: 'Top Bar Background',  hint: 'Color of the header bar' },
    { key: 'backgroundColor',   label: 'Page Background',     hint: 'Main page background color' },
    { key: 'accentColor',       label: 'Accent / Highlight',  hint: 'Used for active states, borders and glows' },
    { key: 'addButtonColor',    label: 'Add Button',          hint: 'Background of all Add / Create buttons' },
    { key: 'updateButtonColor', label: 'Update Button',       hint: 'Background of all Edit / Save buttons' },
    { key: 'deleteButtonColor', label: 'Delete Button',       hint: 'Background of all Delete / Remove buttons' },
  ];

  constructor() {
    this.form = { ...this.ds.settings() };
  }

  get currentSettings(): DesignSettings {
    return this.ds.settings();
  }

  getColor(key: ColorKey): string {
    return (this.form as Record<string, string>)[key] ?? '#000000';
  }

  setColor(key: ColorKey, value: string): void {
    (this.form as Record<string, string>)[key] = value;
  }

  setSection(s: Section): void {
    this.activeSection = s;
  }

  onLogoChange(e: Event): void {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.logoFile = file;
    const reader = new FileReader();
    reader.onload = ev => { this.logoPreview = ev.target?.result as string; };
    reader.readAsDataURL(file);
  }

  onFavIconChange(e: Event): void {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.favIconFile = file;
    const reader = new FileReader();
    reader.onload = ev => { this.favIconPreview = ev.target?.result as string; };
    reader.readAsDataURL(file);
  }

  // Clear logo preview (before saving — just clears local selection)
  clearLogoSelection(): void {
    this.logoFile    = null;
    this.logoPreview = '';
  }

  // Clear favicon preview (before saving)
  clearFavIconSelection(): void {
    this.favIconFile    = null;
    this.favIconPreview = '';
  }

  // DELETE logo from server
  removeLogo(): void {
    this.isRemovingLogo = true;
    this.ds.removeLogo().subscribe({
      next: () => {
        this.isRemovingLogo = false;
        this.logoPreview    = '';
        this.logoFile       = null;
        this.showSuccess('Logo removed successfully.');
      },
      error: () => {
        this.isRemovingLogo = false;
        this.saveError = 'Failed to remove logo.';
      }
    });
  }

  // DELETE favicon from server
  removeFavIcon(): void {
    this.isRemovingFavIcon = true;
    this.ds.removeFavIcon().subscribe({
      next: () => {
        this.isRemovingFavIcon = false;
        this.favIconPreview    = '';
        this.favIconFile       = null;
        this.showSuccess('Favicon removed successfully.');
      },
      error: () => {
        this.isRemovingFavIcon = false;
        this.saveError = 'Failed to remove favicon.';
      }
    });
  }

  resetColor(key: ColorKey): void {
    (this.form as Record<string, string>)[key] = this.defaultColors[key];
  }

  resetAllColors(): void {
    (Object.keys(this.defaultColors) as ColorKey[]).forEach(k => {
      (this.form as Record<string, string>)[k] = this.defaultColors[k];
    });
  }

  private showSuccess(msg: string): void {
    this.saveMsg = msg;
    setTimeout(() => this.saveMsg = '', 4000);
  }

  save(): void {
    this.isSaving  = true;
    this.saveMsg   = '';
    this.saveError = '';

    const fd = new FormData();
    const data: Record<string, string> = {};

    if (this.form.projectTitle)      data['projectTitle']      = this.form.projectTitle;
    if (this.form.sideNavbarColor)   data['sideNavbarColor']   = this.form.sideNavbarColor;
    if (this.form.topNavbarColor)    data['topNavbarColor']    = this.form.topNavbarColor;
    if (this.form.backgroundColor)   data['backgroundColor']   = this.form.backgroundColor;
    if (this.form.accentColor)       data['accentColor']       = this.form.accentColor;
    if (this.form.addButtonColor)    data['addButtonColor']    = this.form.addButtonColor;
    if (this.form.updateButtonColor) data['updateButtonColor'] = this.form.updateButtonColor;
    if (this.form.deleteButtonColor) data['deleteButtonColor'] = this.form.deleteButtonColor;

    fd.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));

    if (this.favIconFile) fd.append('favIcon', this.favIconFile);
    if (this.logoFile)    fd.append('logo',    this.logoFile);

    this.ds.update(fd).subscribe({
      next: () => {
        this.isSaving = false;
        this.showSuccess('Settings saved successfully.');
      },
      error: () => {
        this.isSaving  = false;
        this.saveError = 'Failed to save settings. Please try again.';
      }
    });
  }
}