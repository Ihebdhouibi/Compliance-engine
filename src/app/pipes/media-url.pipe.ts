import { Pipe, PipeTransform } from '@angular/core';
import { environment } from '../environments/environment';
import { MediaModel } from '../models/media.model';

@Pipe({ name: 'mediaUrl', standalone: true })
export class MediaUrlPipe implements PipeTransform {
  transform(media: MediaModel | null | undefined): string {
    if (!media?.url) return 'assets/default-avatar.png';
    return `${environment.serverBaseUrl}${media.url}`;
  }
}